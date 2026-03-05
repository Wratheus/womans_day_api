#!/bin/bash
set -euo pipefail

# Директория бэкапов (рядом с docker-compose.yml)
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
BACKUP_DIR="${PROJECT_DIR}/backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

mkdir -p "$BACKUP_DIR"

# Загружаем переменные из .env (если есть)
if [ -f "${PROJECT_DIR}/.env" ]; then
    set -a
    . "${PROJECT_DIR}/.env"
    set +a
fi

DB_USER="${DB_USER:?DB_USER not set}"
DB_NAME="${DB_NAME:?DB_NAME not set}"

BACKUP_FILE="${BACKUP_DIR}/db_${TIMESTAMP}.sql.gz"

# Дамп PostgreSQL из контейнера
docker compose -f "${PROJECT_DIR}/docker-compose.yml" exec -T db \
    pg_dump -U "$DB_USER" "$DB_NAME" \
    | gzip > "$BACKUP_FILE"

# Проверяем что файл не пустой
if [ ! -s "$BACKUP_FILE" ]; then
    echo "ERROR: Backup file is empty, something went wrong"
    rm -f "$BACKUP_FILE"
    exit 1
fi

SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
echo "OK: ${BACKUP_FILE} (${SIZE})"

# Удалить бэкапы старше 14 дней
find "$BACKUP_DIR" -name "db_*.sql.gz" -mtime +14 -delete
