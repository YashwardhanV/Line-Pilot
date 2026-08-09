# Final SDE-1 Audit

Audit date: 7 August 2026.

## Scores

| Area | Score | Evidence and remaining gap |
|---|---:|---|
| Java fundamentals | 8/10 | Immutable records, enums, entity behavior, clock injection, streams used selectively, executor/latch concurrency tests. Could add richer domain value objects and more unit-level service tests. |
| Spring Boot knowledge | 9/10 | MVC, validation, JPA, Security, transactions, events, Actuator, environment config, Flyway, OpenAPI, centralized exceptions. |
| REST API design | 8/10 | DTOs, validation, correct status families, Location header, pagination, ProblemDetail, action endpoints. No versioning/idempotency strategy yet. |
| Database design | 9/10 | Normalized three-table scope, foreign/check/unique constraints, partial/query-specific indexes, migration ownership, PostgreSQL integration tests. |
| Transaction knowledge | 9/10 | Pessimistic locks for numbering, `FOR UPDATE SKIP LOCKED` for claims, after-commit SSE, database safety net, executable concurrent tests. |
| Testing | 9/10 | 15 backend tests including PostgreSQL concurrency/security/API/constraints plus frontend lint/build/component test. Browser E2E is absent. |
| Reliability | 8/10 | Rollback-safe state changes, constraints, cleanup callbacks, health checks, structured errors, benchmark failure detection. No rate limiting, idempotency key, or durable event delivery. |
| Frontend integration | 8/10 | Typed API models, customer/staff flows, accessible controls, responsive Tailwind UI, live SSE, safe minimal local storage. Admin UI and browser E2E are absent. |
| Deployment/run experience | 9/10 | One Compose command, health-gated startup, non-root backend container, persistent PostgreSQL, environment example, CI. No hosted deployment is claimed. |
| Interview defensibility | 9/10 | Narrow architecture, explicit tradeoffs, code/test/benchmark claim map, limitations, and dedicated guide. |

Overall: **8.6/10** for the intended SDE-1 portfolio level.

## 1. Is anything still unnecessarily SDE-2/SDE-3?

No distributed-system layer remains. `FOR UPDATE SKIP LOCKED`, partial indexes, after-commit events, and SSE may sound advanced, but each directly solves a domain requirement and is implemented in a small, inspectable form.

The command/query service split is only package organization; it is not CQRS. OpenAPI, Flyway, Testcontainers, Docker, and CI are normal professional fundamentals. None requires a platform team or separate service ownership.

## 2. Is anything too basic or tutorial-like?

The product is deliberately small, but not basic CRUD. The non-tutorial depth is:

- transactional concurrent claiming;
- database-enforced daily numbering and active ownership;
- explicit lifecycle invariants;
- committed live-update delivery;
- moving-average service estimation;
- security/role integration tests;
- database-specific constraint and concurrency tests;
- measured external HTTP/SSE benchmark.

The frontend component test is minimal, and the admin API has no UI. A single browser E2E happy path would be the best next testing addition—not another infrastructure technology.

## 3. Is every technology justified?

| Technology | Justification |
|---|---|
| Java 21 / Spring Boot | Primary target stack and strong framework fundamentals. |
| Spring MVC | REST/SSE endpoints and validation. |
| Spring Data JPA / Hibernate | Relational persistence and transaction participation. |
| Spring Security | Database-backed staff/admin access control. |
| PostgreSQL | Relational state, constraints, partial indexes, and row-locking semantics. |
| Flyway | Reviewable schema migrations; Hibernate only validates. |
| React + TypeScript | Requested SPA stack with compile-time API contracts. |
| Tailwind CSS | Requested styling stack and responsive accessible UI. |
| SSE | Simplest server-to-client live update protocol for this requirement. |
| Testcontainers | Real PostgreSQL behavior for locks and indexes; H2 is insufficient. |
| Docker / Compose | Five-minute reproducible app + database startup. |
| Nginx | Serves the static SPA and proxies REST/SSE in the Compose deployment. |
| OpenAPI | Discoverable, runnable API documentation. |
| GitHub Actions | Basic repeatable CI for backend, frontend, and Compose validation. |

Removed as unjustified: microservices, gateway, Redis, Kafka/RabbitMQ, Kubernetes/Helm, tracing/metrics stacks, QR, SMS, multi-database, CQRS/event sourcing, support tickets, and mutable uploads.

## 4. Can every resume bullet be proven?

Yes, with a condition: Version B numbers are proven only for the recorded environment and code state in `BENCHMARK_RESULTS.md`. If code, hardware, Docker allocation, or workload changes, rerun and update both files.

Version A claims map directly to implementation and tests. Version B maps concurrency numbers and SSE distribution to raw benchmark output captured on 7 August 2026. Neither version claims internet-scale capacity, production readiness, or absolute security.

## 5. Ten most likely interview questions

1. Why can two transactions call the same token, and how does your query prevent it?
2. What is the difference between `FOR UPDATE`, `SKIP LOCKED`, and optimistic locking?
3. What happens when two staff call next but only one token is waiting?
4. Why publish SSE after commit, and what happens on rollback?
5. Why did you choose SSE instead of WebSocket or polling?
6. How are daily token numbers generated safely under concurrent joins?
7. Which lifecycle transitions are legal, and where are they enforced?
8. How is estimated wait calculated, and what are its limitations?
9. Why use real PostgreSQL in tests instead of H2 or mocked repositories?
10. What would you change before deploying this to the public internet or multiple backend instances?

Concise answer themes are in `docs/INTERVIEW_GUIDE.md`.

## 6. What must be understood before adding it to a resume?

1. Walk through the exact SQL and transaction for call-next.
2. Explain why `@Transactional` alone does not prevent duplicate selection.
3. Run the concurrency tests and interpret the one-token and two-token cases.
4. Draw the state machine from memory and explain invalid transitions.
5. Explain the two database uniqueness protections and three query indexes.
6. Distinguish queue waiting time from actual service duration.
7. Explain the after-commit SSE flow and single-instance limitation.
8. Explain Basic auth’s HTTPS requirement, CSRF decision, and why it is a demo tradeoff.
9. Run `docker compose up --build`, tests, and the benchmark yourself.

## Recommended next change

Add a Playwright/Cypress end-to-end test that joins as a customer, signs in as staff, calls/serves/completes the token, and observes the live board. Do this before adding any cache, broker, or orchestration layer.
