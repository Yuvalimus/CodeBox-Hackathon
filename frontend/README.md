# Frontend API integration

Run the backend on port 8080, then run `npm install` and `npm run dev` inside frontend. Restart Vite for the new proxy configuration. Development requests to /api forward to http://localhost:8080. Production needs an equivalent proxy or VITE_API_BASE_URL plus backend CORS configuration.

Connected: registration, login, GET/PATCH /me, PUT/DELETE /looking-now, recommendations filtered through online presence, accept/reject, matches, chats, paginated messages, and message sending. Use Refresh matches/messages for incoming updates. Backend presence expires in two hours and is not ended by navigating away; use Stop looking. Discovery checks the first 50 recommendations and the backend presence cache may lag 30 seconds. Session location is saved as the first preferredStudyLocations entry because there is no session location field.

Authentication uses bearer tokens in sessionStorage. Passwords are never stored. Registration requires 12-200 trimmed characters. Refresh retrieves /me.

Backend gaps: login accepts username, not email. Registration and login derive a stable 32-character username from normalized email (SHA-256, cp_ prefix). Accounts registered through this frontend work with email-only login; existing accounts with other usernames need backend email-login support. The backend has no display name, college year, avatar, or photo upload support. These fields are explicitly local only. Other students see backend usernames. HTTPS picture URLs, classes, major, and bio persist. Graduation year is not substituted for college year. Chat summaries lack participant names and are labeled by ID. Pending requests are shown for the current deck only because there is no pending-request endpoint.

Checks: `npm run build` and `node --test src/api.test.js`.

Live test: create two accounts in separate browser sessions, save shared classes, start looking on both, request each other, refresh matches, open chat, and send messages. No synthetic accounts are inserted automatically.

Structure: src/pages contains pages, src/components reusable UI, src/api.js API adapters, and src/App.jsx routes.

Matching updates: /match celebrates a confirmed match, takes the current account offline, and opens /chat. Active clients poll for new chats every five seconds and open chats poll messages. Server ranking is preserved. See BACKEND_HANDOFF.md for request prioritization and atomic two-user offline behavior that cannot be implemented with the existing frontend permissions.
