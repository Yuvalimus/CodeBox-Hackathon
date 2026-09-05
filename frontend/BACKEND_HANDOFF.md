# Matching contract handoff (backend unchanged)

## Backend-owned behavior still required

1. Incoming accept decisions should prioritize the requester in the recipient's recommendations. Do not send a request notification. The current recommendation response has no incoming-decision field and its ranking does not prioritize these actors. The frontend preserves server order and cannot safely invent this priority.
2. On mutual acceptance, delete both users' looking-now rows within the match transaction and invalidate presence caches. DELETE /looking-now only affects the authenticated caller. The frontend can end its own presence, never another account's presence. Closed clients otherwise remain online until expiry.
3. Expose a stable match event with match ID, partner profile, and chat ID to both clients, ideally through events or a cursor-based endpoint. GET /matches currently returns partner profiles without chat IDs; GET /chats returns chat IDs without partners. The frontend detects new chat IDs after establishing a baseline, while the accepting client uses the explicit accept response. Existing chats on first load are not falsely treated as new matches. Several simultaneous new matches remain accessible in the chat list; only one celebration is displayed.

## Frontend implementation

- No incoming-request notification UI, browser notification, or sound.
- Right swipe records a server decision; only `matched: true` or a newly observed server-created chat triggers celebration.
- `/match` animates, ends the current user's presence, then opens `/chat` after 2.4 seconds. Reduced-motion preferences disable animation. An offline failure provides retry and prevents claiming the user is offline.
- Both active clients poll `/chats` every five seconds. Backend caches can delay detection. Chat messages refresh while their chat is open.
- Recipient priority and atomic removal of both presences are not claimed as implemented.

## Other existing API gaps

Email-only login needs native backend email lookup. The current frontend derives usernames for its own registrations. Display names, college year, avatar persistence, and photo uploads also need server support. Client validation is for usability; backend validation remains authoritative. HTTPS picture URLs are supported already.

## Manual two-client test

Sign into two separate browser sessions, use a common class, and start looking on both. Request from A: B should see no request notification. Request back from B: B celebrates immediately from the server response; A celebrates after polling detects the new chat. Each open client deletes its own presence and enters the same chat. Send messages and allow for the backend cache. Test offline failure with the backend unavailable and verify Retry works without resending the swipe.
