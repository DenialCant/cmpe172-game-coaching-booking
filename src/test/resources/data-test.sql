INSERT INTO users (user_id, username, password_hash, full_name, email, role) VALUES
(1, 'player1', '$2a$10$YX04m3X1niinY63bILj8FOQwg1iN5uy0cLYyOcXMoie67HlLGMEXa', 'Player One', 'one@example.com', 'CUSTOMER'),
(2, 'player2', '$2a$10$YX04m3X1niinY63bILj8FOQwg1iN5uy0cLYyOcXMoie67HlLGMEXa', 'Player Two', 'two@example.com', 'CUSTOMER'),
(3, 'coachclark', '$2a$10$YX04m3X1niinY63bILj8FOQwg1iN5uy0cLYyOcXMoie67HlLGMEXa', 'Clark Kent', 'clark@example.com', 'PROVIDER');
INSERT INTO providers (provider_id, user_id, bio, game_specialty) VALUES
(1, 3, 'Competitive FPS coach', 'Valorant');
INSERT INTO services (service_id, provider_id, service_name, description, duration_minutes) VALUES
(1, 1, 'Valorant Aim Coaching', 'Aim training', 60);
INSERT INTO availability_slots (slot_id, provider_id, service_id, start_time, end_time, active) VALUES
(1, 1, 1, '2099-10-15 15:00:00', '2099-10-15 16:00:00', TRUE);
