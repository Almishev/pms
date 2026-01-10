#!/bin/bash
set -e

# Този скрипт се изпълнява след като PostgreSQL е стартирал
# и таблиците са създадени от Hibernate

echo "Waiting for tables to be created by Hibernate..."

# Изчакваме малко за да се създадат таблиците
sleep 10

# Проверяваме дали таблицата users съществува
until psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c '\dt' | grep -q users; do
  echo "Waiting for users table..."
  sleep 2
done

echo "Tables created. Inserting default users..."

# Вмъкване на потребители
psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" <<-EOSQL
  DELETE FROM users WHERE username IN ('admin', 'receptionist');
  
  INSERT INTO users (username, password_hash, role, active) VALUES
  ('admin', '$2a$10$uhyHZP6RgUIIy6aTAQZCJ.SwGxCZ.o6GM95xOgvftuOMLoV8Pwfky', 'ADMIN', true),
  ('receptionist', '$2a$10$uhyHZP6RgUIIy6aTAQZCJ.SwGxCZ.o6GM95xOgvftuOMLoV8Pwfky', 'RECEPTIONIST', true)
  ON CONFLICT (username) DO UPDATE 
  SET password_hash = EXCLUDED.password_hash,
      role = EXCLUDED.role,
      active = EXCLUDED.active;
EOSQL

echo "Default users created successfully!"

