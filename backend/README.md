# Study Finder API

Spring Boot + SQLite backend for mutual study-partner matching.

## Run

Copy `.env.example` to your shell environment (or supply equivalent environment variables), then run:

```powershell
.\gradlew.bat bootRun
```

The API listens on `http://localhost:8080`. SQLite schema migrations are applied at startup.

## Checks

```powershell
.\gradlew.bat test
.\gradlew.bat check
```

## API

Public endpoints: `POST /register`, `POST /login`, `GET /health`.
All other routes require `Authorization: Bearer <token>`.

`GET /recommendations`, `POST /recommendations/{userId}/accept`, `POST /recommendations/{userId}/reject`,
`GET /matches`, `GET /chats`, `GET /chats/{chatId}`, `POST /chats/{chatId}/messages`,
and `PUT|GET|DELETE /looking-now` implement the product flows. Profile reads/updates are at `GET|PATCH /me`.

Profile arrays (`classes`, `studying`, `studyTimes`, `preferredStudyLocations`) are JSON arrays at the API boundary.
