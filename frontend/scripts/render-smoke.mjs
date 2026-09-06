import assert from 'node:assert/strict';
import { createServer } from 'vite';
import React from 'react';
import { renderToString } from 'react-dom/server';

globalThis.sessionStorage = { getItem: () => null };
globalThis.window = { location: { pathname: '/', origin: 'http://localhost:5173' } };
const server = await createServer({ server: { middlewareMode: true }, appType: 'custom' });
const profile = { id: 1, name: 'Alex', username: 'Alex', classes: ['CSC 2001'], studying: [], major: '', bio: '', year: '', avatar: 'sage' };
try {
  for (const [path, props] of [
    ['/src/App.jsx', {}],
    ['/src/pages/HomePage.jsx', { profile, session: null }],
    ['/src/pages/HomePage.jsx', { profile, session: { classes: profile.classes } }],
    ['/src/pages/ProfileSetupPage.jsx', { profile, editing: true }],
    ['/src/pages/ChatPage.jsx', { profile }],
    ['/src/pages/LoginPage.jsx', {}],
    ['/src/pages/SignupPage.jsx', {}],
  ]) {
    const { default: Page } = await server.ssrLoadModule(path);
    assert.ok(renderToString(React.createElement(Page, props)).length > 0);
    console.log(`Rendered ${path}`);
  }
} finally { await server.close(); }
