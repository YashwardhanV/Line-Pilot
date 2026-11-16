# LinePilot

A digital queue for service desks. Customers take a token (like `A-007`) from their phone, see their place in line live, and get a text when it's their turn. Staff call the next customer, mark no-shows and complete service.

Built by **Yashwardhan Verma** · [GitHub](https://github.com/YashwardhanV) · [LinkedIn](https://www.linkedin.com/in/yashwardhanv)

**Stack:** Java 21 · Spring Boot 3 · PostgreSQL 16 · React 18 + TypeScript + Tailwind · Docker Compose

## What it does

- **Customers** join without an account, see people ahead and an estimated wait, and can leave the queue. An optional phone number gets an SMS or WhatsApp message when they are called ("please go to Counter 2"). A no-show can rejoin once within 30 minutes.
- **Staff** sign in, call the next customer, start and complete service, mark no-shows and browse history.
- **The board** updates live over Server-Sent Events.

## Run it

```bash
cp .env.example .env    # optional, defaults work
docker compose up --build
```

- App: <http://localhost:3000>
- Staff: <http://localhost:3000/staff> (`staff1` or `staff2`, password `demo123`)
- API docs: <http://localhost:8080/swagger-ui.html>

By default "your turn" messages are only written to the backend log. To send real ones, set `NOTIFY_PROVIDER=twilio` and the `TWILIO_*` values in `.env`.

## Tests

```bash
cd backend && mvn verify          # needs Docker (Testcontainers runs real PostgreSQL)
cd frontend && npm ci && npm run lint && npm test && npm run build
```

## The parts worth reading

- **Two staff never get the same customer.** "Call next" picks the oldest waiting token with `SELECT … FOR UPDATE SKIP LOCKED`. `QueueConcurrencyIntegrationTest` races two threads against real Postgres to prove it.
- **Token numbers never repeat.** Joining locks the queue row while it bumps the daily counter, and a unique constraint backs it up. Numbers restart at the desk's local midnight (`APP_TIME_ZONE`).
- **Yesterday's leftovers don't block today.** A job after midnight expires tokens that were never served and deletes stored phone numbers.
- **Messages go out only after the database commits**, on a background thread, so a slow SMS provider never slows staff down.

More: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)

## Known limitations

- Not yet tried at a real desk with real customers.
- One backend instance only: live-board subscribers and rate-limit counters are kept in memory.
- Anyone with a token's tracking link can view or cancel it. Customers have no accounts.
- Queues and staff come from seed data. There is no admin screen.
- WhatsApp needs a Twilio-approved message template in production, and SMS in India needs DLT registration.
- Staff sign in with HTTP Basic, which must run behind HTTPS outside localhost.
- No browser end-to-end tests yet.
