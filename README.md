# Skill Cap - Game Coaching Booking System

Skill Cap is a Spring Boot, Thymeleaf, JDBC, and PostgreSQL appointment system created for CMPE 172. Milestone 2 adds session login, role-based access, booking and cancellation, provider availability management, pagination, validation, transactions, and concurrency protection.

## Demo accounts

| Role | Username | Password |
|---|---|---|
| Customer | `player1` | `password` |
| Customer | `player2` | `password` |
| Provider | `coachclark` | `password` |

The database stores BCrypt hashes, not plaintext passwords.

## Run on Windows

1. Make sure PostgreSQL is running and the `game_coaching` database exists.
2. Open Command Prompt inside the project folder.
3. Set the PostgreSQL password for the current terminal:

```bat
set DB_PASSWORD=your_postgres_password
```

If the PostgreSQL username is not `postgres`, also run:

```bat
set DB_USERNAME=your_postgres_username
```

4. Start the application:

```bat
mvnw.cmd spring-boot:run
```

5. Open `http://localhost:8080`.

The application runs `schema.sql` and `seed.sql` safely on startup. Existing appointments are preserved.

## Run tests

```bat
mvnw.cmd test
```

The test suite includes:

- Booking-service rule tests
- Owner-only cancellation test
- Provider input-validation test
- Two-thread concurrency test asserting that exactly one booking succeeds

## Concurrency design

`BookingService.book` uses a `READ_COMMITTED` transaction. The repository selects the requested availability row with `SELECT ... FOR UPDATE`. The first request holds the row lock while it checks and inserts the appointment. A second request waits, then sees the committed booking and receives HTTP 409. A PostgreSQL partial unique index on active `BOOKED` appointments is the database-level backstop.

## Main routes

| Route | Purpose |
|---|---|
| `/` | Home page |
| `/login` | Session login |
| `/slots` | Filtered and paginated available slots |
| `/customer/appointments` | Customer appointment list and cancellation |
| `/provider/dashboard` | Provider availability and booked appointments |
| `/api/home` | JSON home summary |
| `/api/slots` | JSON paginated slot data |
