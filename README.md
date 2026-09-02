# WomansDay API

WomansDay API is a backend for a corporate March 8th event. It lets participants complete tasks, submit text or media evidence, earn tulips, receive loot boxes, and track their place on the leaderboard. Administrators manage tasks, review submissions, distribute loot boxes, export approved media, and control whether the game is active.

## Features

- JWT-based registration, login, token refresh, logout, and password changes
- Text, media, and combined task submissions
- Collaborative tasks with participant invitations and responses
- Administrator review, approval, rejection, and cancellation of submissions
- Tulip balance calculated from immutable balance transactions
- Loot boxes awarded for the first completed task and reward milestones
- User avatars and submission-media storage on the local file system
- User, task, balance, loot box, and leaderboard statistics for administrators
- ZIP export of approved submission media and text responses

## Technology

- Java 17
- Spring Boot 3
- Gradle
- PostgreSQL
- Spring Data JPA
- Spring Security and JWT
- Docker and Docker Compose

## Requirements

- Java 17 or newer
- PostgreSQL
- A writable directory for media storage

Docker is optional if you prefer to run the application in containers.

## Configuration

Configure the application through Spring profiles and properties. The application uses the `dev` profile by default.

The following properties are required by the application:

| Property | Environment variable | Purpose |
| --- | --- | --- |
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` | PostgreSQL JDBC connection URL |
| `spring.datasource.username` | `SPRING_DATASOURCE_USERNAME` | PostgreSQL user name |
| `spring.datasource.password` | `SPRING_DATASOURCE_PASSWORD` | PostgreSQL password |
| `jwt.secret` | `JWT_SECRET` | Secret used to sign JWTs |
| `jwt.access-expiration-ms` | `JWT_ACCESS_EXPIRATION_MS` | Access-token lifetime in milliseconds |
| `jwt.refresh-expiration-ms` | `JWT_REFRESH_EXPIRATION_MS` | Refresh-token lifetime in milliseconds |
| `admin.login` | `ADMIN_LOGIN` | Login for the initial administrator |
| `admin.password` | `ADMIN_PASSWORD` | Password for the initial administrator |
| `cors.allowed-origins` | `CORS_ALLOWED_ORIGINS` | Origins allowed to call the API |
| `app.media.storage-dir` | `APP_MEDIA_STORAGE_DIR` | Local directory for avatars and submission files |

Do not commit production credentials or the JWT secret. In the `prod` profile, the administrator password must be at least 12 characters long.

## Run Locally

1. Create a PostgreSQL database and configure the required properties for the selected profile.
2. Start the application:

```bash
./gradlew bootRun
```

3. Check that it is running:

```bash
curl http://localhost:8080/api/status
```

The default local base URL is `http://localhost:8080`.

## Run With Docker

The repository includes a `Dockerfile` and `docker-compose.yml`. Configure the values required by your Compose environment, then build and start the services:

```bash
docker compose up --build
```

## Tests

Run the test suite with:

```bash
./gradlew test
```

## Authentication And Access

`/api/status`, `/api/`, `/api/brew-coffee`, and most `/api/auth/**` endpoints are public. All other endpoints require an access token:

```http
Authorization: Bearer <access-token>
```

Administrator-only operations are under `/api/admin/**`. The initial administrator is created when the user table is empty, using `admin.login` and `admin.password`.

Register a participant or log in to receive an access token and refresh token:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{
    "login": "maria",
    "password": "securepassword",
    "firstName": "Maria",
    "lastName": "Ivanova",
    "department": "Marketing"
  }'
```

Use `POST /api/auth/login` for an existing account, `POST /api/auth/refresh` to renew tokens, and `POST /api/auth/logout` to revoke a refresh token.

## Participant Workflow

1. Register or log in and store the returned access token.
2. View available tasks with `GET /api/tasks`.
3. Submit a task through `POST /api/tasks/{id}/submit` as `multipart/form-data`.
4. Add `text`, one or more `files`, and `participantIds` when required by the task.
5. For a collaborative task, invited users view `GET /api/users/me/invitations` and respond through `POST /api/tasks/submissions/{submissionId}/respond?accept=true`.
6. View submission status with `GET /api/users/me/submissions`.
7. After approval, view balance history through `GET /api/users/me/history` and open owned loot boxes with `POST /api/lootbox/{id}/open`.

Task types are `text`, `media`, and `textAndMedia`. A submission can contain up to five files and up to 5,000 text characters.

Submission statuses are `notStarted`, `invited`, `waitingForParticipants`, `pending`, `approved`, `rejected`, and `cancelled`.

## Rewards And Loot Boxes

Tulips are the in-game currency. A user balance is the sum of records in `balance_transactions`, rather than a mutable user-balance field.

When an administrator approves a submission, every participant receives the task reward as a transaction. A participant receives one loot box after their first approved task and additional boxes for each 150 tulips earned from task rewards, up to the configured milestone cap. Opening a loot box awards a random tulip prize.

## API Overview

| Area | Base path | Examples |
| --- | --- | --- |
| Status | `/api` | `GET /status` |
| Authentication | `/api/auth` | register, login, refresh, logout, change password |
| Users | `/api/users` | profile, avatars, submissions, invitations, balance history |
| Tasks | `/api/tasks` | list tasks, submit, respond to invitations, download media, budget |
| Loot boxes | `/api/lootbox` | list owned boxes, open a box |
| Administration | `/api/admin` | task CRUD, review submissions, user management, statistics, media export, game state |

For complete request examples and response models, import [`postman_collection.json`](postman_collection.json) into Postman. The collection stores access and refresh tokens automatically after a successful registration or login.

## Game Control

Administrators can stop new submissions and loot-box openings with `POST /api/admin/game/finish`, and allow them again with `POST /api/admin/game/resume`.
