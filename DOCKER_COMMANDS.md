# 🐳 Docker команди за управление на контейнерите

## ⚠️ Важно разграничение

### `docker compose` команди
- ✅ Работят **САМО** с контейнерите от `docker-compose.yml` в **текущата директория**
- ✅ **НЕ засягат** други Docker контейнери на системата
- ✅ Безопасни - засягат само това приложение
- ⚠️ **ВАЖНО:** Трябва да сте в директорията на проекта!

### Къде да изпълнявате командите?

```bash
# ✅ ПРАВИЛНО - в директорията на проекта
PS C:\Users\Admin\Desktop\lamadev\hotel-pos> docker compose up -d

# ❌ ГРЕШКА - в друга директория
PS C:\Users\Admin\Desktop> docker compose up -d
# Грешка: не може да намери docker-compose.yml
```

**Винаги изпълнявайте командите в директорията:**
```
C:\Users\Admin\Desktop\lamadev\hotel-pos
```

### `docker` команди (без compose)
- ⚠️ Работят с **ВСИЧКИ** контейнери на системата
- ⚠️ Използвайте внимателно!

**За това приложение винаги използвайте `docker compose` команди!** ✅

---

## Основни команди

### Стартиране на контейнерите

```bash
# Стартиране на всички контейнери
docker compose up -d

# Стартиране с преизграждане (ако има промени в кода)
docker compose up -d --build
```

**Описание:**
- `-d` = detached mode (в фонов режим)
- `--build` = преизгражда images преди стартиране

---

### Спиране на контейнерите

```bash
# Спиране на всички контейнери (запазва данните)
docker compose down

# Спиране и изтриване на volumes (ВНИМАНИЕ: изтрива базата данни!)
docker compose down -v
```

**Описание:**
- `down` = спира контейнерите и премахва мрежите
- `-v` = изтрива и volumes (базата данни!)

---

## Други полезни команди

### Проверка на статуса

```bash
# Вижте статуса на контейнерите
docker compose ps

# Вижте всички контейнери (включително спретите)
docker compose ps -a
```

### Логове

```bash
# Логове на всички контейнери
docker compose logs

# Логове на конкретен контейнер
docker compose logs backend
docker compose logs frontend
docker compose logs postgres

# Логове в реално време (follow)
docker compose logs -f

# Последните 50 реда
docker compose logs --tail=50
```

### Рестартиране

```bash
# Рестартиране на всички контейнери
docker compose restart

# Рестартиране на конкретен контейнер
docker compose restart backend
docker compose restart frontend
```

### Преизграждане

```bash
# Преизграждане на всички images
docker compose build

# Преизграждане без кеш (чисто преизграждане)
docker compose build --no-cache

# Преизграждане на конкретен сервис
docker compose build backend
docker compose build frontend
```

### Влизане в контейнер

```bash
# Влизане в backend контейнер
docker compose exec backend sh

# Влизане в postgres контейнер
docker compose exec postgres psql -U hotelpms_user -d hotel_pms

# Влизане в frontend контейнер
docker compose exec frontend sh
```

---

## Често използвани комбинации

### Пълно рестартиране (спиране + преизграждане + стартиране)

```bash
docker compose down
docker compose build --no-cache
docker compose up -d
```

### Проверка на всичко

```bash
# Статус
docker compose ps

# Логове
docker compose logs --tail=20

# Health check
curl http://localhost:8085/actuator/health
curl http://localhost:3000
```

### Backup на базата данни

```bash
# Създаване на backup
docker compose exec postgres pg_dump -U hotelpms_user hotel_pms > backup_$(date +%Y%m%d_%H%M%S).sql

# Възстановяване от backup
docker compose exec -T postgres psql -U hotelpms_user -d hotel_pms < backup_20240110_120000.sql
```

---

## Бърз справка

| Действие | Команда | Засяга |
|----------|---------|--------|
| **Стартиране** | `docker compose up -d` | ✅ Само това приложение |
| **Спиране** | `docker compose down` | ✅ Само това приложение |
| **Рестартиране** | `docker compose restart` | ✅ Само това приложение |
| **Статус** | `docker compose ps` | ✅ Само това приложение |
| **Логове** | `docker compose logs -f` | ✅ Само това приложение |
| **Преизграждане** | `docker compose build --no-cache` | ✅ Само това приложение |
| **Пълно рестартиране** | `docker compose down && docker compose up -d --build` | ✅ Само това приложение |

### Разлика между команди

```bash
# Вижте САМО контейнерите на това приложение
docker compose ps

# Вижте ВСИЧКИ контейнери на системата
docker ps

# Спрете САМО контейнерите на това приложение
docker compose down

# Спрете ВСИЧКИ контейнери на системата (НЕ използвайте!)
docker stop $(docker ps -q)
```

---

## ⚠️ Важни бележки

1. **`docker compose down`** НЕ изтрива базата данни (volumes се запазват)
2. **`docker compose down -v`** ИЗТРИВА базата данни! Използвайте внимателно!
3. **`docker compose up -d`** стартира контейнерите в фонов режим
4. **`--build`** е полезно когато има промени в кода

---

## 🆘 Troubleshooting

### Контейнерите не стартират

```bash
# Проверете логовете
docker compose logs

# Проверете дали портовете са заети
netstat -an | grep 8085
netstat -an | grep 3000
netstat -an | grep 5432
```

### Контейнерите се рестартират постоянно

```bash
# Проверете логовете за грешки
docker compose logs backend
docker compose logs postgres
```

### Искате да изчистите всичко и да започнете отначало

```bash
# ВНИМАНИЕ: Това изтрива ВСИЧКО включително базата данни!
docker compose down -v
docker system prune -a
docker compose up -d --build
```

