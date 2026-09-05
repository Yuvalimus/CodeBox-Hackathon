import React from 'react';
import './AuthLayout.css';

export default function AuthLayout({ children, label }) {
  return (
    <main className="auth-layout auth-layout-centered">
      <section className="form-panel" aria-label={label}>{children}</section>
    </main>
  );
}
