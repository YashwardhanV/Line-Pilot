# LinePilot Interview Guide

Use this to understand the project, not to memorize impressive words. Open the named classes while practicing explanations.

## 1. Concurrent “call next” correctness

**Problem:** Two staff members can press “Call next” at the same time. A normal read-then-update implementation can return the same waiting token to both requests.

**How it works:** `QueueTokenRepository.lockNextWaitingToken` executes a PostgreSQL `FOR UPDATE SKIP LOCKED` query ordered by priority, join time, and ID. `QueueCommandService.callNext` runs that query, changes the selected entity to `CALLED`, records the staff account, flushes, and commits in one transaction. A partial unique index also permits only one active `CALLED`/`SERVING` token per staff account.

**Important classes:** `QueueCommandService`, `QueueTokenRepository`, `QueueToken`, `QueueConcurrencyIntegrationTest`.

**Tables/indexes:** `queue_tokens`; `idx_queue_tokens_call_next`; `uk_queue_tokens_active_staff_claim`.

**Alternatives considered:**

- Java `synchronized`: protects one JVM only and is disconnected from the database transaction.
- Optimistic locking: can detect a collision but needs a careful retry loop and can create noisy conflicts under contention.
- Plain pessimistic lock without `SKIP LOCKED`: safe, but the second staff request may wait on the first row instead of claiming another available row.
- Redis/distributed lock: unnecessary because PostgreSQL already owns the state and there is one backend.

**Failure cases:** no waiting token returns 409; one token/two staff yields one success and one 409; one staff with an active claim gets 409; transaction failure rolls back claim and state together.

**Tradeoff:** the query is PostgreSQL-specific. That is accepted because PostgreSQL is an explicit system dependency and correctness matters more than theoretical database portability.

**Likely questions and concise answers:**

- *Why doesn’t `@Transactional` alone solve it?* A transaction makes each request atomic but does not stop two transactions from reading the same eligible row before either updates it.
- *What does `SKIP LOCKED` do?* It ignores rows another transaction has locked, so another worker can claim the next eligible token.
- *How did you prove it?* A Testcontainers test launches two real concurrent service calls against PostgreSQL. With one token it asserts exactly one success; with two it asserts two distinct IDs.

## 2. Explicit state machine and transaction boundaries

**Problem:** Queue actions are not arbitrary CRUD updates. Completing a waiting token or cancelling a serving token would corrupt history.

**How it works:** `QueueToken` exposes domain methods (`call`, `startServing`, `complete`, `skip`, `cancel`) that check current state and claim ownership. Controllers cannot set status fields. Every command service method is transactional.

**Important classes:** `QueueToken`, `TokenStatus`, `QueueCommandService`, `QueueTokenStateTest`, `ApiExceptionHandler`.

**Tables:** `queue_tokens`, especially `status`, `claimed_by_id`, and lifecycle timestamps.

**Alternatives considered:** controller-level `if` statements, a generic update-status endpoint, or a state-machine library. Entity methods keep invariants close to data without adding a library for six states.

**Failure cases:** stale client action, action by a different staff member, duplicate cancel, and invalid order all fail with 409 and leave the row unchanged.

**Tradeoff:** the entity knows transition rules, while service code owns repository lookups and transactions. This is modest domain modeling, not full DDD infrastructure.

**Likely question:** *Why record separate timestamps?* They let the system distinguish queue wait, time-to-answer, actual service duration, and terminal time; using one timestamp would make estimates and audit history ambiguous.

## 3. Daily token generation

**Problem:** Human-friendly numbers should restart each day, but concurrent joins must not produce the same number.

**How it works:** `joinQueue` pessimistically locks the `service_queues` row. `ServiceQueue.nextSequence` resets when the UTC date changes and increments the stored counter. The token insert has a unique `(queue, service_date, sequence_number)` constraint.

**Important classes:** `ServiceQueue`, `QueueCommandService`, `ServiceQueueRepository`.

**Tables:** `service_queues.sequence_date`, `service_queues.last_sequence`, and `queue_tokens` unique daily sequence.

**Alternatives considered:** `MAX(sequence)+1` races; a global database sequence does not reset per queue/day; UUID-only tokens are safe but unfriendly to call aloud.

**Failure cases:** transaction rollback restores the sequence increment; the uniqueness constraint rejects any duplicate if a future code path bypasses the locked method.

