# Study Finder API endpoints

Base URL in local development: `http://localhost:8080`.

All write requests use JSON. Except for `/health`, `/register`, and `/login`, send:

```http
Authorization: Bearer <token>
```

Successful errors use this stable shape:

```json
{
  "error": {
    "code": "invalid_request",
    "message": "Request body or parameters are invalid"
  }
}
```

## Authentication

### `POST /register`

Creates an account and returns an access token plus the authenticated user profile.

```json
{
  "username": "Alex Rivera",
  "email": "alex@calpoly.edu",
  "password": "at-least-8-characters",
  "bio": "Optional; up to 500 characters.",
  "comments": "Studying for one hour before the midterm.",
  "pictureUrl": "https://example.com/alex.jpg",
  "avatar": "sage",
  "major": "Computer Science",
  "gradYear": 2027,
  "classes": ["CSC 357"],
  "studying": ["CSC 357"],
  "studyDurationMinutes": 60,
  "preferredStudyLocations": ["Kennedy Library"]
}
```

`username`, `email`, and `password` are required. Passwords must be 8–200 characters. Usernames are display names: they may contain spaces and symbols and are not unique. Emails must be unique `@calpoly.edu` addresses. `studying` must be a subset of `classes`.

Returns `201 Created`:

```json
{
  "token": "<jwt>",
  "user": { "id": 1, "username": "Alex Rivera", "email": "alex@calpoly.edu" }
}
```

If the email is already registered, returns `409` with `error.code` of `email_already_used`.

### `POST /login`

```json
{
  "email": "alex@calpoly.edu",
  "password": "at-least-8-characters"
}
```

Returns `200` with `{ "token", "user" }`. An unknown email or wrong password returns `401` with `error.code` of `invalid_credentials` and message `Invalid username or password`.

### `POST /logout`

Requires a bearer token. Returns `204 No Content` and revokes that token until it expires. Delete the local token too.

### `GET /health`

Returns `200`:

```json
{ "status": "ok" }
```

## Current profile

### `GET /me`

Returns the authenticated user's complete profile, including their email.

### `PATCH /me`

Updates one or more profile fields and returns the complete updated profile. The editable fields are `username`, `email`, `bio`, `comments`, `pictureUrl`, `avatar`, `major`, `gradYear`, `studyDurationMinutes`, `classes`, `studying`, and `preferredStudyLocations`. `studyDurationMinutes` is an integer from `15` through `480`, in 15-minute increments; it defaults to `60`. `avatar` must be one of `sage`, `blue`, `peach`, or `lavender`; omitted avatars default to `sage`. `comments` is a public, optional 500-character field for describing what the user is looking for.

```json
{
  "username": "Alex Rivera",
  "bio": "I like afternoon review sessions.",
  "comments": "Studying for one hour before my exam.",
  "studyDurationMinutes": 60,
  "classes": ["CSC 357", "STAT 312"],
  "studying": ["CSC 357"]
}
```

When changing any profile array, send a complete valid array for that field. If `classes` or `studying` changes, the resulting `studying` list must remain a subset of `classes`. Updating `email` to one used by another user returns `409/email_already_used`.

### `POST /me/picture`

Uploads and saves a profile picture. Send a `multipart/form-data` request with a `file` field and the bearer token. JPEG, PNG, and WebP files are supported up to 5 MB.

```http
POST /me/picture
Authorization: Bearer <token>
Content-Type: multipart/form-data
```

Returns the complete updated profile. Its `pictureUrl` is a public URL such as `/uploads/profile-pictures/0f5f4da0-8d3e-4cfd-a8bf-4c1c3e2c5db4.jpg`.

## Online queue

Users must be in the online queue to appear in recommendations. The client joins the queue only after the user starts looking for a study buddy, sends a heartbeat every 30 seconds for that active discovery session, and leaves the queue when the user stops looking or navigates away. A user is removed automatically after three missed heartbeats (90 seconds). Generated development test profiles are permanent queue members and do not need heartbeats.

### `POST /queue`

Adds the authenticated user to the recommendation queue. Returns `{ "online": true, "expiresAt": "..." }`.

### `POST /queue/heartbeat`

Refreshes the authenticated user's queue presence for 90 seconds and returns `looking: true`. Call this every 30 seconds only while the user is actively looking for a study buddy. The endpoint can report a non-queued site presence for other callers, but the web client does not send heartbeats outside an active discovery session.

### `GET /queue`

Returns the authenticated user's queue state. When queued, the response includes `expiresAt`; otherwise it returns `{ "online": false }`.

### `DELETE /queue`

Immediately removes the authenticated user from recommendations. Use this for a Stop Looking button. Returns `204 No Content`.

## Recommendations and matches

### `GET /recommendations?limit=20`

Returns up to 20 candidates by default; `limit` must be from 1 through 50.

```json
{
  "recommendations": [
    {
      "id": 12,
      "username": "Sam Lee",
      "major": "Mathematics",
      "classes": ["CSC 357"],
      "studying": ["CSC 357"],
      "studyDurationMinutes": 60,
      "preferredStudyLocations": [],
      "compatibility": 0.85
    }
  ]
}
```

