# Study Finder Backend — Agent Guide

Build a secure, small backend for a Tinder-like study-partner finder. Prefer a conventional REST API, TypeScript, Express/Fastify, SQLite, an ORM with migrations (Drizzle or Prisma), JWT authentication, and Zod (or equivalent) request validation. Keep the app runnable locally with a documented `.env.example` and tests for the critical matching/chat flows.

## Product rules

- A user can browse recommendations, accept or reject each recommendation, and only chat after a **mutual** acceptance.
- `classes`, `studying`, `subjects`, `preferredStudyLocations`, `matches`, and chat membership are arrays at the API boundary. Store them in normalized join tables where practical; do not depend on SQLite JSON querying for core invariants.
- Passwords must **not** use raw SHA-256. Use Argon2id (preferred) or bcrypt with a per-password salt. This deliberately overrides the initial raw-SHA-256 requirement because unsalted SHA-256 is unsafe for credentials.
- IDs in public APIs are opaque strings or positive integer IDs, consistently. Never expose password hashes or internal matching scores.
- All timestamps are UTC ISO 8601 strings.

## HTTP conventions

Use JSON request bodies for writes. The query-string examples below are legacy-compatible aliases only; never put real passwords in URLs because URLs leak through logs and browser history.

- Success: `200`/`201` and a JSON object.
- Invalid input: `400`; unauthenticated: `401`; token valid but forbidden/not a member: `403`; missing resource: `404`; duplicate username/email: `409`.
- Return a stable error shape: `{ "error": { "code": "...", "message": "..." } }`.
- Protect all routes except registration, login, and health checks with `requireAuth`.
- Apply rate limiting to registration/login and a smaller per-user limit to swipes and messages.

## Authentication routes

| Method | Route | Auth | Notes |
|---|---|---:|---|
| `POST` | `/register` | No | Body: `username`, `password`, `email`, plus optional profile fields. Validate and create user; return sanitized user and JWT. |
| `POST` | `/login` | No | Body: `username`, `password`. Verify Argon2id/bcrypt hash and return `{ token, user }`. |
| `GET` | `/health` | No | Lightweight health check. |
| `GET` | `/me` | Yes | Current sanitized profile. |
| `PATCH` | `/me` | Yes | Update profile/preferences; validate `studying` is a subset of `classes`. |

Support `GET /register?username=...&password=...` and `GET /login?...` only if explicitly required for a demo. Mark them deprecated, never log query strings, and remove them before production. The JWT should contain only `sub` (user ID), an expiry (for example 7 days), issuer, and audience. Read it from `Authorization: Bearer <token>`.

## User profile and database schema

Use migrations, foreign keys, indexes, and `PRAGMA foreign_keys = ON` for every SQLite connection.

### `users`

| Field | Type / rule |
|---|---|
| `id` | `INTEGER PRIMARY KEY AUTOINCREMENT` |
| `username` | required, unique, normalized case-insensitively; 3–32 chars |
| `email` | required, normalized, unique |
| `password_hash` | required; never return |
| `bio` | `TEXT NOT NULL DEFAULT ''`, max 500 chars |
| `picture_url` | nullable HTTPS URL (or approved upload URL) |
| `grad_year` | nullable integer, reasonable bounded range |
| `created_at`, `updated_at` | UTC timestamps |

Model the arrays with join tables:

- `user_classes(user_id, class_name)` — unique pair.
- `user_studying(user_id, class_name)` — unique pair; enforce in service code that each exists in `user_classes`.
- `user_study_times(user_id, hour_of_week)` — integer `0..167`, unique pair. Document this as the canonical meaning of “array of ints.”
- `user_preferred_locations(user_id, location)` — unique pair.

Do not keep `matches` or `chats` as mutable arrays on `users`; derive them from the relationship tables below. The API can still return arrays.

### Matching and chats

- `match_decisions(actor_user_id, target_user_id, decision, created_at)` with `decision IN ('accepted','rejected')`, unique `(actor_user_id, target_user_id)`, and `actor_user_id != target_user_id`.
- `matches(id, user_a_id, user_b_id, created_at)` with ordered user IDs (`user_a_id < user_b_id`) and a unique pair. This is created atomically only when both acceptance decisions exist.
- `chats(id, created_at)` and `chat_members(chat_id, user_id, joined_at)`; enforce one direct chat per matched pair in the service/transaction.
- `messages(id, chat_id, sender_user_id, body, created_at)`. Index `(chat_id, created_at, id)`. Limit message body to 2,000 characters.
- `looking_now(user_id PRIMARY KEY, subjects_json, expires_at, created_at)` is acceptable as a small TTL cache table. `subjects_json` contains a validated non-empty string array. Delete expired rows before reads (and run periodic cleanup); index `expires_at`.

Use foreign keys with appropriate cascading deletes only for dependent rows. Never cascade a user deletion into another user's messages without an explicit product decision.

