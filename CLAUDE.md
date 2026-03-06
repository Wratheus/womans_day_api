# WomansDay API

Java 17+ / Spring Boot / Gradle

## Структура
- `src/main/java/com/womansday/api/` — основной код
- `controller/` — REST контроллеры (Admin, Auth, Task, User, Root)
- `service/` — бизнес-логика
- `entity/` — JPA сущности (Task, TaskSubmission, User, SubmissionMedia, RevokedToken)
- `repository/` — Spring Data JPA репозитории
- `enums/` — TaskType, SubmissionStatus, Role
- `dto/request/`, `dto/response/` — DTO
- `security/` — JWT, фильтры
- `config/` — Security, exception handlers

## Ключевые моменты
- Админские эндпоинты: `/api/admin/**`
- Collaborative задания: submission может ждать участников (`WAITING_FOR_PARTICIPANTS`)
- Статусы submission: NOT_STARTED, INVITED, WAITING_FOR_PARTICIPANTS, PENDING, APPROVED, REJECTED, CANCELLED
- Медиа хранится в S3-совместимом хранилище (MinIO)
- Postman коллекция: `postman_collection.json`