Candidates who have already accepted the authenticated user are prioritized first.
Compatibility is ranked as 70% shared `studying` classes, 15% preferred study-duration similarity, and 15% graduation-year similarity. Study-duration similarity is the shorter requested duration divided by the longer requested duration, so equal durations receive full credit.
Standard accept and reject decisions expire after five minutes, after which the two users can appear in one another's recommendations and decide again. A rejection records the cooldown for both users, so either person is hidden from the other's recommendations during those five minutes. A deferred accept is retained only while its requester remains in the queue.

Mutual matches are exclusive while their direct chat is active: users with an active match are excluded from all recommendation lists. When the chat expires after 24 hours, its match is deleted and both users return to recommendation pools.

### `POST /recommendations/{userId}/accept`

Records an accept. If the other person has not accepted yet:

```json
{ "decision": "accepted", "matched": false }
```

If the selected person already has an active match, the request is accepted as a deferred request and returns the same unmatched response. It does not disclose that person's matched status. The requester remains queued for that person after they unmatch, as long as the requester is still actively looking. An unexpired rejection cooldown still returns an empty JSON response (`{}`).

For a mutual accept, returns the created (or existing) match and direct chat:

```json
{
  "decision": "accepted",
  "matched": true,
  "match": { "id": 4 },
  "chat": { "id": 9 }
}
```

### `POST /recommendations/{userId}/reject`

Records a reciprocal rejection and returns `204 No Content`. Both users are hidden from one another's recommendations for five minutes.

### `GET /matches`

Returns matched users. Match profile responses never include another user's email.

```json
{
  "matches": [
    {
      "user": { "id": 12, "username": "Sam Lee", "major": "Mathematics" }
    }
  ]
}
```

## Chats

Chats are created only by mutual accepts; clients cannot create arbitrary chats.
Chats expire 24 hours after creation. Expiration deletes the chat, its messages, and its match; both users then become eligible recommendations again.

### Authenticated WebSocket chat events

First request a one-time WebSocket ticket, then connect to `/ws/chat?ticket=<ticket>` (or `/api/ws/chat` when the reverse proxy exposes the API beneath `/api`). Tickets are valid for one minute and are consumed by the handshake, so the normal bearer token is never placed in the WebSocket URL.

### `POST /ws/chat-ticket`

Requires a bearer token and returns a short-lived, single-use ticket:

```json
{ "ticket": "..." }
```

The server sends each chat member a live event after a message is saved:

```json
{
  "type": "chat.message",
  "chatId": "uuid",
  "message": { "id": 31, "senderUserId": 1, "body": "Want to study?", "createdAt": "2026-09-05T20:00:00Z" }
}
```

For a reverse proxy, forward WebSocket `Upgrade` and `Connection` headers for this route.

### `GET /chats`

Returns the current user's chat summaries:

```json
{
  "chats": [
    { "id": 9, "createdAt": "2026-09-05T20:00:00Z", "latestMessage": "Want to study?" }
  ]
}
```

### `GET /chats/{chatId}?cursor={createdAt,id}`

`chatId` is a UUID.

Returns the newest messages first, with up to 50 messages per page. Pass `nextCursor` from a previous response to fetch older messages.

```json
{
  "id": 9,
  "messages": [
    { "id": 31, "senderUserId": 1, "body": "Want to study?", "createdAt": "2026-09-05T20:00:00Z" }
  ],
  "nextCursor": null
}
```

### `POST /chats/{chatId}/messages`

`chatId` is a UUID.

```json
{ "message": "Want to review after class?" }
```

Returns `201 Created` with the saved message. Messages are trimmed and must be 1–2,000 characters. Non-members receive `403/not_chat_member`.

### `DELETE /chats/{chatId}`

Ends the authenticated member's direct chat, removes its match, and returns both participants to the queue. The pair is hidden from one another's recommendations for five minutes before they can be recommended again. Returns `204 No Content`.

## Looking now

### `PUT /looking-now`

Advertises the current user for two hours.

```json
{ "subjects": ["CSC 357", "STAT 312"] }
```

`subjects` must be a non-empty array of up to 20 non-empty strings. Returns `204 No Content`.

### `GET /looking-now`

Returns other unexpired active users, each with `subjects` and `expiresAt`.

```json
{ "users": [] }
```

### `DELETE /looking-now`

Stops the authenticated user's active presence and returns `204 No Content`.

## Development test data

### `POST /test/profiles?count=10`

Creates randomized study profiles for local development and returns their sanitized profiles. It requires a bearer token and accepts a `count` from 1 through 50 (default: 10).

This route is disabled by default. Set `TEST_DATA_ENABLED=true` before starting the backend to enable it. Disabled environments return `404`.

```json
{
  "profiles": [
    { "id": 12, "username": "Alex Rivera", "classes": ["CSC 357"], "studying": ["CSC 357"] }
  ]
}
```
