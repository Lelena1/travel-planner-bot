# Перенос бота на сервер

Бот на домашнем компьютере работает, только пока компьютер включён и не спит. На сервере (VPS) он работает круглосуточно.

## 1. Сервер

Подойдёт самый простой VPS:

| Параметр | Значение |
|---|---|
| Система | Ubuntu 24.04 (или 22.04) |
| Память | 2 ГБ (сборка Java-приложения в 1 ГБ может не уместиться) |
| Процессор | 1 ядро |
| Диск | 20 ГБ |
| Цена | около 300–500 ₽ в месяц у российских хостингов |

При создании сервера добавьте **SSH-ключ** (публичный ключ, файл `.pub`) — тогда пароль для входа не нужен.

## 2. Установка

```bash
ssh root@АДРЕС_СЕРВЕРА
curl -fsSL https://raw.githubusercontent.com/Lelena1/travel-planner-bot/main/deploy/setup-server.sh -o setup-server.sh
bash setup-server.sh
```

Скрипт поставит Docker, скачает код и создаст `/opt/travel-planner-bot/.env`. Заполните его (токен бота, токен Travelpayouts, Telegram ID) и запустите:

```bash
cd /opt/travel-planner-bot && docker compose up -d --build
```

> ⚠️ Один бот — один запущенный экземпляр. Telegram отдаёт сообщения только одному процессу, поэтому перед запуском на сервере остановите бота на домашнем компьютере: `docker compose stop bot`.

## 3. Обновление

```bash
bash /opt/travel-planner-bot/deploy/update.sh
```

Скрипт заберёт последнюю версию с GitHub и пересоберёт бота. База данных при этом не трогается.

## 4. Резервные копии

```bash
crontab -e
# добавить строку — копия базы каждую ночь в 3:30, хранятся 14 дней:
30 3 * * * bash /opt/travel-planner-bot/deploy/backup.sh >> /var/log/travelbot-backup.log 2>&1
```

## Полезные команды

```bash
cd /opt/travel-planner-bot
docker compose ps              # что запущено
docker compose logs -f bot     # журнал бота
docker compose restart bot     # перезапуск
```
