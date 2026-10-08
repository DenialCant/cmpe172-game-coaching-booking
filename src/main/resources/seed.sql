INSERT INTO users (username, password_hash, full_name, email, role)
VALUES
('player1', '$2a$10$YX04m3X1niinY63bILj8FOQwg1iN5uy0cLYyOcXMoie67HlLGMEXa', 'Daniel Khant', 'player1@example.com', 'CUSTOMER'),
('player2', '$2a$10$YX04m3X1niinY63bILj8FOQwg1iN5uy0cLYyOcXMoie67HlLGMEXa', 'Alex Player', 'player2@example.com', 'CUSTOMER'),
('coachclark', '$2a$10$YX04m3X1niinY63bILj8FOQwg1iN5uy0cLYyOcXMoie67HlLGMEXa', 'Clark Kent', 'clark@example.com', 'PROVIDER')
ON CONFLICT (username) DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
    full_name = EXCLUDED.full_name,
    email = EXCLUDED.email,
    role = EXCLUDED.role;

INSERT INTO providers (user_id, bio, game_specialty)
SELECT user_id, 'Competitive FPS coach', 'Valorant'
FROM users
WHERE username = 'coachclark'
ON CONFLICT (user_id) DO UPDATE SET
    bio = EXCLUDED.bio,
    game_specialty = EXCLUDED.game_specialty;

INSERT INTO services (provider_id, service_name, description, duration_minutes)
SELECT provider_id, 'Valorant Aim Coaching', '1-on-1 aim and mechanics coaching', 60
FROM providers
WHERE user_id = (SELECT user_id FROM users WHERE username = 'coachclark')
AND NOT EXISTS (SELECT 1 FROM services WHERE service_name = 'Valorant Aim Coaching');

INSERT INTO services (provider_id, service_name, description, duration_minutes)
SELECT provider_id, 'Valorant VOD Review', 'Review recorded gameplay and give feedback', 45
FROM providers
WHERE user_id = (SELECT user_id FROM users WHERE username = 'coachclark')
AND NOT EXISTS (SELECT 1 FROM services WHERE service_name = 'Valorant VOD Review');

INSERT INTO availability_slots (provider_id, service_id, start_time, end_time)
SELECT s.provider_id, s.service_id, '2026-10-15 15:00:00', '2026-10-15 16:00:00'
FROM services s
WHERE s.service_name = 'Valorant Aim Coaching'
AND NOT EXISTS (
    SELECT 1 FROM availability_slots WHERE start_time = '2026-10-15 15:00:00'
);

INSERT INTO availability_slots (provider_id, service_id, start_time, end_time)
SELECT s.provider_id, s.service_id, '2026-10-16 17:00:00', '2026-10-16 17:45:00'
FROM services s
WHERE s.service_name = 'Valorant VOD Review'
AND NOT EXISTS (
    SELECT 1 FROM availability_slots WHERE start_time = '2026-10-16 17:00:00'
);
