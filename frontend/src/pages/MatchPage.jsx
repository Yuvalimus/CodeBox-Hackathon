import React, { useEffect } from 'react';
import './MatchPage.css';

export default function MatchPage({ match, goTo }) {
  useEffect(() => {
    if (!match) return;
    const timer = setTimeout(() => goTo('/chat'), 2400);
    return () => clearTimeout(timer);
  }, [match, goTo]);
  if (!match) return <main className="match-page"><h1>No new match to show.</h1><button onClick={() => goTo('/home')}>Back to home</button></main>;
  return <main className="match-page"><h1>You’re a match.</h1><p role="status">Opening your chat…</p><button className="home-primary" onClick={() => goTo('/chat')}>Open chat ↗</button></main>;
}