**Tradeoff:** joins for the same queue serialize briefly on one small row. That is appropriate at this scale and keeps the rule obvious.

**Likely question:** *Why UTC?* The demo uses one deterministic boundary. A real multi-location product would store the queue’s business timezone and derive its service date there.

## 4. Wait-time estimation

**Problem:** Customers need a useful, defensible estimate without an opaque model.

**How it works:** `QueueQueryService` loads up to ten recent completed tokens with a real `serving_at`. `WaitTimeEstimator` averages `serving_at → completed_at`, rounds to minutes, and multiplies by people ahead. With no history, it uses the queue’s configured default.

**Important classes:** `WaitTimeEstimator`, `QueueQueryService`, `WaitTimeEstimatorTest`.

**Tables/index:** `queue_tokens.serving_at`, `completed_at`; `idx_queue_tokens_wait_estimate`; `service_queues.default_service_minutes`.

**Alternatives considered:** total time from join to completion (mixes waiting and service), all-time average (slow to adapt), ML prediction (unjustified data/complexity), fixed estimate (ignores recent behavior).

**Failure cases:** zero/negative durations are excluded; no samples use fallback; terminal non-completed states are excluded.

**Tradeoff:** the estimate ignores parallel staff capacity and time-of-day effects. It is explainable and honestly labeled an estimate.

**Likely question:** *Why the last ten?* It is a small moving window that adapts while remaining easy to query and explain; the number is configurable future work, not a scientifically optimized constant.

## 5. Live updates with SSE

**Problem:** Customer/staff boards should update without manual refresh or aggressive polling.

**How it works:** `PublicQueueController.events` registers an `SseEmitter`. `QueueEventStream` stores emitters in thread-safe collections and sends typed snapshots. Commands publish an application event; `QueueChangedListener` broadcasts only in `AFTER_COMMIT`, so clients never see rolled-back state. Browser `EventSource` automatically reconnects.

**Important classes:** `QueueEventStream`, `QueueChangedEvent`, `QueueChangedListener`, `QueueQueryService`, `useQueueEvents.ts`.

**Tables:** SSE has no table; snapshots read committed `service_queues` and active `queue_tokens`.

**Alternatives considered:** polling (wasted requests/stale intervals), WebSocket/STOMP (bidirectional protocol not needed), Kafka/Redis pub-sub (no multi-instance requirement).

**Failure cases:** disconnect/error/timeout removes the emitter; browser reconnects; server restart drops subscribers but not queue state; failure before commit emits nothing.

**Tradeoff:** emitter state is local to one backend instance. That limitation is documented instead of hidden behind unused distributed infrastructure.

**Likely question:** *Why after commit?* Publishing inside the transaction could query old state or send an update that later rolls back. After-commit aligns the live view with durable state.

## 6. REST API, DTOs, and errors

**Problem:** The API needs stable contracts, validation, correct status codes, and no accidental entity serialization.

**How it works:** record DTOs define requests/responses. Controllers return 201 + `Location` for joins/queue creation, 200 for returned state, 400 for validation, 401/403 for security, 404 for absence, and 409 for state conflicts. `ApiExceptionHandler` produces `ProblemDetail` and a validation field map. History is page/size bounded to 100.

**Important classes:** all controllers, `dto/*`, `ApiExceptionHandler`, `QueueApiIntegrationTest`.

**Tables:** none directly; DTOs prevent lazy relationships/password hashes from leaking.

**Alternatives considered:** exposing entities (couples persistence/API and risks recursion), returning 200 for every outcome, or a custom error envelope. Spring `ProblemDetail` follows a standard shape.

**Failure cases:** malformed/blank names, missing resources, illegal state, database constraint violation, bad credentials, and wrong role.

**Tradeoff:** transition actions use verbs (`/start`, `/complete`) because they are domain commands, which is clearer than pretending each is a generic resource replacement.

**Likely question:** *Why is no-waiting-token 409 instead of 404?* The queue exists, but the requested transition conflicts with its current state.

## 7. Security

**Problem:** Only staff should mutate queue operations, and only admins should manage queues.

**How it works:** `DatabaseUserDetailsService` loads BCrypt-hashed accounts. `SecurityConfig` applies role rules and stateless Basic authentication. The React staff page keeps the authorization value only in memory.

