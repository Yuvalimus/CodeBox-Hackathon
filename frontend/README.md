# Frontend

React login and signup screens for Cal Poly students.

```sh
npm install
npm run dev
```

Open `/` for the landing page, `/login` to log in, or `/signup` to create an account. Build with `npm run build`; serve the build with `npm run preview`. Production hosting should fall back to `index.html` for client routes.

Only email and password are supported. Email validation accepts the exact `calpoly.edu` domain, case-insensitively, with surrounding whitespace ignored. Signup requires a matching password confirmation and a provisional minimum of eight characters.

API integration is intentionally unimplemented. Valid login submissions show a preview notice. Signup requires a nonblank name and routes to `/profile-setup` for testing. Neither flow authenticates, creates an account, verifies email ownership, or persists credentials. The integration placeholder is in `src/components/auth/AuthForm.jsx` inside `submit`. Backend validation and the final password policy must be coordinated when connecting authentication.

The temporary product label is configured in `src/config/brand.js`; shared color tokens are in `src/styles.css`.

## Source structure

- `src/main.jsx`: React entry point and global stylesheet import.
- `src/App.jsx`: route table, navigation, and page titles. Register new pages here.
- `src/pages/`: separate landing, login, signup, and not-found page components. Landing page styles are scoped in `LandingPage.css`.
- `src/layouts/AuthLayout.jsx`: shared login/signup layout and campus illustration.
- `src/components/auth/AuthForm.jsx`: shared auth form and validation.
- `src/components/`: reusable password field and book icon.
- `src/config/`: shared product configuration.

New pages can use their own layouts; the auth layout is only used by the login and signup pages. Navigation supports direct URLs and browser back/forward without adding a routing dependency.

## Profile setup preview

Sign up with a name, a Cal Poly email, and matching passwords to open profile setup. Add at least one class using a subject and four-digit number, such as `CSC 2001`; lowercase and missing spaces are normalized. Duplicate codes are rejected. This checks formatting only, not catalog membership. Major, bio, profile picture, and year (First through Fifth+) are optional. Finish profile displays a completion notice; discovery is not connected yet. Profile data and the local photo preview stay in memory and reset on refresh. Opening profile setup without a test signup links back to signup.
