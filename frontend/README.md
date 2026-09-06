# Frontend

Run npm install and npm run dev inside frontend. API calls default to https://study.happyxd.dev/api/. VITE_API_BASE_URL can override this address; restart Vite after configuration changes. The hosted backend must allow the frontend origin through CORS.

## API flows

During local development, the frontend uses the Vite `/api` proxy to reach the hosted API without a browser CORS request. A separately hosted production frontend must still be permitted by the API server's CORS policy.

Contracts follow backend/endpoints.md. Register sends username (display name), email, and password; login sends email and password. Names save through PATCH /me. Tokens live in sessionStorage, passwords are never stored. Log out ends presence then calls POST /logout and clears the local token.

Profile setup saves classes, name, major, bio, and the selected avatar through PATCH /me; selecting an avatar saves it immediately, including the default sage avatar. Photos save through POST /me/picture. Relative upload URLs resolve against the API host (or the local API proxy). Signup accepts 8–200 character passwords. Active matches are exclusive; chats expire after 24 hours. Profile class updates preserve the latest server studying subset. Looking sessions support up to 20 selected classes and default location to Kennedy Library.

Discovery preserves server ranking, including incoming-request priority, and intersects recommendations with active presence. The deck refreshes every five seconds without replacing a card mid-swipe. Requests are silent; mutual matches show an animation and open a chat after ending the current user's presence. Matches & chats is accessible from the home top bar. Chat messages poll while open and support older-message pagination. Looping test profiles bypass presence and decision writes.

Run npm test and npm run build to verify. For live end-to-end testing, use two accounts in separate browser sessions with a shared class; start looking, accept each other, open the resulting chat, exchange messages, and log out. No synthetic accounts are inserted automatically.

See BACKEND_HANDOFF.md for remaining server contract gaps. Backend files are not modified by frontend integration work.


Test data: enable TEST_DATA_ENABLED=true on the updated backend, then use Generate 100 test profiles in Find a buddy. This inserts a fresh batch into the backend database and permanent queue; it is not a simulated deck. Start looking uses normal ranking and request APIs. With TEST_DATA_ENABLED enabled, generated users have a 30% chance of accepting each request, creating a real match/chat. They do not send automated messages.
