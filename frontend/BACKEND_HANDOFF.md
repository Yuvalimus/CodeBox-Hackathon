# Remaining backend integration gaps

Reviewed against backend/endpoints.md and the current Java services. Backend files were not changed.

## Now supported

Email/password login; display names through username at registration and PATCH /me; requester-first recommendations; mutual acceptance with a direct chat ID; profiles, messages, presence, and token revocation on logout.

## Still needed

- Pool removal on matching belongs to the backend. Frontend clients stop their looking session/heartbeat on a confirmed match and no longer send DELETE presence on match.
- Include match ID, partner profile, and chat ID together in GET /matches, and participant details in GET /chats. Current matches have only a user object; chats have no partner information. The frontend does not guess relationships by array position.
- A durable match event/cursor or live event stream for both users. Open clients currently poll chat IDs every five seconds after a baseline; caches and browser throttling can delay detection. Existing chats are accessible through Matches & chats.
- Persist avatar selection and college year. Photo uploads are now integrated through POST /me/picture. College year is not graduation year.
- Expose the authenticated user's current presence/session including location and expiry so refresh can restore looking state reliably. Currently the presence GET excludes self; location is saved in preferredStudyLocations.
- Paginate recommendations or filter online candidates before limiting: the frontend can only filter the first 50 returned recommendations by presence, so additional online users may be omitted.

The backend still returns a compatibility number despite its agent guideline against exposing internal scores. The frontend never renders it. Frontend course input intentionally follows the requested four-digit format (CSC 2001); endpoint examples use older three-digit codes, which can still be displayed when returned by the server.

Media integration is implemented in frontend/src/profileMedia.js. No profile data is persisted in cookies or browser storage. Uploaded photos are stored by the backend with a database URL; avatar choices and college year remain memory-only.

Backend inconsistencies: PATCH /me rejects the relative pictureUrl returned by uploads; frontend omits unchanged picture URLs. Expired chat access returns 403/not_chat_member rather than a distinct expiry response; the UI reports the chat as unavailable. Repeating an accept on an existing match returns 409/user_unavailable despite endpoint wording that mentions returning an existing chat. The test-data endpoint exists but is not called automatically; the looping frontend test deck remains isolated.

## Presence lifecycle skeleton

App owns the in-memory looking session; Home and its deck stay mounted but hidden during profile/chat navigation. Keyboard swipes are disabled when Home is hidden. Stop looking uses the current DELETE /looking-now endpoint through presence.js; only clear the session after success. Navigation does not send an offline request. Browser reload restoration still needs a current-session endpoint.

PresenceHeartbeat owns a cancellable, non-overlapping 30-second loop across routes. Its heartbeat API adapter is deliberately empty until the backend defines the route, cadence, expiry, and response. Heartbeats must only renew existing presence, never recreate a matched/stopped session. Matching ends the frontend looking session without sending offline: removal from the pool belongs to the backend. A match opens chat immediately while swiping, or shows a clickable banner elsewhere without interrupting the current chat/editor.

