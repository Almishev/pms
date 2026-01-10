# Ръководство за Deployment и Обновяване

## Текуща ситуация

✅ **GitHub Actions е настроен за автоматично deployment!**

При всеки push в `main` или `master` branch, системата автоматично:
- Изтегля новия код
- Прави backup на базата данни
- Преизгражда Docker images
- Рестартира контейнерите

**Ако все още не сте настроили GitHub Secrets**, вижте секцията "Настройка на GitHub Actions" по-долу.

---

## Как да обновите след качване в GitHub

### Стъпка 1: Влезте в сървъра (Dell mini PC)

```bash
ssh user@your-server-ip
cd /path/to/hotel-pos
```

### Стъпка 2: Изтеглете новите промени от GitHub

```bash
git pull origin main
# или
git pull origin master
```

### Стъпка 3: Спрете контейнерите

```bash
docker compose down
```

### Стъпка 4: Преизградете и рестартирайте контейнерите

```bash
# Преизграждане на images с новия код
docker compose build --no-cache

# Стартиране на контейнерите
docker compose up -d
```

### Стъпка 5: Проверка на статуса

```bash
docker compose ps
docker compose logs backend --tail=50
docker compose logs frontend --tail=50
```

---

## Бърз скрипт за обновяване

Създайте файл `update.sh` на сървъра:

```bash
#!/bin/bash
echo "Обновяване на Hotel PMS..."

# Pull новия код
git pull origin main

# Спиране на контейнерите
docker compose down

# Преизграждане
docker compose build --no-cache

# Стартиране
docker compose up -d

echo "Обновяването е завършено!"
docker compose ps
```

Направете го изпълним:
```bash
chmod +x update.sh
```

Използвайте го така:
```bash
./update.sh
```

---

## Настройка на GitHub Actions (Автоматично Deployment)

✅ **GitHub Actions workflow файлът вече е създаден** в `.github/workflows/deploy.yml`

### Стъпка 1: Генериране на SSH ключ на сървъра

```bash
# На сървъра (Dell mini PC)
ssh-keygen -t ed25519 -C "github-actions-deploy" -f ~/.ssh/github_actions_deploy

# Добавете публичния ключ в authorized_keys
cat ~/.ssh/github_actions_deploy.pub >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys

# Покажете приватния ключ (ще го копирате в GitHub)
cat ~/.ssh/github_actions_deploy
```

### Стъпка 2: Добавяне на Secrets в GitHub

1. Отидете в GitHub repository → **Settings** → **Secrets and variables** → **Actions**
2. Добавете следните Secrets:

   - **`SSH_PRIVATE_KEY`**: Съдържанието на `~/.ssh/github_actions_deploy` (приватния ключ)
   - **`SERVER_HOST`**: IP адрес или домейн на сървъра (напр. `192.168.1.100`)
   - **`SERVER_USER`**: SSH потребител (напр. `admin` или `ubuntu`)
   - **`DEPLOY_PATH`**: Пътят до проекта (напр. `/home/admin/hotel-pos`)

### Стъпка 3: Тестване

```bash
# Направете промяна и push
git add .
git commit -m "Test GitHub Actions deployment"
git push origin main
```

След това проверете в GitHub → **Actions** tab за изпълнението.

**Пълни инструкции:** Вижте `.github/workflows/README.md`

### Опция 2: Watchtower (Автоматично обновяване на Docker images)

Watchtower автоматично проверява за нови images и ги обновява.

**Добавете в `docker-compose.yml`:**

```yaml
  watchtower:
    image: containrrr/watchtower
    container_name: watchtower
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
    environment:
      - WATCHTOWER_CLEANUP=true
      - WATCHTOWER_POLL_INTERVAL=3600  # Проверява на всеки час
    restart: unless-stopped
```

**ВАЖНО:** Това работи само ако images се публикуват в Docker Hub или друг registry. За локални builds не работи.

### Опция 3: Cron Job на сървъра

Създайте cron job, който периодично проверява за промени:

```bash
# Редактирайте crontab
crontab -e

# Добавете (проверява на всеки час)
0 * * * * cd /path/to/hotel-pos && git pull origin main && docker compose up -d --build
```

---

## Препоръчителен workflow за production

### 1. Development → GitHub
```bash
git add .
git commit -m "Описание на промените"
git push origin main
```

### 2. На сървъра (ръчно или автоматично)
```bash
cd /path/to/hotel-pos
./update.sh  # или използвайте GitHub Actions
```

### 3. Проверка
```bash
# Проверете дали всичко работи
docker compose ps
curl http://localhost:8085/actuator/health
curl http://localhost:3000
```

---

## Важни бележки

1. **Backup преди обновяване**: Винаги правете backup на базата данни преди обновяване:
   ```bash
   docker compose exec postgres pg_dump -U hotelpms_user hotel_pms > backup_$(date +%Y%m%d_%H%M%S).sql
   ```

2. **Database миграции**: Spring Boot автоматично прилага миграции при стартиране (ако използвате `spring.jpa.hibernate.ddl-auto=update`).

3. **Zero-downtime deployment**: За production препоръчително е да използвате:
   - Load balancer
   - Blue-green deployment
   - Rolling updates

4. **Environment variables**: Уверете се, че `.env` файлът (ако има такъв) е актуален на сървъра.

---

## Текуща конфигурация

Вашата текуща Docker setup:
- ✅ Локални builds (от source code)
- ✅ Multi-stage builds (оптимизирани images)
- ✅ Автоматично стартиране при boot (systemd)
- ❌ НЕ автоматично обновяване от GitHub

---

## Следващи стъпки

1. **За development**: Използвайте ръчно обновяване с `update.sh` скрипта
2. **За production**: Настройте GitHub Actions за автоматично deployment
3. **За тестване**: Винаги тествайте промените локално преди push в GitHub