## Required protected routes

| Method | Route | Behavior |
|---|---|---|
| `GET` | `/recommendations` | Return ranked, non-rejected, non-matched candidates for the authenticated user. Optional bounded `limit` (default 20, max 50). |
| `POST` | `/recommendations/:userId/accept` | Record acceptance. In one transaction, detect reciprocal acceptance; create `matches` and a direct chat idempotently if mutual. Return `{ decision: 'accepted', matched, match?, chat? }`. |
| `POST` | `/recommendations/:userId/reject` | Upsert a rejection and remove that user from the current recommendation cache. Return `204` or a small confirmation object. |
| `GET` | `/matches` | Return current user's matches with the other user's sanitized profile and chat summary. |
| `GET` | `/chats` | Return chats where the current user is a member, latest message preview, and unread count if implemented. |
| `GET` | `/chats/:chatId` | Require membership; return chat metadata and paginated messages. |
| `POST` | `/chats/:chatId/messages` | Require membership; body `{ message }`; persist and return the message. |
| `PUT` | `/looking-now` | Body `{ subjects: string[] }`; upsert a two-hour expiry. |
| `GET` | `/looking-now` | Return non-expired, compatible users; never return the requester. |
| `DELETE` | `/looking-now` | End the current user's looking-now presence. |

Pagination should use a cursor (`createdAt,id`) rather than unbounded offsets for messages and activity lists. Exclude the caller, blocked/rejected candidates, and existing matches from recommendations. Do not allow clients to create chats or choose arbitrary chat members: chats arise from mutual matches.

## Matching engine

At registration and whenever profile data affecting compatibility changes, rebuild the user's in-memory recommendation cache. Cache only short-lived candidate IDs/scores; SQLite is the source of truth. Use a TTL (for example 15 minutes), invalidate it after decisions/profile updates, and cap cache size. It must be safe to recompute after process restart.

For a candidate `c` and user `u`:

```text
score = 0.70 * studyingOverlap + 0.15 * timeOverlap + 0.15 * yearSimilarity
```

- `studyingOverlap`: Jaccard overlap of `u.studying` and `c.studying`; use `0` if both empty.
- `timeOverlap`: intersection size / min(nonzero set sizes) of weekly time slots; use `0` if either empty.
- `yearSimilarity`: `1` if both grad years are known and equal; otherwise `max(0, 1 - abs(years)/4)` when both known; `0` if either is missing.
- Sort descending by score; break ties deterministically by candidate ID. Persist neither score nor rank.

Keep the scorer pure and unit-tested. Fetch eligible candidates efficiently in batches and filter relationship exclusions in SQL. If recommendations need to be generated asynchronously later, preserve the same service interface.

## Middleware and service boundaries

Implement middleware in this order: request ID → secure headers/CORS → JSON parsing with body limit → request logger (redacts authorization/passwords) → rate limiter → route validation → auth → route handler → centralized error handler.

- `requireAuth`: verifies JWT signature, issuer, audience, expiry; loads current user or attaches `req.auth.userId`.
- `validate(schema)`: validates params, query, and body with Zod; reject unknown sensitive fields where appropriate.
- `requireChatMember`: loads chat and confirms `chat_members` contains `req.auth.userId`; use it on all chat/message routes.
- `requireMatchParticipant`: use for routes that operate on a specific match.
- Services own transactions and domain rules; controllers stay thin. Recommended services: `AuthService`, `UserService`, `RecommendationService`, `MatchService`, `ChatService`, and `LookingNowService`.

Use parameterized ORM/query calls exclusively. Set CORS to known frontend origins, configure JWT/database settings only from environment variables, and avoid logging tokens, passwords, message contents, or emails.

## Validation and authorization checklist

- Username: trimmed lowercase uniqueness key; reject reserved names and unsafe characters.
- Password: at least 12 characters (allow long passphrases); confirm only in the client, never store confirmation.
- All ID params: strict positive integer parser; target user must exist and must not equal caller.
- Array items: strings trimmed, deduplicated, bounded in count and length; reject empty items.
- Study times: integer hours `0..167`; deduplicate.
- URLs: require HTTPS for external pictures, with an allowlist if feasible.
- Message writes: membership check occurs before insert; sender is always the authenticated user, never body input.
- Match writes: do not accept a client-provided score, matched state, user pair, or chat ID.
- Use transactions for accept → reciprocal decision check → match creation → chat creation. Enforce uniqueness so retries are idempotent.

## Delivery bar

Before considering the backend complete, include migrations, seed-free tests, OpenAPI or route documentation, `.env.example`, and tests covering registration/login, expired/invalid JWTs, recommendation exclusions and score ordering, rejection idempotency, mutual acceptance producing exactly one match/chat, chat membership enforcement, message validation, and looking-now TTL behavior. Ensure `npm test`, typecheck, lint, and migration commands are documented and pass.
