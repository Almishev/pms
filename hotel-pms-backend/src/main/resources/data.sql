-- Demo Data for Hotel PMS

-- Insert default admin user (password: admin123)
INSERT INTO users (username, password_hash, role, active) VALUES
('admin', '$2a$10$uhyHZP6RgUIIy6aTAQZCJ.SwGxCZ.o6GM95xOgvftuOMLoV8Pwfky', 'ADMIN', true),
('receptionist', '$2a$10$uhyHZP6RgUIIy6aTAQZCJ.SwGxCZ.o6GM95xOgvftuOMLoV8Pwfky', 'RECEPTIONIST', true)
ON CONFLICT (username) DO NOTHING;

-- Insert room types
INSERT INTO room_type (name, capacity, base_price, active) VALUES
('Single', 1, 50.00, true),
('Double', 2, 80.00, true),
('Triple', 3, 110.00, true),
('Suite', 4, 150.00, true)
ON CONFLICT DO NOTHING;

-- Insert rooms (15 rooms total)
INSERT INTO room (room_number, room_type_id, active) VALUES
('101', 1, true), ('102', 1, true), ('103', 1, true),
('201', 2, true), ('202', 2, true), ('203', 2, true), ('204', 2, true),
('301', 2, true), ('302', 2, true), ('303', 2, true),
('401', 3, true), ('402', 3, true),
('501', 4, true), ('502', 4, true), ('503', 4, true)
ON CONFLICT (room_number) DO NOTHING;

