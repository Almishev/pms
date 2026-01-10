-- SQL заявка за създаване на потребители
-- Парола за двата потребителя: admin123

-- Първо проверете дали потребителите вече съществуват и ги изтрийте ако е нужно:
DELETE FROM users WHERE username IN ('admin', 'receptionist');

-- Създайте потребителите с правилния BCrypt хеш
INSERT INTO users (username, password_hash, role, active) VALUES
('admin', '$2a$10$uhyHZP6RgUIIy6aTAQZCJ.SwGxCZ.o6GM95xOgvftuOMLoV8Pwfky', 'ADMIN', true),
('receptionist', '$2a$10$uhyHZP6RgUIIy6aTAQZCJ.SwGxCZ.o6GM95xOgvftuOMLoV8Pwfky', 'RECEPTIONIST', true);

