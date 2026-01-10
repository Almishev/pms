# 🚀 Бързо ръководство за GitHub Actions Deployment

## ✅ Какво е готово

- ✅ GitHub Actions workflow файл (`.github/workflows/deploy.yml`)
- ✅ Автоматично deployment при push в `main` или `master` branch
- ✅ Backup на базата данни преди всяко обновяване
- ✅ Health checks след deployment

## 📋 Стъпки за настройка (5 минути)

### 1. Генерирайте SSH ключ на сървъра

```bash
# Влезте в сървъра (Dell mini PC)
ssh user@your-server-ip

# Генерирайте SSH ключ
ssh-keygen -t ed25519 -C "github-actions-deploy" -f ~/.ssh/github_actions_deploy

# Добавете публичния ключ
cat ~/.ssh/github_actions_deploy.pub >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys

# Покажете приватния ключ (копирайте целия изход)
cat ~/.ssh/github_actions_deploy
```

### 2. Добавете Secrets в GitHub

1. Отидете в: **GitHub Repository** → **Settings** → **Secrets and variables** → **Actions**
2. Кликнете **"New repository secret"**
3. Добавете 4 secrets:

| Secret Name | Описание | Пример |
|------------|----------|--------|
| `SSH_PRIVATE_KEY` | Приватния SSH ключ (от стъпка 1) | `-----BEGIN OPENSSH PRIVATE KEY-----...` |
| `SERVER_HOST` | IP адрес на сървъра | `192.168.1.100` |
| `SERVER_USER` | SSH потребител | `admin` |
| `DEPLOY_PATH` | Път до проекта | `/home/admin/hotel-pos` |

### 3. Тествайте deployment

```bash
# Направете малка промяна
echo "# Test" >> README.md
git add .
git commit -m "Test GitHub Actions deployment"
git push origin main
```

4. Проверете в GitHub → **Actions** tab

---

## 🎯 Как работи

При всеки `git push origin main`:

1. ✅ GitHub Actions стартира автоматично
2. ✅ Свързва се със сървъра чрез SSH
3. ✅ Прави backup на базата данни
4. ✅ Изтегля новия код (`git pull`)
5. ✅ Спира Docker контейнерите
6. ✅ Преизгражда images с новия код
7. ✅ Стартира контейнерите
8. ✅ Проверява health status

---

## 🔍 Проверка на статуса

### В GitHub:
- **Actions** tab → Вижте последното изпълнение
- Зелено ✅ = Успешно
- Червено ❌ = Грешка (кликнете за детайли)

### На сървъра:
```bash
# Проверете статуса на контейнерите
docker compose ps

# Вижте логовете
docker compose logs -f
```

---

## ⚠️ Troubleshooting

### Проблем: "Permission denied (publickey)"
**Решение:** Проверете дали SSH ключът е добавен в `authorized_keys`:
```bash
cat ~/.ssh/authorized_keys | grep github-actions
```

### Проблем: "Cannot connect to Docker daemon"
**Решение:** Добавете потребителя в docker групата:
```bash
sudo usermod -aG docker $USER
# Излезте и влезте отново в SSH сесията
```

### Проблем: "DEPLOY_PATH not found"
**Решение:** Проверете дали `DEPLOY_PATH` secret е правилният път:
```bash
# На сървъра, проверете пътя
pwd  # Текуща директория
ls -la docker-compose.yml  # Проверете дали файлът съществува
```

### Проблем: Deployment работи, но промените не се виждат
**Решение:** 
1. Проверете дали `git pull` работи на сървъра
2. Проверете Docker logs: `docker compose logs backend`
3. Проверете дали frontend е rebuild-нат: `docker compose logs frontend`

---

## 🔒 Безопасност

- ✅ SSH ключовете са запазени в GitHub Secrets (криптирани)
- ✅ Само оторизирани потребители могат да виждат secrets
- ✅ Backup се прави автоматично преди всяко обновяване

**Препоръка:** Регулярно ротирайте SSH ключовете (на всеки 6 месеца).

---

## 📚 Допълнителна информация

- Пълна документация: `.github/workflows/README.md`
- Deployment ръководство: `DEPLOYMENT.md`
- Ръчно обновяване: `update.sh`

---

## ✨ Готово!

След настройката, всяко `git push` автоматично ще обновява системата на сървъра! 🎉

