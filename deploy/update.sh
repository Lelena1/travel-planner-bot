#!/usr/bin/env bash
# Обновление бота на сервере до последней версии с GitHub.
# Запускать на сервере:  bash /opt/travel-planner-bot/deploy/update.sh
set -euo pipefail

cd /opt/travel-planner-bot
echo "==> Забираю изменения с GitHub"
git pull --ff-only
echo "==> Пересобираю и перезапускаю бота (база данных не трогается)"
docker compose up -d --build bot
echo "==> Удаляю старые образы, чтобы не занимали диск"
docker image prune -f >/dev/null
docker compose ps
