#!/usr/bin/env bash
# Резервная копия базы (поездки, подписки, пользователи) в /opt/backups.
# Хранятся копии за последние 14 дней. Для ежедневного запуска добавьте в crontab (crontab -e):
#   30 3 * * * bash /opt/travel-planner-bot/deploy/backup.sh >> /var/log/travelbot-backup.log 2>&1
set -euo pipefail

BACKUP_DIR=/opt/backups
mkdir -p "$BACKUP_DIR"
cd /opt/travel-planner-bot

FILE="$BACKUP_DIR/travelbot-$(date +%Y-%m-%d).sql.gz"
docker compose exec -T db pg_dump -U "${DB_USER:-travelbot}" travelbot | gzip > "$FILE"
find "$BACKUP_DIR" -name 'travelbot-*.sql.gz' -mtime +14 -delete
echo "$(date '+%F %T') копия сохранена: $FILE ($(du -h "$FILE" | cut -f1))"
