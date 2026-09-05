# Front-end guidelines

## Product

Build the React front end for an in-the-moment study-buddy matching website for Cal Poly San Luis Obispo (SLO) students. A teammate owns the backend. Students select what they want to study, go online, and browse currently online study partners. Swiping right sends a match request; an accepted match opens a chat.

## Confirmed requirements

- Student profiles include major and current classes.
- Students may optionally provide photos, a bio, their year, and a preferred study location. The profile and candidate cards must work without these optional fields.
- Do not include availability schedules: study sessions are initiated in the moment.
- Before going online, students select which of their existing classes they want to study for in the current session. Keep this session selection separate from their full class list.
- Students can explicitly indicate that they are online and looking for a study buddy, and go offline when finished.
- Discovery shows only students who are currently online, ordered by likely compatibility. Classes in common are the most important matching signal; exact ranking rules remain to be coordinated with the backend teammate.
- Swipe left passes on a candidate; swipe right sends a match request.
- Make the request status clear. Sending a request is not itself an accepted match.
- After a match is accepted, show a chat between the matched students.
- School-email verification is the intended approach, pending final confirmation. Do not choose an authentication provider or claim verification is enforced by a front-end prototype.
- The primary brand color is `#CCDDB7`.
- Additional brand colors have not been selected. Keep the palette easy to extend and treat any necessary neutral colors as provisional.

## Front-end scope

Use React. Scope work to the front end: profile setup/editing, selecting classes for the current study session, going online/offline, ranked candidate discovery, incoming/outgoing match requests, and post-match chat. Plan for a school-email verification UI once the authentication flow is agreed. The exact navigation and screen layout remain open.

The backend is being developed by a teammate. Do not implement backend services or independently choose authentication, persistence, presence, ranking, or real-time messaging infrastructure. Coordinate the data and API contracts with that teammate. While APIs are unavailable, use clearly identified mock data behind a replaceable data interface, including mock presence, match-request, and chat states. Do not present simulated requests or messages as delivered to real students.

The product name is undecided. `KennedyMatch` is only a candidate, not an approved name; keep any temporary name easy to replace.

## Interaction and accessibility

- Provide labeled Pass and Request to match buttons alongside swipe gestures so the core flow works with a mouse, touch, and keyboard.
- Clearly distinguish online availability, a pending request, and an accepted match.
- Explain visible similarities using available profile data; do not invent matching scores.
- Include loading, empty, error, and request-pending states where applicable.
- Prevent duplicate request submissions while a request is pending.
- Use responsive layouts suitable for phones and desktop browsers.
- Maintain readable contrast, visible keyboard focus, accessible form labels, and comfortable touch targets. Do not rely on color alone to communicate state.
- Store brand colors in shared design tokens. Check contrast when using the light primary color for backgrounds, controls, or text.

## Decisions to resolve with the product owner

- Final confirmation of school-email verification and the backend authentication/API contracts.
- How online status expires and how the UI handles candidates going offline.
- Exact ranking rules, including how current-session class selections relate to full class-list overlap and how ties are resolved. Shared classes must remain the strongest signal.
- Request acceptance/decline details, expiration, and whether chats persist after students go offline.
- Overall visual style, remaining colors, product name, and any logo assets.

## Working conventions

- Treat confirmed requirements as the source of truth and keep unresolved choices explicit.
- Prefer straightforward, reusable components and plain student-facing language.
- Keep changes focused on the requested front-end work.
- Update this document when product decisions are confirmed.
