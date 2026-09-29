#!/bin/bash
# Бекъп на базата върху първия закачен USB диск.
# Ако часът 23:35 е бил пропуснат, скриптът записва бекъпа при следващото пускане.

set -euo pipefail

STATE_DIR="${HOME}/.hotel-pms"
STATE_FILE="$STATE_DIR/last-backup.txt"
DB_NAME="${DB_NAME:-hotel_pms}"
DB_USER="${DB_USER:-user1}"
DB_PASSWORD="${DB_PASSWORD:-asroma}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"

slot_epoch() {
  local today yesterday
  today=$(date -d "today 23:35" +%s)
  local now
  now=$(date +%s)
  if [ "$now" -lt "$today" ]; then
    date -d "yesterday 23:35" +%s
  else
    echo "$today"
  fi
}

mkdir -p "$STATE_DIR"
exec 9>"$STATE_DIR/backup.lock"
if ! flock -n 9; then
  echo "Бекъпът вече тече."
  exit 0
fi

if [ -f "$STATE_FILE" ]; then
  last=$(date -d "$(tr -d '\r' < "$STATE_FILE")" +%s 2>/dev/null || echo 0)
  due=$(slot_epoch)
  if [ "$last" -ge "$due" ]; then
    echo "Бекъпът за този период вече е направен."
    exit 0
  fi
fi

usb=""
while read -r target source; do
  [ -n "$source" ] || continue
  case "$source" in
    /dev/*) ;;
    *) continue ;;
  esac
  case "$target" in
    /|/boot*) continue ;;
  esac
  dev=$(basename "$source")
  if [[ $dev =~ ^(nvme[0-9]+n[0-9]+)p[0-9]+$ ]]; then
    block="${BASH_REMATCH[1]}"
  elif [[ $dev =~ ^(mmcblk[0-9]+)p[0-9]+$ ]]; then
    block="${BASH_REMATCH[1]}"
  else
    block=$(echo "$dev" | sed -E 's/[0-9]+$//')
  fi
  flag="/sys/block/$block/removable"
  if [ -f "$flag" ] && [ "$(tr -d '[:space:]' < "$flag")" = "1" ] && [ -w "$target" ]; then
    usb="$target"
    break
  fi
done < <(findmnt -nro TARGET,SOURCE)

if [ -z "$usb" ]; then
  echo "Няма закачен външен диск." >&2
  exit 1
fi

folder="$usb/hotel-pms-backups"
mkdir -p "$folder"
file="$folder/hotel-pms-$(date +%Y-%m-%d_%H%M).sql"

if docker ps --format '{{.Names}}' 2>/dev/null | grep -qx 'hotel-pms-db'; then
  docker exec -e PGPASSWORD="${DOCKER_DB_PASSWORD:-hotelpms_password}" hotel-pms-db \
    pg_dump -U "${DOCKER_DB_USER:-hotelpms_user}" "$DB_NAME" --no-owner --no-acl > "$file"
else
  PGPASSWORD="$DB_PASSWORD" PGCLIENTENCODING=UTF8 \
    pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" --no-owner --no-acl > "$file"
fi

if [ ! -s "$file" ]; then
  rm -f "$file"
  echo "Бекъпът е празен." >&2
  exit 1
fi

mkdir -p "$STATE_DIR"
date +%Y-%m-%dT%H:%M:%S > "$STATE_FILE"
echo "Бекъпът е записан: $file"
