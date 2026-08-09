import { performance } from "node:perf_hooks";

const baseUrl = process.env.BASE_URL ?? "http://localhost:3000/api";
const runs = numberSetting("RUNS", 3);
const rounds = numberSetting("ROUNDS", 20);
const sseClientsPerRun = numberSetting("SSE_CLIENTS", 5);
const staff = [
  { username: process.env.STAFF_ONE ?? "staff1", password: process.env.STAFF_PASSWORD ?? "demo123" },
  { username: process.env.STAFF_TWO ?? "staff2", password: process.env.STAFF_PASSWORD ?? "demo123" },
];

const callLatencies = [];
const sseLatencies = [];
const assignedTokenIds = new Set();
const runSummaries = [];
let duplicateAssignments = 0;
let failures = 0;
let callNextRequests = 0;

async function main() {
  const queues = await request("/queues");
  const queue = queues.find((item) => item.code === "GENERAL") ?? queues[0];
  if (!queue) throw new Error("No queue is available. Start LinePilot with demo data enabled.");

  const benchmarkStarted = performance.now();
  for (let run = 1; run <= runs; run += 1) {
  const clients = await Promise.all(
    Array.from({ length: sseClientsPerRun }, (_, index) => SseTracker.connect(`${baseUrl}/queues/${queue.id}/events`, `run-${run}-client-${index + 1}`)),
  );
  const runStarted = performance.now();
  let runDuplicates = 0;
  let runFailures = 0;

  try {
    for (let round = 1; round <= rounds; round += 1) {
      await Promise.all([
        request(`/queues/${queue.id}/tokens`, {
          method: "POST",
          body: JSON.stringify({ customerName: `Benchmark ${run}-${round}-A` }),
        }),
        request(`/queues/${queue.id}/tokens`, {
          method: "POST",
          body: JSON.stringify({ customerName: `Benchmark ${run}-${round}-B` }),
        }),
      ]);

      const callStarted = performance.now();
      const calls = staff.map(async (account) => {
        const started = performance.now();
        const token = await request(`/staff/queues/${queue.id}/call-next`, { method: "POST" }, account);
        callLatencies.push(performance.now() - started);
        callNextRequests += 1;
        return { token, account };
      });

      let assignments;
      try {
        assignments = await Promise.all(calls);
      } catch (error) {
        failures += 1;
        runFailures += 1;
        throw error;
      }

      const [first, second] = assignments;
      if (first.token.id === second.token.id) {
        duplicateAssignments += 1;
        runDuplicates += 1;
      }
      for (const assignment of assignments) {
        if (assignedTokenIds.has(assignment.token.id)) {
          duplicateAssignments += 1;
          runDuplicates += 1;
        }
        assignedTokenIds.add(assignment.token.id);
      }

      const expectedIds = new Set(assignments.map((assignment) => assignment.token.id));
      const eventReceipts = await Promise.all(clients.map((client) => client.waitFor((snapshot) => {
        const called = snapshot.tokens.filter((token) => expectedIds.has(token.id) && token.status === "CALLED");
        return called.length === expectedIds.size;
      }, 5000)));
      for (const receivedAt of eventReceipts) sseLatencies.push(receivedAt - callStarted);

      await Promise.all(assignments.map(async ({ token, account }) => {
        await request(`/staff/tokens/${token.id}/start`, { method: "POST" }, account);
        await request(`/staff/tokens/${token.id}/complete`, { method: "POST" }, account);
      }));
    }
  } finally {
    clients.forEach((client) => client.close());
  }

  runSummaries.push({
    run,
    rounds,
    callNextRequests: rounds * 2,
    duplicateAssignments: runDuplicates,
    failures: runFailures,
    elapsedSeconds: round((performance.now() - runStarted) / 1000),
  });
  }

  const elapsedSeconds = (performance.now() - benchmarkStarted) / 1000;
  const result = {
  benchmark: "LinePilot concurrent call-next and SSE",
  date: new Date().toISOString(),
  baseUrl,
  configuration: {
    runs,
    roundsPerRun: rounds,
    concurrentStaffPerRound: 2,
    sseClientsPerRun,
    totalCallNextRequests: callNextRequests,
    totalSseLatencySamples: sseLatencies.length,
  },
  correctness: {
    duplicateTokenAssignments: duplicateAssignments,
    failures,
    uniqueAssignedTokens: assignedTokenIds.size,
  },
  callNextLatencyMs: statistics(callLatencies),
  sseUpdateLatencyMs: statistics(sseLatencies),
  throughput: {
    callNextRequestsPerSecondAcrossWholeScenario: round(callNextRequests / elapsedSeconds),
    elapsedSeconds: round(elapsedSeconds),
  },
  runs: runSummaries,
  };

  console.log(JSON.stringify(result, null, 2));
  if (duplicateAssignments > 0 || failures > 0) process.exitCode = 1;
}

