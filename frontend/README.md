# Frontend

React login and signup screens for Cal Poly students.

```sh
npm install
npm run dev
```

Open `/login` or `/signup`. The root path also shows login. Build with `npm run build`; serve the build with `npm run preview`. Production hosting should fall back to `index.html` for client routes.

Only email and password are supported. Email validation accepts the exact `calpoly.edu` domain, case-insensitively, with surrounding whitespace ignored. Signup requires a matching password confirmation and a provisional minimum of eight characters.

API integration is intentionally unimplemented. Valid submissions show a preview notice and do not authenticate, create an account, verify email ownership, or persist credentials. The integration placeholder is in `src/main.jsx` inside `AuthForm.submit`. Backend validation and the final password policy must be coordinated when connecting authentication.

The temporary product label is configured in `PRODUCT_NAME`; shared color tokens are in `src/styles.css`.
