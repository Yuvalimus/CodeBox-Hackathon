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

Public endpoints: `POST /register`, `POST /login`, `GET /health`. Login accepts `{ "email", "password" }`; passwords must be 8–200 characters. Emails are unique, while usernames are display names and may be shared. Usernames are display names, so spaces and symbols are accepted (up to 32 characters). Failed login attempts return `401` with `error.code: "invalid_credentials"` and the message `Invalid username or password`. A reused email returns `409` with `error.code: "email_already_used"`.
All other routes require `Authorization: Bearer <token>`.

`GET /recommendations`, `POST /recommendations/{userId}/accept`, `POST /recommendations/{userId}/reject`,
`GET /matches`, `GET /chats`, `GET /chats/{chatId}`, `POST /chats/{chatId}/messages`,
and `PUT|GET|DELETE /looking-now` implement the product flows. Profile reads/updates are at `GET|PATCH /me`.

`PATCH /me` updates any supplied profile field: `username`, `email`, `bio`, `pictureUrl`, `major`, `gradYear`, `classes`, `studying`, `studyTimes`, or `preferredStudyLocations`. Profile arrays are JSON arrays at the API boundary.

Upload a profile picture with authenticated `POST /me/picture` using multipart field `file`. JPEG, PNG, and WebP files up to 5 MB are stored at `UPLOAD_DIR` (default: `uploads/profile-pictures`) and saved to `pictureUrl`.

`POST /logout` requires a bearer token and returns `204`; that token is revoked until its normal expiry. When someone accepts a recommendation, they are prioritized in the recipient's recommendation list until the recipient swipes on them.

Mutual matches are exclusive while their direct chat is active. Direct chats expire after 24 hours; expiration deletes the chat, its messages, and the match record, then returns both users to recommendation pools.

For local development, set `TEST_DATA_ENABLED=true` to enable `POST /test/profiles?count=10`. It requires a bearer token and creates 1–50 randomized test profiles; it is disabled by default.
