# Front-end guidelines

## Product

Build the React front end for an in-the-moment study-buddy matching website for Cal Poly San Luis Obispo (SLO) students. A teammate owns the backend. Students select what they want to study, go online, and browse currently online study partners. Swiping right sends a match request; an accepted match opens a chat.

## Confirmed requirements

- Signup requires a nonblank name alongside Cal Poly email and password. Signup creates a backend account and navigates to profile setup.
- First signup setup uses three centered pages: major/classes, year/bio, and avatar/upload. Keep copy minimal and omit the left story panel.
- Profile setup requires at least one current class, entered as a subject and four-digit number (for example `CSC 2001`). Major is optional.
- Students may optionally provide a profile picture, bio, and year (First, Second, Third, Fourth, or Fifth+). The profile and candidate cards must work without these optional fields. A preferred study location remains optional for future profile editing.
- Do not show recurring study times in profiles. Select session duration beneath classes in Find a buddy with four single-choice boxes: 30, 60, 90, or 120+ minutes. Require an explicit selection without labeling it mandatory. Keep duration local until a backend field exists.
- Completed profile setup opens `/home`. Users can edit all profile fields, including their required name, through My profile. Four illustrated avatars from the supplied reference are available as photo alternatives.
- The homepage Find a buddy flow requires selecting one or more existing classes using checkboxes. Study location is optional and defaults to Kennedy Library when blank. Connect the existing backend APIs without editing the backend.
- Below the homepage top bar, show only a large Find a study buddy button. Clicking replaces it with an animated class/location form and Start looking button. Do not show introductory copy, class summaries, or chats below the homepage form.
- Before going online, students select which of their existing classes they want to study for in the current session. Keep this session selection separate from their full class list.
- Students can explicitly indicate that they are online and looking for a study buddy, and go offline when finished.
- Discovery shows only students who are currently online, ordered by likely compatibility. Classes in common are the most important matching signal; exact ranking rules remain to be coordinated with the backend teammate.
- Swipe left passes on a candidate; swipe right sends a match request.
- Requests should be silent and prioritize the requester in the recipient's deck; ranking belongs to the backend. The frontend preserves server order. The backend now prioritizes incoming requests; refresh the deck while preserving server order.
- Mutual matches open the server-created chat immediately when swiping. On other pages, show a large clickable match banner without interrupting the current page. Removal from the matching pool belongs to the backend; do not send offline on matching. Keep looking sessions and the deck across profile/chat navigation; Stop looking explicitly goes offline. Heartbeat integration remains a blank adapter until its backend contract exists.
- Discovery displays one candidate at a time. Support A/Left Arrow to pass and D/Right Arrow to request, alongside pointer swipes and labeled buttons. Use backend recommendations filtered by online presence. Requests remain pending until the backend confirms a mutual match.
- Make the request status clear. Sending a request is not itself an accepted match.
- After a match is accepted, show a chat between the matched students.
- Login and signup use email and password only. Accept only `@calpoly.edu` email addresses. Use the existing backend authentication API; frontend validation does not verify email ownership or authenticate users. Do not choose an authentication provider or claim verification is enforced by a front-end prototype.
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


- Use light mode only. Matches & chats opens a row list with an empty state and per-row Unmatch actions. Chat navigation must not redirect users home for missing matches. Keep the message composer within the viewport.

- Stop looking automatically after 60 seconds without a swipe decision (gesture, Pass/Request button, or keyboard). Count time across in-app navigation; use the same offline endpoint as Stop looking and clean up the timer when the session ends.

- No frontend mock profiles or simulated swipes. The explicit test generation action calls POST /test/profiles?count=100; backend configuration controls availability.
