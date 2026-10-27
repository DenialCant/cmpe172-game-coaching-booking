# Skill Cap - Game Coaching Booking System

Spring Boot 4.1.1 / Java 17, Thymeleaf, JDBC, and PostgreSQL. Milestone 2 booking,
role-based login, provider tools, and concurrency protection are preserved.
Milestone 3 adds an HTTP notification Gateway, durable fallback/recovery,
observability, Docker Compose, and continuous integration.

## Apply this update on Windows

1. Stop the old application with Ctrl+C and back up your project folder.
2. Copy the CONTENTS of this folder over your existing project. Keep your existing
   `.git` directory. Include `.github`, `.mvn`, `.dockerignore`, and `.env.example`.
3. Run tests before starting the app:

```bat
mvnw.cmd clean verify
```

The normal test profile uses isolated in-memory H2 databases and needs no PostgreSQL
password. `PostgresIntegrationTest` contains two additional tests enabled only when
`POSTGRES_TEST_URL` is set. GitHub Actions supplies a disposable PostgreSQL service.
Do not point that variable at your personal database; those two tests delete test data.

## Run with your existing PostgreSQL database

Ensure PostgreSQL is running and the `game_coaching` database exists. In Command Prompt:

```bat
set DB_PASSWORD=your_postgres_password
mvnw.cmd spring-boot:run
```

Optional: `set DB_USERNAME=your_postgres_username` or
`set DB_URL=jdbc:postgresql://localhost:5432/game_coaching`.
Open http://localhost:8080 and keep the terminal running.

| Role | Username | Password |
|---|---|---|
| Customer | player1 | password |
| Customer | player2 | password |
| Provider | coachclark | password |

Passwords are BCrypt hashes in the database. Demo seeding refreshes the three demo
accounts on startup. Existing appointments and availability are preserved. Fresh
databases receive slots seven/eight days in the future. If your existing slots have
expired, log in as `coachclark` and create new future availability.

## Run the app and PostgreSQL with Docker

Start Docker Desktop with Linux containers, then:

```bat
copy .env.example .env
```

Edit `.env` and replace `DB_PASSWORD` and `NOTIFICATION_TOKEN`. Then run:

```bat
docker compose up --build -d
docker compose ps
docker compose logs -f app
```

Open http://localhost:8080. Compose uses a NEW database in a Docker volume; it does
not import or change the PostgreSQL database installed on your laptop. The app
waits for PostgreSQL readiness. Its container health probe calls `/health`.
`docker compose down` stops containers while retaining the volume and bookings.
The database port is internal to Compose, so it does not conflict with local 5432.
Changing a DB password in `.env` does not change an already-initialized volume's
password: retain the original value or deliberately update the database user first.

## Milestone 3 notification flow

`BookingService` suspends any caller transaction. It calls the separate proxied
`BookingTransactionService`, which locks the slot with `SELECT ... FOR UPDATE`,
writes the appointment, and queues a notification in `notification_outbox` in the
same transaction. After that method commits and returns, `NotificationDispatcher`
calls the Gateway over HTTP. Cancellation follows the same commit-then-notify flow.

The single coarse-grained DTO contains `notificationId`, `recipient`, `type`, and
`message`. No database entity, password, or sequence of fine-grained lookups crosses
the HTTP interface. The mock `POST /internal/notifications` controller stores the
request and logs acceptance; it does NOT send real email or SMS.

Default per-attempt connection/read timeouts are 500/1000 ms. Up to three attempts
reuse the same ID and payload, with 100/200 ms backoff. Network errors, 5xx, and 429
are retried; other 4xx errors stop immediate retries. If delivery fails, the booking
still succeeds and the outbox entry stays PENDING. A worker retries up to 25 pending
events every 30 seconds, beginning 30 seconds after startup. Successfully delivered
events become DELIVERED. Receiver primary keys reject duplicate side effects even
after an app restart; changed payloads using the same ID receive 409.

This provides at-least-once attempts with idempotent mock acceptance. The application
does not claim exactly-once real email delivery. A real external receiver must honor
the same idempotency contract. Sender and mock receiver share PostgreSQL in this
assignment deployment; they are separate HTTP interfaces, not independently deployed
microservices. Retained outbox/receipt data needs a retention policy for long-term use.

