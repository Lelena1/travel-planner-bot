#!/usr/bin/env bash
# Первая установка бота на чистый сервер с Ubuntu 22.04/24.04.
# Запускать от root:  bash setup-server.sh
#
# Что делает: ставит Docker, скачивает код с GitHub в /opt/travel-planner-bot,
# создаёт .env из шаблона (если его ещё нет) и запускает бота с базой.
set -euo pipefail

APP_DIR=/opt/travel-planner-bot
REPO=https://github.com/Lelena1/travel-planner-bot.git

echo "==> Обновляю список пакетов и ставлю git"
apt-get update -q
apt-get install -y -q git ca-certificates curl

if ! command -v docker >/dev/null 2>&1; then
	echo "==> Ставлю Docker (официальный установочный скрипт docs.docker.com)"
	curl -fsSL https://get.docker.com | sh
fi
systemctl enable --now docker

if [ ! -d "$APP_DIR/.git" ]; then
	echo "==> Скачиваю код бота"
	git clone "$REPO" "$APP_DIR"
fi
cd "$APP_DIR"

if [ ! -f .env ]; then
	cp .env.example .env
	chmod 600 .env
	echo
	echo "!!! Создан $APP_DIR/.env — заполните токены и ID, затем запустите:"
	echo "    cd $APP_DIR && docker compose up -d --build"
	exit 0
fi

echo "==> Собираю и запускаю бота (первая сборка — несколько минут)"
docker compose up -d --build
docker compose ps
echo "==> Готово. Журнал бота: cd $APP_DIR && docker compose logs -f bot"
