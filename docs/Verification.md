# Milestone 3 verification

Validation date: October 27, 2026. Java 17 / Spring Boot 4.1.1.

## Executed

- Maven clean verify: BUILD SUCCESS.
- Tests discovered: 26; passed: 24; failures: 0; errors: 0; skipped: 2.
- Required outage integration test: passed. Actual login, booking, cancellation,
  and rebooking HTTP requests succeeded while the notification URL was unreachable.
- Commit-before-HTTP and released-slot-lock checks: passed.
- Durable pending delivery/replay and receiver duplicate protection: passed.
- Timeout, retry, validation, health/metrics, and original concurrency tests: passed.
- Executable Spring Boot JAR built and repackaged successfully.
- POM XML, Compose YAML, workflow push/pull_request triggers: parsed and checked.

## Environment limits and remaining evidence

- Two PostgreSQL-specific tests were skipped locally because a disposable PostgreSQL
  server was not available. CI is configured to supply PostgreSQL 16 and run them.
- Docker is unavailable in this workspace. Compose was checked structurally; an
  actual image build/container startup must still be checked in Docker Desktop or CI.
- GitHub Actions has not been run here; push these files and inspect its result.
- Record the minimum five-minute code walkthrough and add its link before submitting.
- No Docker/CI screenshots, real email delivery, or external deployment are claimed.

## Per-class results

| Test class | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| BookingConcurrencyTest | 1 | 0 | 0 | 0 |
| GameCoachingBookingApplicationTests | 1 | 0 | 0 | 0 |
| NotificationHttpIntegrationTest | 7 | 0 | 0 | 0 |
| NotificationOutageIntegrationTest | 3 | 0 | 0 | 0 |
| PostgresIntegrationTest | 2 | 0 | 0 | 2 |
| AuthInterceptorTest | 2 | 0 | 0 | 0 |
| HttpNotificationGatewayTest | 4 | 0 | 0 | 0 |
| ManagementControllerTest | 2 | 0 | 0 | 0 |
| BookingServiceTest | 3 | 0 | 0 | 0 |
| ProviderServiceTest | 1 | 0 | 0 | 0 |
