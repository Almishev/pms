#!/bin/bash

# Скрипт за автоматична настройка на systemd service
# Използване: sudo ./setup-auto-start.sh

set -e

PROJECT_DIR="/opt/hotel-pos"
SERVICE_FILE="hotel-pms.service"
SYSTEMD_DIR="/etc/systemd/system"

echo "🚀 Настройка на автоматично стартиране на Hotel PMS..."

# Проверка дали проектът съществува
if [ ! -d "$PROJECT_DIR" ]; then
    echo "❌ Грешка: Проектът не е намерен в $PROJECT_DIR"
    echo "   Моля, променете PROJECT_DIR в скрипта или клонирайте проекта там."
    exit 1
fi

# Проверка дали service файлът съществува
if [ ! -f "$PROJECT_DIR/$SERVICE_FILE" ]; then
    echo "❌ Грешка: Service файлът не е намерен: $PROJECT_DIR/$SERVICE_FILE"
    exit 1
fi

# Проверка за root права
if [ "$EUID" -ne 0 ]; then 
    echo "❌ Грешка: Трябват root права. Използвайте: sudo ./setup-auto-start.sh"
    exit 1
fi

# Копиране на service файла
echo "📋 Копиране на systemd service файл..."
cp "$PROJECT_DIR/$SERVICE_FILE" "$SYSTEMD_DIR/$SERVICE_FILE"

# Проверка дали пътят в service файла е правилен
if ! grep -q "WorkingDirectory=$PROJECT_DIR" "$SYSTEMD_DIR/$SERVICE_FILE"; then
    echo "⚠️  Внимание: Пътят в service файла може да не съвпада с $PROJECT_DIR"
    echo "   Моля, проверете и променете ако е нужно."
fi

# Релоад на systemd
echo "🔄 Релоад на systemd daemon..."
systemctl daemon-reload

# Активиране на автоматично стартиране
echo "✅ Активиране на автоматично стартиране..."
systemctl enable hotel-pms

# Стартиране на service
echo "🚀 Стартиране на service..."
systemctl start hotel-pms

# Проверка на статуса
echo ""
echo "📊 Статус на service:"
systemctl status hotel-pms --no-pager

echo ""
echo "✅ Готово! Service е настроен и стартиран."
echo ""
echo "Полезни команди:"
echo "  - Проверка на статус: sudo systemctl status hotel-pms"
echo "  - Преглед на логове: sudo journalctl -u hotel-pms -f"
echo "  - Рестартиране: sudo systemctl restart hotel-pms"
echo ""
echo "⚠️  За да тествате автоматичното стартиране, рестартирайте компютъра:"
echo "  sudo reboot"