## Health, metrics, and logs

```bat
curl http://localhost:8080/health
curl http://localhost:8080/metrics
```

`/health` returns HTTP 200 and `{"status":"UP","database":"UP"}` when a database
connection validates. It returns HTTP 503 with DOWN fields if validation fails.
Pool acquisition waits at most three seconds; connection validation uses two seconds.
Notifications are a noncritical dependency and are not included in readiness.

`/metrics` returns JSON counters since process startup: `bookings_total`,
`failed_bookings_total`, `cancellations_total`, notification HTTP attempts, delivery
completions/deferrals, and average booking service latency in milliseconds. Latency
includes notification waiting and failed booking calls. A replay can increment a
delivery completion more than once; it is not a unique-email count. Counters reset
on restart and are per instance, not a Prometheus endpoint or hourly aggregate.

INFO logs include committed operations and accepted/delivered notification IDs/types.
WARN logs include rejected bookings, HTTP attempt failures, and deferred delivery.
ERROR logs include database-health failures, failed outbox scans, and unexpected
dispatch failures. Logs omit passwords, tokens, recipient addresses, and message text.

## Demonstrate notification failure and recovery

For a LOCAL Maven run, stop the app and run in the same Command Prompt:

```bat
set NOTIFICATION_STUB_ENABLED=false
mvnw.cmd spring-boot:run
```

Log in as `player1` and book a future slot. Booking still confirms; the logs show a
failed notification and a deferred ID. `/health` stays UP while PostgreSQL works.
Cancellation also succeeds and queues its own confirmation.
Stop the app, re-enable the controller, and restart:

```bat
set NOTIFICATION_STUB_ENABLED=true
mvnw.cmd spring-boot:run
```

After about 30 seconds, pending notifications are accepted and marked DELIVERED.
No rebooking is needed. For a refused TCP connection instead of a missing controller,
temporarily set `NOTIFICATION_BASE_URL=http://localhost:65534`; clear it with
`set NOTIFICATION_BASE_URL=` before recovery. The automated outage tests use a closed
loopback port, independently of this manual demo.

For Docker, edit `NOTIFICATION_STUB_ENABLED=false` in `.env`, run
`docker compose up -d --force-recreate app`, and book/cancel. Restore `true` and
recreate the app again; the outbox survives in the database volume.

## Tests and CI

The suite covers the original RBAC/provider/booking rules and concurrent booking,
real HTTP booking/cancellation with notifications unreachable, transaction rollback,
commit visibility and released slot locks before HTTP, durable replay/deduplication,
slow HTTP timeouts, retry policy, DTO validation, and health/metrics responses.
H2 uses a generated BOOKED-only unique key to model the PostgreSQL partial index.
The conditional PostgreSQL tests run the production SQL against PostgreSQL 16 in CI.

`.github/workflows/ci.yml` builds and tests on every push and pull request, validates
Compose, and builds the Docker image. The implementation was pushed as `af64ac2`, and Actions run #1 passed.
The student also verified Docker startup, book/cancel notifications, and outage recovery. No hosting service, paid API, or cloud deployment is required by this PDF.

## Files for your walkthrough

| File | Show |
|---|---|
| `service/BookingService.java` | Commit-return-notify ordering, metrics and logs |
| `service/BookingTransactionService.java` | Atomic appointment and outbox writes |
| `notification/NotificationRequest.java` | Coarse-grained DTO |
| `notification/HttpNotificationGateway.java` | HTTP, timeouts, retry and stable IDs |
| `notification/NotificationDispatcher.java` | Fallback and periodic recovery |
| `notification/MockNotificationController.java` | Validation, token and deduplication |
| `observability/ManagementController.java` | Health and metric endpoints |
| `NotificationOutageIntegrationTest.java` | Required outage test |
| `Dockerfile`, `docker-compose.yml`, `.github/workflows/ci.yml` | Delivery |

See `docs/` for the report, distribution diagram, verification notes, and code-demo
script. The report and verification notes include the observed Docker/Actions results.
Add your recorded video link before submission. The walkthrough must explain code for at least five minutes.
