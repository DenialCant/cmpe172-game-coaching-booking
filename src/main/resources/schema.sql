CREATE TABLE IF NOT EXISTS users (
    user_id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('CUSTOMER', 'PROVIDER'))
);

CREATE TABLE IF NOT EXISTS providers (
    provider_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    bio TEXT,
    game_specialty VARCHAR(100),

    FOREIGN KEY (user_id)
        REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS services (
    service_id SERIAL PRIMARY KEY,
    provider_id INT NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    description TEXT,
    duration_minutes INT NOT NULL,

    FOREIGN KEY (provider_id)
        REFERENCES providers(provider_id)
);

CREATE TABLE IF NOT EXISTS availability_slots (
    slot_id SERIAL PRIMARY KEY,
    provider_id INT NOT NULL,
    service_id INT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    FOREIGN KEY (provider_id)
        REFERENCES providers(provider_id),

    FOREIGN KEY (service_id)
        REFERENCES services(service_id),

    CHECK (end_time > start_time)
);

ALTER TABLE availability_slots
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE IF NOT EXISTS appointments (
    appointment_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    slot_id INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'BOOKED'
        CHECK (status IN ('BOOKED', 'CANCELLED', 'COMPLETED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id)
        REFERENCES users(user_id),

    FOREIGN KEY (slot_id)
        REFERENCES availability_slots(slot_id)
);

-- Milestone 1 used UNIQUE(slot_id). The partial unique index keeps the
-- database-level double-booking backstop while allowing a cancelled slot
-- to be booked again later.
ALTER TABLE appointments
    DROP CONSTRAINT IF EXISTS appointments_slot_id_key;

CREATE UNIQUE INDEX IF NOT EXISTS uq_appointments_booked_slot
    ON appointments(slot_id)
    WHERE status = 'BOOKED';

CREATE INDEX IF NOT EXISTS idx_slots_filter
    ON availability_slots(provider_id, service_id, start_time);

CREATE INDEX IF NOT EXISTS idx_appointments_user
    ON appointments(user_id, created_at);
