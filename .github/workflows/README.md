# GitHub Actions Deployment Setup

## Как да настроите автоматично deployment

### Стъпка 1: Генериране на SSH ключ

На сървъра (Dell mini PC), генерирайте SSH ключ за GitHub Actions:

```bash
# Влезте в сървъра
ssh user@your-server-ip

# Генерирайте нов SSH ключ (ако нямате)
ssh-keygen -t ed25519 -C "github-actions-deploy" -f ~/.ssh/github_actions_deploy

# Покажете публичния ключ
cat ~/.ssh/github_actions_deploy.pub
```

**ВАЖНО:** Добавете публичния ключ в `~/.ssh/authorized_keys` на сървъра:
```bash
cat ~/.ssh/github_actions_deploy.pub >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
```

### Стъпка 2: Добавяне на Secrets в GitHub

1. Отидете в GitHub repository → **Settings** → **Secrets and variables** → **Actions**

2. Добавете следните Secrets:

   #### `SSH_PRIVATE_KEY`
   - Стойност: Съдържанието на **приватния** ключ (`~/.ssh/github_actions_deploy`)
   - Как да го получите:
     ```bash
     cat ~/.ssh/github_actions_deploy
     ```
   - Копирайте целия изход (включително `-----BEGIN OPENSSH PRIVATE KEY-----` и `-----END OPENSSH PRIVATE KEY-----`)

   #### `SERVER_HOST`
   - Стойност: IP адрес или домейн на сървъра
   - Пример: `192.168.1.100` или `hotel-pms.example.com`

   #### `SERVER_USER`
   - Стойност: Потребителско име за SSH достъп
   - Пример: `admin` или `ubuntu`

   #### `DEPLOY_PATH`
   - Стойност: Пътят до проекта на сървъра
   - Пример: `/home/admin/hotel-pos` или `/opt/hotel-pms`

### Стъпка 3: Тестване на deployment

1. Направете промяна в кода
2. Commit и push:
   ```bash
   git add .
   git commit -m "Test deployment"
   git push origin main
   ```
3. Отидете в GitHub → **Actions** tab
4. Вижте workflow изпълнението в реално време

### Стъпка 4: Ръчно стартиране (опционално)

Можете да стартирате deployment ръчно от GitHub UI:
- Отидете в **Actions** → **Deploy to Server** → **Run workflow**

---

## Безопасност

⚠️ **ВАЖНО:**
- Никога не споделяйте SSH приватния ключ публично
- Използвайте GitHub Secrets за чувствителни данни
- Ограничете SSH достъпа само до необходимите IP адреси (firewall)
- Регулярно ротирайте SSH ключовете

---

## Troubleshooting

### Проблем: "Permission denied (publickey)"
- Проверете дали SSH ключът е добавен правилно в `authorized_keys`
- Проверете правата: `chmod 600 ~/.ssh/authorized_keys`

### Проблем: "Host key verification failed"
- Workflow файлът автоматично добавя host в known_hosts
- Ако проблемът продължава, проверете `SERVER_HOST` secret

### Проблем: "Cannot connect to Docker daemon"
- Проверете дали потребителят е в `docker` групата:
  ```bash
  sudo usermod -aG docker $USER
  ```
- Или използвайте `sudo` в командите (не препоръчително)

### Проблем: Deployment работи, но промените не се виждат
- Проверете дали `DEPLOY_PATH` е правилният път
- Проверете дали `git pull` работи правилно на сървъра
- Проверете Docker logs: `docker compose logs`

---

## Алтернативен подход: SSH с парола (непрепоръчително)

Ако не можете да използвате SSH ключове, можете да използвате `appleboy/ssh-action`:

```yaml
- name: Deploy
  uses: appleboy/ssh-action@master
  with:
    host: ${{ secrets.SERVER_HOST }}
    username: ${{ secrets.SERVER_USER }}
    password: ${{ secrets.SERVER_PASSWORD }}
    script: |
      cd ${{ secrets.DEPLOY_PATH }}
      git pull
      docker compose down
      docker compose build --no-cache
      docker compose up -d
```

**ВНИМАНИЕ:** Използването на пароли е по-малко сигурно от SSH ключовете!

