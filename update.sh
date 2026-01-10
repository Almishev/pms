#!/bin/bash

# Скрипт за обновяване на Hotel PMS след git pull
# Използване: ./update.sh

set -e  # Спира при грешка

echo "=========================================="
echo "Обновяване на Hotel PMS система"
echo "=========================================="

# Проверка дали сме в правилната директория
if [ ! -f "docker-compose.yml" ]; then
    echo "ГРЕШКА: docker-compose.yml не е намерен!"
    echo "Моля, стартирайте скрипта от root директорията на проекта."
    exit 1
fi

# Backup на базата данни (опционално)
echo ""
echo "Създаване на backup на базата данни..."
BACKUP_FILE="backup_$(date +%Y%m%d_%H%M%S).sql"
docker compose exec -T postgres pg_dump -U hotelpms_user hotel_pms > "$BACKUP_FILE" 2>/dev/null || echo "Backup пропуснат (контейнерът може да не работи)"
echo "Backup създаден: $BACKUP_FILE"

# Pull новия код от GitHub
echo ""
echo "Изтегляне на новия код от GitHub..."
git pull origin main || git pull origin master || {
    echo "ГРЕШКА: Неуспешно изтегляне от GitHub!"
    exit 1
}

# Спиране на контейнерите
echo ""
echo "Спиране на контейнерите..."
docker compose down

# Преизграждане на images
echo ""
echo "Преизграждане на Docker images..."
docker compose build --no-cache

# Стартиране на контейнерите
echo ""
echo "Стартиране на контейнерите..."
docker compose up -d

# Изчакване за стартиране
echo ""
echo "Изчакване за стартиране на услугите..."
sleep 10

# Проверка на статуса
echo ""
echo "=========================================="
echo "Статус на контейнерите:"
echo "=========================================="
docker compose ps

# Проверка на логовете
echo ""
echo "=========================================="
echo "Последни логове от backend:"
echo "=========================================="
docker compose logs backend --tail=20

echo ""
echo "=========================================="
echo "Обновяването е завършено!"
echo "=========================================="
echo ""
echo "Проверете статуса с: docker compose ps"
echo "Вижте логовете с: docker compose logs -f"
echo ""

