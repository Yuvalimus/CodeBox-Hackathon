# Frontend

Start the backend at http://localhost:8080, then run npm install and npm run dev inside frontend. Restart Vite after configuration changes. Its /api development proxy forwards to port 8080; production requires an equivalent proxy or VITE_API_BASE_URL plus allowed backend CORS.

## API flows

Contracts follow backend/endpoints.md. Register sends username (display name), email, and password; login sends email and password. Names save through PATCH /me. Tokens live in sessionStorage, passwords are never stored. Log out ends presence then calls POST /logout and clears the local token.

Profile setup saves classes, name, major, bio, and an existing HTTPS picture URL. Year, avatar choice, and selected image files remain local until backend support exists. Profile class updates preserve the latest server studying subset. Looking sessions support up to 20 selected classes and default location to Kennedy Library.

Discovery preserves server ranking, including incoming-request priority, and intersects recommendations with active presence. The deck refreshes every five seconds without replacing a card mid-swipe. Requests are silent; mutual matches show an animation and open a chat after ending the current user's presence. Matches & chats is accessible from the home top bar. Chat messages poll while open and support older-message pagination. Looping test profiles bypass presence and decision writes.

Run npm test and npm run build to verify. For live end-to-end testing, use two accounts in separate browser sessions with a shared class; start looking, accept each other, open the resulting chat, exchange messages, and log out. No synthetic accounts are inserted automatically.

See BACKEND_HANDOFF.md for remaining server contract gaps. Backend files are not modified by frontend integration work.
