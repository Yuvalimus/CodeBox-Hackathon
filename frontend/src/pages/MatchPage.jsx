import React, { useEffect, useState } from 'react';
import { request } from '../api.js';
import './MatchPage.css';

export default function MatchPage({ match, goTo }) {
  const [error, setError] = useState('');
  const [offline, setOffline] = useState(false);
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    if (!match) return;
    let active = true;
    setError(''); setOffline(false);
    request('/looking-now', 'DELETE').then(() => { if (active) setOffline(true); }).catch(error => { if (active) setError(error.message); });
    return () => { active = false; };
  }, [match, attempt]);
  useEffect(() => {
    if (!offline || !match) return;
    const timer = setTimeout(() => goTo('/chat'), 2400);
    return () => clearTimeout(timer);
  }, [offline, match, goTo]);
  if (!match) return <main className="match-page"><h1>No new match to show.</h1><button onClick={() => goTo('/home')}>Back to home</button></main>;
  return <main className="match-page" aria-labelledby="match-title"><div className="match-burst" aria-hidden="true"><span>✦</span><div className="match-orb">You</div><div className="match-orb">Buddy</div><span>✳</span></div><div className="eyebrow">A SHARED YES. A NEW STUDY BUDDY.</div><h1 id="match-title">You’re a match.</h1><p>{match.name ? `You and ${match.name} both chose to study together.` : 'You both chose to study together.'}</p><p role="status">{offline ? 'You’re offline now. Opening your chat…' : 'Ending your looking status…'}</p>{error && <div role="alert"><p>{error}</p><button onClick={() => setAttempt(value => value + 1)}>Retry going offline</button></div>}<button className="home-primary" disabled={!offline} onClick={() => goTo('/chat')}>Open chat ↗</button></main>;
}
