-- Проверка на потребителите в базата данни
SELECT id, username, role, active, created_at FROM users;

-- Проверка дали паролата е правилно хеширана
-- Паролата трябва да започва с $2a$ за BCrypt
SELECT username, 
       LENGTH(password_hash) as hash_length,
       SUBSTRING(password_hash, 1, 4) as hash_prefix
FROM users;

