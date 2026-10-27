# Skill Cap Milestone 3 - code walkthrough (about 6 minutes)

Open these files in advance. Keep your database password and `.env` off screen.

## 0:00-0:25 - project folders

"Hi, this is Skill Cap, my game coaching booking system. For Milestone 3 I kept
the existing customer and provider workflows and added an HTTP notification boundary,
failure recovery, health checks, metrics, Docker, and a GitHub Actions pipeline."

## 0:25-1:20 - BookingService.book and BookingTransactionService.book/enqueue

"This is the important part of the booking flow. BookingService calls a separate
transaction service. Inside that transaction, the application locks the slot,
creates the appointment, and writes its notification to an outbox table.
Those database writes either both commit or both roll back.

When the transaction method returns, the commit is finished. Only then do we call
the notification dispatcher. The remote call does not hold the slot lock, and a
notification failure cannot roll back a successful booking. Cancellation follows
the same pattern. The extra outbox table also preserves the notification if the
application restarts between committing and sending it."

## 1:20-2:20 - NotificationRequest and HttpNotificationGateway.send

"The Gateway sends one complete DTO containing the recipient, notification type,
message, and a stable notification ID. This is a coarse-grained interface because
the receiver gets everything it needs in one request.

Here are the connection and read timeouts. By default the Gateway makes at most
three immediate attempts with a short increasing delay. Network errors, server
errors, and rate limiting can be retried. Other client errors stop those immediate
retries. Every attempt uses the same notification ID and payload."

## 2:20-3:15 - MockNotificationController.accept and NotificationDispatcher

"The second controller stands in for the external notification service. It validates
the DTO and a shared token, stores the notification, and logs its ID. It does not
send a real email. The ID is the primary key, so retrying the same event does not
store it twice. Reusing the same ID for different data is rejected.

If the Gateway cannot deliver, the dispatcher leaves the notification pending.
This scheduled method checks pending entries every thirty seconds. When the service
recovers, it sends them again and marks them delivered. The database keeps both the
pending events and the receiver's duplicate protection across restarts."

## 3:15-3:55 - ManagementController and SchedulerMetrics

"The health endpoint checks an actual database connection. It returns UP with HTTP
200, or DOWN with HTTP 503. A notification outage does not make the scheduler unhealthy
because bookings can still work. Metrics include successful and failed bookings,
cancellations, notification attempts, and average booking latency. They are process
counters, so they reset on restart. INFO logs show committed work, WARN logs show
retry or deferral, and ERROR logs show operational failures."

## 3:55-4:40 - NotificationOutageIntegrationTest and test output

"This test starts the actual application and logs in through HTTP. Its notification
URL points to an unavailable endpoint. It verifies that booking still returns a
confirmation redirect, the appointment is committed, and the notification stays
pending. It also checks cancellation and rebooking.

Other tests check slow-service timeouts, identical retries, duplicate acceptance,
outbox rollback, and recovery. The original concurrency test still ensures only
one customer can book a slot. Show your actual Maven result here."

## 4:40-5:15 - Dockerfile, docker-compose.yml, .github/workflows/ci.yml

"The Dockerfile builds and tests the project and then runs it in a smaller Java
runtime image as a non-root user. Compose starts the app and PostgreSQL, waits for
database readiness, and keeps the data in a volume. GitHub Actions builds and tests
every push, including PostgreSQL integration tests, and builds the Docker image."

Only say the Docker/Actions runs passed after checking your actual runs.

## 5:15-6:15 - website and endpoints

"Finally, here is the application. I'll log in as a customer and book a future
session. The booking confirmation is the same user workflow, with notification
handling happening afterward. Here are the health and metrics endpoints, and here
are the notification IDs in the logs."

If prepared in advance: demonstrate `NOTIFICATION_STUB_ENABLED=false`, show that a
booking/cancellation succeeds with deferred logs, then restart with true and show
recovery after the worker runs. End with: "That completes the Milestone 3 workflow."
