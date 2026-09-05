# Remaining backend integration gaps

Reviewed against backend/endpoints.md and the current Java services. Backend files were not changed.

## Now supported

Email/password login; display names through username at registration and PATCH /me; requester-first recommendations; mutual acceptance with a direct chat ID; profiles, messages, presence, and token revocation on logout.

## Still needed

- Delete both looking_now rows atomically when a mutual match is created, and invalidate presence caches. Current frontend clients each delete their own presence after detecting a match. A closed client cannot do that.
- Include match ID, partner profile, and chat ID together in GET /matches, and participant details in GET /chats. Current matches have only a user object; chats have no partner information. The frontend does not guess relationships by array position.
- A durable match event/cursor or live event stream for both users. Open clients currently poll chat IDs every five seconds after a baseline; caches and browser throttling can delay detection. Existing chats are accessible through Matches & chats.
- Persist avatar selection and college year, and provide photo uploads. HTTPS pictureUrl works already. College year is not graduation year.
- Expose the authenticated user's current presence/session including location and expiry so refresh can restore looking state reliably. Currently the presence GET excludes self; location is saved in preferredStudyLocations.
- Paginate recommendations or filter online candidates before limiting: the frontend can only filter the first 50 returned recommendations by presence, so additional online users may be omitted.

The backend still returns a compatibility number despite its agent guideline against exposing internal scores. The frontend never renders it. Frontend course input intentionally follows the requested four-digit format (CSC 2001); endpoint examples use older three-digit codes, which can still be displayed when returned by the server.

Media integration placeholder: frontend/src/profileMedia.js. Implement authenticated uploads and database-backed avatarId, returning the updated user and serving the original-resolution photo URL. No media is persisted in cookies or browser storage; current selections are memory-only previews.