**Important classes:** `SecurityConfig`, `DatabaseUserDetailsService`, `UserAccount`, `AuthController`, `QueueApiIntegrationTest`.

**Tables:** `user_accounts`.

**Alternatives considered:** JWT adds signing/refresh/revocation concerns with little benefit for this one server; secure cookie sessions improve UX/credential reuse but require CSRF/session design. Basic was chosen for a local demo and must sit behind HTTPS outside localhost.

**Failure cases:** bad credentials 401; wrong role 403; disabled account cannot authenticate; public token UUID is a possession secret and has no recovery flow.

**Tradeoff:** simple and interview-defensible, but not a final internet-facing authentication UX. Never say “100% secure” or “production-ready.”

**Likely question:** *Why disable CSRF?* The API is stateless and does not use ambient browser cookies; the browser explicitly sends an Authorization header. If moved to cookies, CSRF protection must be reintroduced.

## 8. Relational design and migrations

**Problem:** Correctness should survive application bugs and schema changes should be reviewable.

**How it works:** Flyway creates three tables, foreign keys, check/unique constraints, and query-specific indexes. Hibernate uses `ddl-auto=validate`, so entity/schema drift fails startup rather than silently altering data.

**Important files:** `V1__create_linepilot_schema.sql`, entity classes, repository interfaces, `QueueRepositoryConstraintIntegrationTest`.

**Tables:** `user_accounts`, `service_queues`, `queue_tokens`.

**Alternatives considered:** `ddl-auto=create/update` is easy for tutorials but not reviewable; separate active/history tables create synchronization; multiple databases add no benefit.

**Failure cases:** duplicate daily number, invalid status/priority, missing foreign key, duplicate active staff claim.

**Tradeoff:** PostgreSQL partial indexes reduce portability but express the invariant precisely.

**Likely question:** *Why is history in the token table?* A token changes through a short lifecycle and then becomes history. Keeping one row preserves identity and avoids copying/synchronizing audit data.

## 9. Testing and benchmark strategy

**Problem:** Coverage percentage cannot prove the concurrency invariant or database behavior.

**How it works:** fast unit tests cover estimation and state transitions. PostgreSQL Testcontainers tests cover constraints, repositories, security, REST, ordering/cancellation, and transactions. The concurrency test uses latches/executor threads. The external Node benchmark drives the Dockerized HTTP/SSE path and exits non-zero on duplicates/failures.

**Important files:** `backend/src/test`, `benchmarks/linepilot-benchmark.mjs`, `BENCHMARK_RESULTS.md`.

**Tables:** all three are exercised by integration tests.

**Alternatives considered:** H2 would not accurately test PostgreSQL locking/partial indexes; mocked repositories cannot prove transaction isolation; fabricated resume percentages are rejected.

**Failure cases:** Docker unavailable prevents integration tests; benchmark results vary by host; the benchmark deliberately states what it does not prove.

**Tradeoff:** the backend test suite takes longer because it boots PostgreSQL, but the critical behavior is database-specific.

**Likely question:** *What is the strongest test?* The one-token/two-staff concurrent test: exactly one call succeeds and the single row has one claimant. It directly targets the previously identified race.

## 10. Docker and CI

**Problem:** A reviewer should see useful behavior quickly and each change should be verifiable.

**How it works:** Compose starts PostgreSQL, waits for health, starts Spring Boot/Flyway, waits for `/actuator/health`, then starts Nginx/React. CI runs Maven/PostgreSQL Testcontainers, frontend lint/test/build, and Compose configuration validation.

**Important files:** `docker-compose.yml`, both Dockerfiles, `nginx.conf`, `.env.example`, `.github/workflows/ci.yml`.

**Alternatives considered:** Kubernetes/Helm is disproportionate; local-only setup is harder to review; adding PostgreSQL directly to CI would duplicate Testcontainers lifecycle management.

**Failure cases:** occupied ports, low Docker memory, image registry/network failure, or invalid environment values. Health/dependency checks prevent the frontend from presenting before the backend is ready.

**Tradeoff:** first build downloads Maven/npm layers and is slower; subsequent builds cache them.

## Practice rule

For every resume bullet, be able to show the code, the test, and the documented limitation. If you cannot explain why an alternative was rejected, do not present the feature as your engineering decision.
