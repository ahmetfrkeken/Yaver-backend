# Yaver Backend

Backend for **Yaver**, a task manager with an AI assistant. Built with Kotlin and Ktor.
The Android client lives in a separate repo: `yaver-android` (coming soon).

## Features
- User registration and login (BCrypt password hashing, JWT authentication)
- Per-user task CRUD (users can only access their own tasks)
- PostgreSQL persistence with Exposed
- Docker Compose for the local database

## Tech stack
Kotlin, Ktor, Exposed, PostgreSQL, Docker, JWT

## Run locally
1. Start the database: `docker compose up -d`
2. Run the app from IntelliJ IDEA (`main.kt`) or with `./gradlew run`
3. The API is available at `http://localhost:8080`

## Configuration
The defaults are for local development only. In production, set:
- `JWT_SECRET`: secret used to sign tokens
- `DB_PASSWORD`: database password

## API
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | /auth/register | No | Create an account |
| POST | /auth/login | No | Log in, returns a JWT |
| GET | /auth/me | Yes | Current user |
| GET | /tasks | Yes | List my tasks |
| POST | /tasks | Yes | Create a task |
| PATCH | /tasks/{id} | Yes | Update a task |
| DELETE | /tasks/{id} | Yes | Delete a task |

## Roadmap
- [ ] AI assistant (natural language to task actions via tool use)
- [ ] Android client (Jetpack Compose)
- [ ] Deployment