# Как да добавите потребители в Docker базата данни

## ✅ Потребителите са вече създадени!

Потребителите са добавени в базата данни:
- **admin** / **admin123** (ADMIN)
- **receptionist** / **admin123** (RECEPTIONIST)

## 🔄 Автоматично създаване

Spring Boot автоматично изпълнява `data.sql` файла при стартиране, който създава потребителите.

## 📝 Ръчно добавяне (ако е нужно)

Ако потребителите не са създадени автоматично, можете да ги добавите ръчно:

### Вариант 1: Чрез Docker команда

```bash
docker compose exec postgres psql -U hotelpms_user -d hotel_pms -c "DELETE FROM users WHERE username IN ('admin', 'receptionist'); INSERT INTO users (username, password_hash, role, active) VALUES ('admin', '\$2a\$10\$uhyHZP6RgUIIy6aTAQZCJ.SwGxCZ.o6GM95xOgvftuOMLoV8Pwfky', 'ADMIN', true), ('receptionist', '\$2a\$10\$uhyHZP6RgUIIy6aTAQZCJ.SwGxCZ.o6GM95xOgvftuOMLoV8Pwfky', 'RECEPTIONIST', true) ON CONFLICT (username) DO UPDATE SET password_hash = EXCLUDED.password_hash, role = EXCLUDED.role, active = EXCLUDED.active;"
```

### Вариант 2: Чрез SQL файл

```bash
# Копиране на SQL файла в контейнера
docker cp init-users.sql hotel-pms-db:/tmp/init-users.sql

# Изпълнение на SQL файла
docker compose exec postgres psql -U hotelpms_user -d hotel_pms -f /tmp/init-users.sql
```

### Вариант 3: Интерактивно в PostgreSQL

```bash
docker compose exec postgres psql -U hotelpms_user -d hotel_pms
```

След това в PostgreSQL конзолата:

```sql
DELETE FROM users WHERE username IN ('admin', 'receptionist');

INSERT INTO users (username, password_hash, role, active) VALUES
('admin', '$2a$10$uhyHZP6RgUIIy6aTAQZCJ.SwGxCZ.o6GM95xOgvftuOMLoV8Pwfky', 'ADMIN', true),
('receptionist', '$2a$10$uhyHZP6RgUIIy6aTAQZCJ.SwGxCZ.o6GM95xOgvftuOMLoV8Pwfky', 'RECEPTIONIST', true)
ON CONFLICT (username) DO UPDATE 
SET password_hash = EXCLUDED.password_hash,
    role = EXCLUDED.role,
    active = EXCLUDED.active;
```

## ✅ Проверка

```bash
docker compose exec postgres psql -U hotelpms_user -d hotel_pms -c "SELECT username, role, active FROM users;"
```

## 🔑 Пароли

- **admin**: `admin123`
- **receptionist**: `admin123`

⚠️ **ВАЖНО**: Променете паролите преди production deployment!

