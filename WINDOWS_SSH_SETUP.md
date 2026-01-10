# 🔑 Настройка на SSH ключ на Windows за GitHub Actions

## ✅ SSH ключът е генериран!

Вашият SSH ключ е създаден в:
- **Приватен ключ:** `C:\Users\Admin\.ssh\github_actions_deploy`
- **Публичен ключ:** `C:\Users\Admin\.ssh\github_actions_deploy.pub`

---

## 📋 Следващи стъпки

### Стъпка 1: Копирайте ПРИВАТНИЯ ключ за GitHub

**Приватният ключ (от изхода по-горе):**
```
-----BEGIN OPENSSH PRIVATE KEY-----
b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAAMwAAAAtzc2gtZW
QyNTUxOQAAACDMPdtjeg0TmnFfd/IKOPYKDIWVxwc1RMGs/nxyUH3vSAAAAJjbQqzB20Ks
wQAAAAtzc2gtZWQyNTUxOQAAACDMPdtjeg0TmnFfd/IKOPYKDIWVxwc1RMGs/nxyUH3vSA
AAAEAEbZzvG1Ucvn38Rroxg8J9XaVH8cfQXd8MQbDIK+i8qMw922N6DROacV938go49goM
hZXHBzVEwaz+fHJQfe9IAAAAFWdpdGh1Yi1hY3Rpb25zLWRlcGxveQ==
-----END OPENSSH PRIVATE KEY-----
```

**Добавете го в GitHub:**
1. Отидете в GitHub repository
2. **Settings** → **Secrets and variables** → **Actions**
3. Кликнете **"New repository secret"**
4. **Name:** `SSH_PRIVATE_KEY`
5. **Secret:** Вмъкнете целия ключ по-горе (включително BEGIN и END редовете)
6. Кликнете **"Add secret"**

---

### Стъпка 2: Добавете ПУБЛИЧНИЯ ключ на сървъра

**Публичният ключ:**
```
ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIMw922N6DROacV938go49goMhZXHBzVEwaz+fHJQfe9I github-actions-deploy
```

**На сървъра (Dell mini PC), изпълнете:**

```bash
# Влезте в сървъра
ssh user@your-server-ip

# Добавете публичния ключ
echo "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIMw922N6DROacV938go49goMhZXHBzVEwaz+fHJQfe9I github-actions-deploy" >> ~/.ssh/authorized_keys

# Настройте правилните права
chmod 600 ~/.ssh/authorized_keys
chmod 700 ~/.ssh
```

**Или ако искате да копирате целия файл:**

На Windows, изпълнете:
```powershell
# Покажете публичния ключ
Get-Content "$env:USERPROFILE\.ssh\github_actions_deploy.pub"
```

След това копирайте изхода и го добавете на сървъра както е показано по-горе.

---

### Стъпка 3: Проверете другите Secrets в GitHub

Уверете се, че имате всички 4 secrets:

1. ✅ `SSH_PRIVATE_KEY` - Приватния ключ (от стъпка 1)
2. ✅ `SERVER_HOST` - IP адрес на сървъра (напр. `192.168.1.100`)
3. ✅ `SERVER_USER` - SSH потребител (напр. `admin`)
4. ✅ `DEPLOY_PATH` - Път до проекта (напр. `/home/admin/hotel-pos`)

---

### Стъпка 4: Тествайте свързването

**На Windows, тествайте дали ключът работи:**

```powershell
# Тествайте SSH свързването
ssh -i "$env:USERPROFILE\.ssh\github_actions_deploy" user@your-server-ip "echo 'SSH connection successful!'"
```

Ако видите "SSH connection successful!", ключът работи правилно!

---

## 🔍 Проверка на файловете

**За да видите ключовете отново:**

```powershell
# Приватен ключ
Get-Content "$env:USERPROFILE\.ssh\github_actions_deploy"

# Публичен ключ
Get-Content "$env:USERPROFILE\.ssh\github_actions_deploy.pub"
```

---

## ⚠️ Важни бележки

1. **Приватният ключ** е СЕКРЕТЕН - не го споделяйте никога!
2. **Публичният ключ** може да се споделя безопасно
3. Приватният ключ отива в **GitHub Secrets**
4. Публичният ключ отива на **сървъра** в `~/.ssh/authorized_keys`

---

## ✅ След като направите всичко

1. Рестартирайте workflow в GitHub Actions
2. Проверете дали deployment работи
3. Ако има грешки, проверете логовете в GitHub Actions

---

## 🆘 Ако има проблеми

Ако SSH свързването не работи:

1. Проверете дали публичният ключ е добавен правилно на сървъра:
   ```bash
   # На сървъра
   cat ~/.ssh/authorized_keys | grep github-actions
   ```

2. Проверете правата на файловете:
   ```bash
   # На сървъра
   ls -la ~/.ssh/
   chmod 600 ~/.ssh/authorized_keys
   chmod 700 ~/.ssh
   ```

3. Проверете SSH конфигурацията на сървъра:
   ```bash
   # На сървъра
   sudo nano /etc/ssh/sshd_config
   # Уверете се, че има:
   # PubkeyAuthentication yes
   # AuthorizedKeysFile .ssh/authorized_keys
   ```

