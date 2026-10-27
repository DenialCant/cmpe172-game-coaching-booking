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

## Verified on the student's Windows / Docker Desktop environment

- Local Maven clean verify: BUILD SUCCESS, 26 discovered, 24 passed, 2 skipped.
- Docker image build: passed, including Maven verify and compilation of the health probe.
- Docker Compose: app and PostgreSQL containers both healthy.
- Containerized booking and cancellation: each committed, accepted by the mock,
  and delivered with its own notification type (user-provided terminal logs).
- Manual outage: disabling the mock produced HTTP 404; appointment 2 still committed
  at 21:18:07 UTC and its stable notification ID remained pending.
- Recovery: after re-enabling the mock and recreating only the app container, the
  scheduled worker delivered appointment-2-booking-confirmation at 21:21:10 UTC.
  No replacement booking was made. This confirms persistence across app recreation.
- Git push: implementation commit af64ac2 reached origin/main.
- GitHub Actions: run #1 for af64ac2 passed in 1m29s, verified from the supplied
  screenshot. The workflow includes Maven verify with PostgreSQL 16, Compose
  validation, and a Docker image build. Exact CI test totals were not separately
  captured; local counts above are from the actual Maven output.

## Remaining submission items

- Record the minimum five-minute code walkthrough and add its link.
- Copy these final docs into the local project and commit/push the documentation update.
- Package final code, report, and diagram as CMPE172_Milestone3_Daniel_Khant.zip.
- This is mock delivery; no real email/SMS or external cloud deployment is claimed.

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
