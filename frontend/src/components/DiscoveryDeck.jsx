import React, { useCallback, useEffect, useRef, useState } from 'react';
import { request, fromUser } from '../api.js';
import { AVATARS } from '../config/avatars.js';
import './DiscoveryDeck.css';

export default function DiscoveryDeck({ session, onEnd }) {
  const [candidates, setCandidates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [apiError, setApiError] = useState('');
  const [busy, setBusy] = useState(false);
  const [reload, setReload] = useState(0);
  useEffect(() => {
    let active = true;
    setLoading(true); setApiError('');
    Promise.all([request('/recommendations?limit=50'), request('/looking-now')]).then(([recs, presence]) => {
      if (!active) return;
      const online = new Map(presence.users.filter(user => Date.parse(user.expiresAt) > Date.now()).map(user => [user.id, user]));
      setCandidates(recs.recommendations.filter(user => online.has(user.id)).map(user => ({ ...fromUser(user), classes: user.classes.filter(course => session.classes.includes(course)), location: user.preferredStudyLocations?.[0] || '', expiresAt: online.get(user.id).expiresAt })));
      setIndex(0);
    }).catch(error => { if (active) setApiError(error.message); }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [session, reload]);
  async function end() { if (busy) return; setBusy(true); try { await onEnd(); } catch(error) { setApiError(error.message); } finally { setBusy(false); } }
  const [index, setIndex] = useState(0);
  const [requests, setRequests] = useState([]);
  const [message, setMessage] = useState('');
  const [drag, setDrag] = useState(0);
  const gesture = useRef(null);
  const lock = useRef(false);
  const timer = useRef(null);
  const region = useRef(null);
  const candidate = candidates[index];

  useEffect(() => { region.current?.focus(); return () => clearTimeout(timer.current); }, []);

  const decide = useCallback(async (direction) => {
    if (!candidate || lock.current || loading || busy || apiError) return;
    if (Date.parse(candidate.expiresAt) <= Date.now()) { setMessage('This student is no longer online.'); setIndex(previous => previous + 1); return; }
    lock.current = true; setBusy(true); setApiError('');
    try {
    const result = await request(`/recommendations/${candidate.id}/${direction === 'right' ? 'accept' : 'reject'}`, 'POST', {});
    if (direction === 'right') {
      setRequests((previous) => [...previous, { id: candidate.id, name: candidate.name, matched: result.matched }]);
      setMessage(result.matched ? `You matched with ${candidate.name}!` : `Choice saved. Keep browsing.`);
      if (result.matched && result.chat?.id) window.dispatchEvent(new CustomEvent('mutual-match', { detail: { chatId: result.chat.id, name: candidate.name } }));
      window.dispatchEvent(new Event('matches-updated'));
    } else setMessage(`Passed on ${candidate.name}.`);
    setIndex((previous) => previous + 1);
    setDrag(0);
    gesture.current = null;
    } catch(error) { setApiError(error.message); } finally { lock.current = false; setBusy(false); }
  }, [candidate, loading, busy, apiError]);

  useEffect(() => {
    function keydown(event) {
      if (event.repeat || event.ctrlKey || event.metaKey || event.altKey || event.shiftKey) return;
      if (event.target.closest('input, textarea, select, button, a, [contenteditable="true"]')) return;
      const key = event.key.toLowerCase();
      if (['a', 'arrowleft', 'd', 'arrowright'].includes(key)) {
        event.preventDefault();
        decide(key === 'a' || key === 'arrowleft' ? 'left' : 'right');
      }
    }
    window.addEventListener('keydown', keydown);
    return () => window.removeEventListener('keydown', keydown);
  }, [decide]);

  function pointerDown(event) {
    if (!event.isPrimary || event.button !== 0 || lock.current) return;
    gesture.current = { id: event.pointerId, x: event.clientX, y: event.clientY };
    event.currentTarget.setPointerCapture(event.pointerId);
  }
  function pointerMove(event) {
    if (gesture.current?.id !== event.pointerId) return;
    setDrag(event.clientX - gesture.current.x);
  }
  function pointerUp(event) {
    const start = gesture.current;
    gesture.current = null;
    setDrag(0);
    if (!start || start.id !== event.pointerId) return;
    const dx = event.clientX - start.x;
    const dy = event.clientY - start.y;
    if (Math.abs(dx) > 75 && Math.abs(dx) > Math.abs(dy) * 1.2) decide(dx > 0 ? 'right' : 'left');
  }

  return <section className="discovery" ref={region} tabIndex={-1} aria-label="Study buddy discovery">
    <div className="discovery-heading"><div><div className="eyebrow">FIND YOUR STUDY PEOPLE</div><h2>A shared class. A fresh face.</h2></div><button className="home-secondary" onClick={end} disabled={busy}>Stop looking</button></div>
    <p className="discovery-session">{session.classes.join(' · ')} <span>at {session.location}</span></p>
    <p className="discovery-demo">Only currently online students are shown. Requests become matches after mutual acceptance.</p>
    {apiError && <p className="error" role="alert">{apiError} <button disabled={busy} onClick={() => setReload(value => value + 1)}>Retry loading</button></p>}{loading && <p role="status">Loading online study buddies...</p>}<div className="discovery-layout"><div className="discovery-main">
      {!loading && candidate ? <>
        <div className="discovery-progress">{index + 1} of {candidates.length} profiles · Recommended for you</div>
        <article className="candidate-card" style={{ transform: `translateX(${Math.max(-130, Math.min(130, drag))}px) rotate(${drag / 35}deg)` }} onPointerDown={pointerDown} onPointerMove={pointerMove} onPointerUp={pointerUp} onPointerCancel={() => { gesture.current = null; setDrag(0); }}>
          <div className="candidate-color" style={{ background: (AVATARS.find((avatar) => avatar.id === candidate.avatar) || AVATARS[0]).color }}><span className="candidate-online">● Online · test profile</span><span className="candidate-monogram" aria-hidden="true">{candidate.name[0]}</span><span className="candidate-decoration" aria-hidden="true">✳</span>{Math.abs(drag) > 35 && <span className="candidate-swipe-hint">{drag > 0 ? 'REQUEST ↗' : '← PASS'}</span>}</div>
          <div className="candidate-content"><h3>{candidate.name}</h3>{(candidate.major || candidate.year) && <p className="candidate-detail">{[candidate.major, candidate.year && `${candidate.year} year`].filter(Boolean).join(' · ')}</p>}<div className="candidate-common"><span>Classes in common for this session</span><ul>{candidate.classes.map((course) => <li key={course}>{course}</li>)}</ul></div>{candidate.bio && <p className="candidate-bio">{candidate.bio}</p>}{candidate.location && <p className="candidate-location">Study spot · {candidate.location}</p>}</div>
        </article>
        <div className="discovery-actions"><button className="discovery-pass" disabled={busy} onClick={() => decide('left')}>← Pass <kbd>A / ←</kbd></button><button className="discovery-request" disabled={busy} onClick={() => decide('right')}>Request to match ↗ <kbd>D / →</kbd></button></div>
        <p className="discovery-help">Swipe the card, use the buttons, or press A / D or ← / →. Keyboard shortcuts pause while you’re using a form or button.</p>
      </> : !loading && <div className="discovery-empty"><span aria-hidden="true">✳</span><h3>You’ve seen this test deck.</h3><p>Try refreshing later to see more study buddies.</p><button className="home-secondary" onClick={end} disabled={busy}>Stop looking</button></div>}
      <p className="discovery-feedback" role="status" aria-live="polite">{message}</p>
    </div><aside className="discovery-pending"><h3>Outgoing requests <span>{requests.length}</span></h3><p>A right swipe requests a match. Chat opens only after an accepted match.</p>{requests.length ? <ul>{requests.map((request) => <li key={request.id}><strong>{request.name}</strong><span>Pending · test request</span></li>)}</ul> : <p className="discovery-no-requests">No requests yet. See someone you’d study with?</p>}</aside></div>
  </section>;
}
