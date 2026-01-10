# 🔧 Отстраняване на грешка: "The ssh-private-key argument is empty"

## Проблем

GitHub Actions не може да намери `SSH_PRIVATE_KEY` secret.

## Решение (стъпка по стъпка)

### Стъпка 1: Генерирайте SSH ключ на сървъра

```bash
# Влезте в сървъра
ssh user@your-server-ip

# Генерирайте ключ (ако още не сте)
ssh-keygen -t ed25519 -C "github-actions-deploy" -f ~/.ssh/github_actions_deploy

# При въпрос за passphrase, натиснете Enter (без парола)

# Добавете публичния ключ
cat ~/.ssh/github_actions_deploy.pub >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
```

### Стъпка 2: Копирайте ПРИВАТНИЯ ключ

```bash
# На сървъра, покажете приватния ключ
cat ~/.ssh/github_actions_deploy
```

**ВАЖНО:** Копирайте ЦЕЛИЯ изход, включително:
```
-----BEGIN OPENSSH PRIVATE KEY-----
b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAAlwAAAAdzc2gtcn
NhAAAAAwEAAQAAAIEAy...
(много редове тук)
...
-----END OPENSSH PRIVATE KEY-----
```

### Стъпка 3: Добавете Secret в GitHub

1. Отидете в GitHub repository
2. Кликнете **Settings** (в горното меню)
3. В лявото меню: **Secrets and variables** → **Actions**
4. Кликнете **"New repository secret"**
5. Попълнете:
   - **Name:** `SSH_PRIVATE_KEY` (точно това име, без интервали!)
   - **Secret:** Вмъкнете целия SSH ключ (от стъпка 2)
6. Кликнете **"Add secret"**

### Стъпка 4: Проверете другите Secrets

Уверете се, че имате всички 4 secrets:

1. ✅ `SSH_PRIVATE_KEY` - Приватния SSH ключ
2. ✅ `SERVER_HOST` - IP адрес (напр. `192.168.1.100`)
3. ✅ `SERVER_USER` - SSH потребител (напр. `admin`)
4. ✅ `DEPLOY_PATH` - Път до проекта (напр. `/home/admin/hotel-pos`)

### Стъпка 5: Рестартирайте workflow

1. Отидете в **Actions** tab в GitHub
2. Намерете последния failed run
3. Кликнете върху него
4. Кликнете **"Re-run all jobs"** (бутон в горния десен ъгъл)

---

## ⚠️ Често срещани грешки

### Грешка 1: Името на secret-а е различно

**НЕПРАВИЛНО:**
- `ssh_private_key` (малки букви)
- `SSH_PRIVATE_KEY ` (интервал в края)
- `SSH-PRIVATE-KEY` (тирета)

**ПРАВИЛНО:**
- `SSH_PRIVATE_KEY` (точно това име!)

### Грешка 2: SSH ключът е непълен

**НЕПРАВИЛНО:**
```
b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQ...
(липсват BEGIN и END редовете)
```

**ПРАВИЛНО:**
```
-----BEGIN OPENSSH PRIVATE KEY-----
b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQ...
(всички редове)
...
-----END OPENSSH PRIVATE KEY-----
```

### Грешка 3: Има допълнителни интервали

**НЕПРАВИЛНО:**
```
    -----BEGIN OPENSSH PRIVATE KEY-----
(интервали преди BEGIN)
```

**ПРАВИЛНО:**
```
-----BEGIN OPENSSH PRIVATE KEY-----
(без интервали преди BEGIN)
```

---

## ✅ Проверка

След като добавите secret-а правилно:

1. Отидете в **Actions** tab
2. Кликнете на последния workflow run
3. Разгънете **"Setup SSH"** стъпката
4. Трябва да видите: ✅ "SSH agent started"

Ако виждате грешка, проверете отново стъпките по-горе.

---

## 🧪 Тест на SSH ключа

Преди да добавите в GitHub, можете да тествате ключа локално:

```bash
# На локалната машина
# Създайте временен файл с ключа
nano /tmp/test_key
# Вмъкнете SSH ключа, запазете (Ctrl+X, Y, Enter)

# Направете го изпълним
chmod 600 /tmp/test_key

# Тествайте свързването
ssh -i /tmp/test_key user@your-server-ip "echo 'SSH works!'"
```

Ако видите "SSH works!", ключът е правилен.

---

## 📞 Нуждаете се от помощ?

Ако проблемът продължава:
1. Проверете `.github/workflows/TROUBLESHOOTING.md` за повече детайли
2. Проверете логовете в GitHub Actions за конкретна грешка
3. Уверете се, че всички 4 secrets са добавени правилно

