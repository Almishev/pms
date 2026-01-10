# Отстраняване на проблеми с GitHub Actions

## Грешка: "The ssh-private-key argument is empty"

### Причина
GitHub не може да намери или прочете `SSH_PRIVATE_KEY` secret.

### Решение

#### Стъпка 1: Проверете дали secret-ът е добавен

1. Отидете в GitHub repository
2. **Settings** → **Secrets and variables** → **Actions**
3. Проверете дали съществува secret с име точно `SSH_PRIVATE_KEY` (case-sensitive!)

#### Стъпка 2: Проверете съдържанието на SSH ключа

На сървъра, изпълнете:

```bash
cat ~/.ssh/github_actions_deploy
```

**ВАЖНО:** Копирайте целия изход, включително:
- `-----BEGIN OPENSSH PRIVATE KEY-----`
- Всички редове между
- `-----END OPENSSH PRIVATE KEY-----`

#### Стъпка 3: Добавете secret правилно

1. В GitHub → **Settings** → **Secrets and variables** → **Actions**
2. Кликнете **"New repository secret"**
3. **Name:** `SSH_PRIVATE_KEY` (точно това име, без интервали!)
4. **Secret:** Вмъкнете целия SSH ключ (включително BEGIN и END редовете)
5. Кликнете **"Add secret"**

#### Стъпка 4: Проверете форматирането

SSH ключът трябва да изглежда така:

```
-----BEGIN OPENSSH PRIVATE KEY-----
b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAAlwAAAAdzc2gtcn
NhAAAAAwEAAQAAAIEAy... (много редове)
...
-----END OPENSSH PRIVATE KEY-----
```

**НЕ трябва да има:**
- Допълнителни интервали в началото/края
- Празни редове преди `-----BEGIN`
- Празни редове след `-----END`

#### Стъпка 5: Рестартирайте workflow

1. Отидете в **Actions** tab
2. Намерете последния failed run
3. Кликнете **"Re-run all jobs"**

---

## Алтернативно решение: Използване на appleboy/ssh-action

Ако проблемът продължава, можете да използвате алтернативен action:

```yaml
- name: Deploy to server
  uses: appleboy/ssh-action@master
  with:
    host: ${{ secrets.SERVER_HOST }}
    username: ${{ secrets.SERVER_USER }}
    key: ${{ secrets.SSH_PRIVATE_KEY }}
    script: |
      cd ${{ secrets.DEPLOY_PATH }}
      git pull origin main
      docker compose down
      docker compose build --no-cache
      docker compose up -d
```

---

## Проверка на всички secrets

Уверете се, че имате всички 4 secrets:

1. ✅ `SSH_PRIVATE_KEY` - Приватния SSH ключ
2. ✅ `SERVER_HOST` - IP адрес на сървъра
3. ✅ `SERVER_USER` - SSH потребител
4. ✅ `DEPLOY_PATH` - Път до проекта

---

## Тестване на SSH ключа локално

Преди да добавите в GitHub, тествайте SSH ключа:

```bash
# На локалната машина, копирайте приватния ключ
# Създайте временен файл
cat > /tmp/test_key << 'EOF'
-----BEGIN OPENSSH PRIVATE KEY-----
[вашият ключ тук]
-----END OPENSSH PRIVATE KEY-----
EOF

# Тествайте свързването
chmod 600 /tmp/test_key
ssh -i /tmp/test_key user@your-server-ip "echo 'SSH connection successful!'"
```

Ако това работи, ключът е правилен.

---

## Често срещани грешки

### Грешка 1: "No such file or directory"
**Причина:** `DEPLOY_PATH` е неправилен
**Решение:** Проверете пътя на сървъра: `pwd` в директорията на проекта

### Грешка 2: "Permission denied"
**Причина:** SSH ключът не е в `authorized_keys`
**Решение:** 
```bash
cat ~/.ssh/github_actions_deploy.pub >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
```

### Грешка 3: "Cannot connect to Docker daemon"
**Причина:** Потребителят не е в docker групата
**Решение:**
```bash
sudo usermod -aG docker $USER
# Излезте и влезте отново в SSH
```