function authHeader(account) {
  return `Basic ${Buffer.from(`${account.username}:${account.password}`).toString("base64")}`;
}

async function request(path, options = {}, account) {
  const headers = new Headers(options.headers);
  if (options.body) headers.set("Content-Type", "application/json");
  if (account) headers.set("Authorization", authHeader(account));
  const response = await fetch(`${baseUrl}${path}`, { ...options, headers });
  if (!response.ok) {
    const body = await response.text();
    throw new Error(`${options.method ?? "GET"} ${path} returned ${response.status}: ${body}`);
  }
  return response.json();
}

function statistics(values) {
  if (values.length === 0) return { samples: 0, average: null, median: null, p95: null, min: null, max: null };
  const sorted = [...values].sort((a, b) => a - b);
  return {
    samples: sorted.length,
    average: round(sorted.reduce((sum, value) => sum + value, 0) / sorted.length),
    median: percentile(sorted, 50),
    p95: percentile(sorted, 95),
    min: round(sorted[0]),
    max: round(sorted.at(-1)),
  };
}

function percentile(sorted, percent) {
  const index = Math.min(sorted.length - 1, Math.ceil((percent / 100) * sorted.length) - 1);
  return round(sorted[index]);
}

function round(value) {
  return Math.round(value * 100) / 100;
}

function numberSetting(name, fallback) {
  const value = Number(process.env[name] ?? fallback);
  if (!Number.isInteger(value) || value < 1) throw new Error(`${name} must be a positive integer`);
  return value;
}

class SseTracker {
  constructor(label, abortController, reader) {
    this.label = label;
    this.abortController = abortController;
    this.reader = reader;
    this.lastSnapshot = null;
    this.lastReceivedAt = 0;
    this.waiters = new Set();
  }

  static async connect(url, label) {
    const abortController = new AbortController();
    const response = await fetch(url, { signal: abortController.signal, headers: { Accept: "text/event-stream" } });
    if (!response.ok || !response.body) throw new Error(`SSE ${label} failed with ${response.status}`);
    const tracker = new SseTracker(label, abortController, response.body.getReader());
    tracker.readLoop();
    await tracker.waitFor(() => true, 5000);
    return tracker;
  }

  async readLoop() {
    const decoder = new TextDecoder();
    let buffer = "";
    try {
      while (true) {
        const { value, done } = await this.reader.read();
        if (done) return;
        buffer += decoder.decode(value, { stream: true }).replaceAll("\r\n", "\n");
        let boundary = buffer.indexOf("\n\n");
        while (boundary >= 0) {
          const block = buffer.slice(0, boundary);
          buffer = buffer.slice(boundary + 2);
          const data = block.split("\n")
            .filter((line) => line.startsWith("data:"))
            .map((line) => line.slice(5).trimStart())
            .join("\n");
          if (data) this.receive(JSON.parse(data));
          boundary = buffer.indexOf("\n\n");
        }
      }
    } catch (error) {
      if (error?.name !== "AbortError") console.error(`SSE ${this.label}: ${error.message}`);
    }
  }

  receive(snapshot) {
    this.lastSnapshot = snapshot;
    this.lastReceivedAt = performance.now();
    for (const waiter of this.waiters) {
      if (waiter.predicate(snapshot)) {
        clearTimeout(waiter.timeout);
        this.waiters.delete(waiter);
        waiter.resolve(this.lastReceivedAt);
      }
    }
  }

  waitFor(predicate, timeoutMs) {
    if (this.lastSnapshot && predicate(this.lastSnapshot)) return Promise.resolve(this.lastReceivedAt);
    return new Promise((resolve, reject) => {
      const waiter = {
        predicate,
        resolve,
        timeout: setTimeout(() => {
          this.waiters.delete(waiter);
          reject(new Error(`Timed out waiting for SSE update from ${this.label}`));
        }, timeoutMs),
      };
      this.waiters.add(waiter);
    });
  }

  close() {
    this.abortController.abort();
  }
}

await main();
