# Frontend

Run npm install and npm run dev inside frontend. API calls default to https://study.happyxd.dev/api/. VITE_API_BASE_URL can override this address; restart Vite after configuration changes. The hosted backend must allow the frontend origin through CORS.

## API flows

Contracts follow backend/endpoints.md. Register sends username (display name), email, and password; login sends email and password. Names save through PATCH /me. Tokens live in sessionStorage, passwords are never stored. Log out ends presence then calls POST /logout and clears the local token.

Profile setup saves classes, name, major, bio, and photos through POST /me/picture. Year and avatar choice remain temporary until backend fields exist. Relative upload URLs resolve against the API host (or the local API proxy). Signup accepts 8–200 character passwords. Active matches are exclusive; chats expire after 24 hours. Profile class updates preserve the latest server studying subset. Looking sessions support up to 20 selected classes and default location to Kennedy Library.

Discovery preserves server ranking, including incoming-request priority, and intersects recommendations with active presence. The deck refreshes every five seconds without replacing a card mid-swipe. Requests are silent; mutual matches show an animation and open a chat after ending the current user's presence. Matches & chats is accessible from the home top bar. Chat messages poll while open and support older-message pagination. Looping test profiles bypass presence and decision writes.

Run npm test and npm run build to verify. For live end-to-end testing, use two accounts in separate browser sessions with a shared class; start looking, accept each other, open the resulting chat, exchange messages, and log out. No synthetic accounts are inserted automatically.

See BACKEND_HANDOFF.md for remaining server contract gaps. Backend files are not modified by frontend integration work.

