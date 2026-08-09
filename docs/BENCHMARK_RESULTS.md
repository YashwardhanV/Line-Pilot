# Benchmark Results

## Integrity rule

These are observed local results, not invented targets and not a general scalability claim. Re-run the command on your machine before using Version B resume bullets. If results differ, update this file and the bullets together.

## Environment

- Date: **7 August 2026** (`2026-08-07T09:08:28Z` benchmark timestamp).
- Host: Windows 11 Home Single Language 10.0.26200.
- CPU: Intel Core i5-10200H @ 2.40 GHz, 8 logical processors.
- Host memory: 7.8 GB; Docker Desktop reported approximately 3.8 GB available to its engine.
- Docker Engine: 29.6.2.
- Containers: PostgreSQL 16 Alpine, Spring Boot backend on Eclipse Temurin Java 21 JRE, Nginx 1.27 Alpine frontend.
- Benchmark client: Node.js 24.19.0 on the host.
- Network path: Node client → `localhost:3000` → Nginx → Spring Boot → PostgreSQL; SSE returned through the same Nginx proxy.
- Database: persistent local Compose volume. Demo data was enabled. A two-round smoke run occurred immediately before the recorded run, leaving a small constant backlog; each recorded round added two tokens and completed two assignments.

## Command

```bash
docker compose up --build -d
node benchmarks/linepilot-benchmark.mjs
```

Defaults used:

```text
RUNS=3
ROUNDS=20
SSE_CLIENTS=5
concurrent staff per round=2
```

## Workload

Each of three runs:

1. Opened five simultaneous SSE clients.
2. Repeated 20 rounds.
3. Created two waiting tokens per round.
4. Dispatched two `call-next` requests concurrently as different staff accounts.
5. Verified returned token IDs were distinct within the round and had never been returned earlier in the benchmark.
6. Waited on every SSE client for a committed snapshot containing both called assignments.
7. Started and completed both services so the staff accounts could proceed to the next round.

Recorded volume:

- 3 independent runs.
- 60 concurrent rounds.
- 120 call-next HTTP requests.
- 120 unique assigned tokens.
- 300 measured SSE update samples (60 rounds × 5 clients).
- 480 mutation requests in the full scenario: 120 joins, 120 call-next, 120 start, and 120 complete.
- 15 SSE connections over the three runs.

## Results

### Correctness

| Metric | Observed |
|---|---:|
| Duplicate token assignments | **0** |
| Request/scenario failures | **0** |
| Unique assigned tokens | **120** |

Every individual run recorded 40 call-next requests, 0 duplicates, and 0 failures.

### Call-next HTTP latency

| Statistic | Milliseconds |
|---|---:|
| Samples | 120 |
| Average | 97.62 |
| Median | 94.47 |
| p95 | 125.23 |
| Minimum | 76.90 |
| Maximum | 176.81 |

Latency is measured on the benchmark client from dispatch until the HTTP response is parsed. The two staff requests in a round are dispatched concurrently.

### SSE committed-update latency

| Statistic | Milliseconds |
|---|---:|
| Samples | 300 |
| Average | 97.75 |
| Median | 93.93 |
| p95 | 121.74 |
| Minimum | 75.83 |
| Maximum | 165.67 |

For each client, latency starts immediately before concurrent call-next dispatch and ends when that client receives a post-commit snapshot containing both called token IDs.

### Scenario timing and throughput

| Metric | Observed |
|---|---:|
| Total recorded scenario time | 20.48 s |
| Run 1 | 7.93 s |
| Run 2 | 6.40 s |
| Run 3 | 6.04 s |
| Call-next requests / total scenario second | 5.86 |

The 5.86 figure divides 120 call-next requests by the whole 20.48-second scenario, which also includes token creation, start/complete transitions, JSON parsing, and SSE synchronization. It is intentionally **not** labeled maximum throughput.

## What this benchmark proves

- Under this exact two-staff, 60-round local scenario, no token was returned twice.
- PostgreSQL locking allowed two staff to receive distinct tokens concurrently.
- Five connected SSE clients per run saw the combined committed assignment state with the latency distribution above.
- The utility is repeatable and exits non-zero if a duplicate or request failure is observed.

## What it does not prove

- It does not establish maximum users, capacity, production latency, multi-host behavior, or long-duration stability.
- It does not test more than two concurrent staff because only two demo staff accounts are configured.
- It does not test multiple backend instances; SSE state is intentionally in-process.
- Results are sensitive to hardware, Docker resource allocation, database state, warm-up, and other host workload.

## Reproducing or changing the workload

PowerShell example:

```powershell
$env:RUNS='5'
$env:ROUNDS='50'
$env:SSE_CLIENTS='10'
node .\benchmarks\linepilot-benchmark.mjs
```

Bash example:

```bash
RUNS=5 ROUNDS=50 SSE_CLIENTS=10 node benchmarks/linepilot-benchmark.mjs
```

Record the raw JSON output with the date/environment, then update all Version B resume numbers. Never reuse the checked-in numbers after changing code or environment without re-running.
