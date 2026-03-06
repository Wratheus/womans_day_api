# WomansDay API

Java 17+ / Spring Boot / Gradle / PostgreSQL / ddl-auto=update

## Структура
- `src/main/java/com/womansday/api/` — основной код
- `controller/` — REST контроллеры (Admin, Auth, Task, User, LootBox, Root)
- `service/` — бизнес-логика
- `entity/` — JPA сущности (Task, TaskSubmission, User, SubmissionMedia, RevokedToken, BalanceTransaction, LootBox)
- `repository/` — Spring Data JPA репозитории
- `enums/` — TaskType, SubmissionStatus, Role, TransactionType
- `dto/request/`, `dto/response/` — DTO
- `security/` — JWT, фильтры
- `config/` — Security, exception handlers, DataInitializer

## Баланс (тюльпаны)
- Единый источник правды: таблица `balance_transactions`
- Баланс = `SUM(amount) FROM balance_transactions WHERE user_id = ?`
- TransactionType: TASK_REWARD, LOOTBOX_PURCHASE, LOOTBOX_PRIZE
- При approve submission — создаётся транзакция для каждого участника
- `bonus_points` в User — устаревшее поле, не используется в расчёте

## Лутбоксы
- Покупка за тюльпаны (фиксированная цена в LootBoxService.LOOTBOX_COST)
- Приз — всегда тюльпаны (тиры захардкожены в LootBoxService.PRIZE_TIERS)
- Массовая раздача — через SQL напрямую
- Эндпоинты: `/api/lootbox/**`

## Ключевые моменты
- Админские эндпоинты: `/api/admin/**`
- Collaborative задания: submission может ждать участников (`WAITING_FOR_PARTICIPANTS`)
- Статусы submission: NOT_STARTED, INVITED, WAITING_FOR_PARTICIPANTS, PENDING, APPROVED, REJECTED, CANCELLED
- Медиа хранится в S3-совместимом хранилище (MinIO)
- DataInitializer: миграция данных из submissions в balance_transactions (идемпотентно)
- Postman коллекция: `postman_collection.json`
