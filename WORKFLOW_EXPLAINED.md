# 🔄 Как работи автоматичното обновяване

## Вашият workflow

### 1. Работа локално (Windows компютър) 💻

```bash
# Правите промени в кода
# Тествате локално
# Commit и push в GitHub
git add .
git commit -m "Описание на промените"
git push origin main
```

### 2. GitHub Actions автоматично обновява сървъра 🚀

След `git push`, GitHub Actions:
- ✅ Свързва се със сървъра (Dell mini PC) чрез SSH
- ✅ Прави backup на базата данни
- ✅ Изтегля новия код (`git pull`)
- ✅ Спира Docker контейнерите
- ✅ Преизгражда Docker images с новия код
- ✅ Стартира контейнерите отново
- ✅ Проверява дали всичко работи

**Вие не трябва да правите НИЩО на сървъра!** Всичко е автоматично! 🎉

---

## 📋 Какво трябва да е готово

### На сървъра (Dell mini PC):
- ✅ Docker и Docker Compose са инсталирани
- ✅ Проектът е клониран от GitHub
- ✅ Публичният SSH ключ е добавен в `~/.ssh/authorized_keys`
- ✅ Потребителят има права за Docker (`sudo usermod -aG docker $USER`)

### В GitHub:
- ✅ `SSH_PRIVATE_KEY` secret е добавен
- ✅ `SERVER_HOST` secret е добавен (IP адрес на сървъра)
- ✅ `SERVER_USER` secret е добавен (SSH потребител)
- ✅ `DEPLOY_PATH` secret е добавен (път до проекта)

---

## 🎯 Примерен workflow

### Ден 1: Настройка (еднократно)

1. **На сървъра:**
   ```bash
   # Клонирайте проекта
   git clone https://github.com/your-username/hotel-pos.git
   cd hotel-pos
   
   # Добавете публичния SSH ключ
   echo "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIMw922N6DROacV938go49goMhZXHBzVEwaz+fHJQfe9I github-actions-deploy" >> ~/.ssh/authorized_keys
   
   # Стартирайте Docker контейнерите
   docker compose up -d
   ```

2. **В GitHub:**
   - Добавете 4-те secrets (вижте `WINDOWS_SSH_SETUP.md`)

### Ден 2 и нататък: Работа

1. **На Windows компютъра:**
   ```bash
   # Правите промени в кода
   # Тествате локално (ако искате)
   
   # Push в GitHub
   git add .
   git commit -m "Добавена нова функционалност"
   git push origin main
   ```

2. **GitHub Actions автоматично:**
   - Обновява сървъра
   - Преизгражда Docker
   - Рестартира контейнерите

3. **Проверка:**
   - Отидете в GitHub → **Actions** tab
   - Вижте дали deployment е успешен ✅
   - Отворете `http://your-server-ip:3000` - новите промени са там!

---

## ✅ Проверка дали всичко работи

### Тест 1: Проверете GitHub Secrets

1. GitHub → **Settings** → **Secrets and variables** → **Actions**
2. Трябва да виждате 4 secrets:
   - `SSH_PRIVATE_KEY`
   - `SERVER_HOST`
   - `SERVER_USER`
   - `DEPLOY_PATH`

### Тест 2: Направете тестов push

```bash
# Направете малка промяна
echo "# Test" >> README.md
git add README.md
git commit -m "Test GitHub Actions"
git push origin main
```

### Тест 3: Проверете GitHub Actions

1. GitHub → **Actions** tab
2. Трябва да видите нов workflow run
3. Кликнете върху него
4. Разгънете стъпките и вижте логовете

**Ако виждате ✅ зелено** - всичко работи!

---

## 🔍 Какво се случва при push

```
Ваш Windows компютър
    │
    │ git push origin main
    │
    ▼
GitHub Repository
    │
    │ GitHub Actions се активира
    │
    ▼
GitHub Actions Runner (Ubuntu)
    │
    │ SSH свързване със сървъра
    │
    ▼
Dell mini PC (Сървър)
    │
    │ git pull
    │ docker compose down
    │ docker compose build --no-cache
    │ docker compose up -d
    │
    ▼
✅ Новите промени са на сървъра!
```

---

## ⚠️ Важни бележки

1. **Не трябва да правите НИЩО на сървъра** след първоначалната настройка
2. **Всички промени** се правят локално и се push-ват в GitHub
3. **GitHub Actions** автоматично обновява сървъра
4. **Проверявайте Actions tab** в GitHub за статус на deployment

---

## 🆘 Ако нещо не работи

1. **Проверете GitHub Actions логовете:**
   - GitHub → Actions → Последният run → Разгънете стъпките

2. **Проверете Secrets:**
   - Всички 4 secrets трябва да са добавени

3. **Проверете SSH свързването:**
   - На сървъра: `cat ~/.ssh/authorized_keys | grep github-actions`

4. **Проверете Docker на сървъра:**
   - `docker compose ps` - трябва да показва работещи контейнери

---

## 🎉 Готово!

След настройката, просто:
1. Работите локално
2. `git push`
3. GitHub Actions автоматично обновява сървъра!

**Никакви ръчни операции на сървъра не са нужни!** 🚀

