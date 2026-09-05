import React from 'react';

export default function NotFoundPage({ navigate }) {
  return (
    <main className="form-panel">
      <h1>Page not found.</h1>
      <p>This page doesn't exist yet.</p>
      <a href="/login" onClick={navigate}>Go to login</a>
    </main>
  );
}
