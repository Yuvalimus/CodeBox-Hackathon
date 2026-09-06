import React, { useCallback, useEffect, useRef, useState } from 'react';
import { request, fromUser } from '../api.js';
import AvatarArt from './AvatarArt.jsx';
import './DiscoveryDeck.css';

export default function DiscoveryDeck({ session, onEnd, active = true }) {
  const [candidates, setCandidates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [apiError, setApiError] = useState('');
  const [busy, setBusy] = useState(false);
  const [reload, setReload] = useState(0);
  const decisionVersion = useRef(0);
  const loadingRef = useRef(false);
  useEffect(() => {
    if (!active) return;
    let mounted = true;
    let refreshTimer;
    setLoading(true); setApiError('');
    async function refresh() {
      const version = decisionVersion.current;
      if (!lock.current && !gesture.current && !loadingRef.current) {
        loadingRef.current = true;
        try {
          const recs = await request('/recommendations?limit=50');
          if (mounted && !lock.current && !gesture.current && version === decisionVersion.current) {
            setCandidates(recs.recommendations.map(user => ({ ...fromUser(user), classes: (user.studying || []).filter(course => session.classes.includes(course)), location: user.preferredStudyLocations?.[0] || '' })));
            setIndex(0); setApiError('');
          }
        } catch(error) { if (mounted) setApiError(error.message); }
        finally { loadingRef.current = false; if (mounted) setLoading(false); }
      }
      if (mounted) refreshTimer = setTimeout(refresh, 5000);
    }
    refresh();
    return () => { mounted = false; clearTimeout(refreshTimer); };
  }, [session, reload, active]);
  const stopping = useRef(false);
  async function end() {
    if (busy || lock.current || stopping.current) return;
    stopping.current = true;
    setBusy(true);
    try { await onEnd(); }
    catch (error) { setApiError(error.message); }
    finally { stopping.current = false; setBusy(false); }
  }
  const [index, setIndex] = useState(0);
  const [message, setMessage] = useState('');
  const [exitDirection, setExitDirection] = useState(null);
  const [failedPicture, setFailedPicture] = useState(null);
  const [drag, setDrag] = useState(0);
  const gesture = useRef(null);
  const dragFrame = useRef(null);
  const pendingDrag = useRef(0);
  const lock = useRef(false);
  const timer = useRef(null);
  const region = useRef(null);
  const candidate = candidates[index];
  const lastSwipeAt = useRef(Date.now());
  const idleAction = useRef(null);
  idleAction.current = end;
  useEffect(() => {
    let retryAfter = 0;
    // Keep counting across profile/chat navigation. Polling and pointer movement
    // are not swipes. Wall-clock checks also catch up after browser throttling.
    function checkIdle() {
      const now = Date.now();
      if (now - lastSwipeAt.current < 60000 || now < retryAfter) return;
      retryAfter = now + 10000;
      void idleAction.current();
    }
    const interval = setInterval(checkIdle, 1000);
    window.addEventListener('focus', checkIdle);
    document.addEventListener('visibilitychange', checkIdle);
    return () => {
      clearInterval(interval);
      window.removeEventListener('focus', checkIdle);
      document.removeEventListener('visibilitychange', checkIdle);
    };
  }, []);

  useEffect(() => { region.current?.focus(); return () => { clearTimeout(timer.current); cancelAnimationFrame(dragFrame.current); }; }, []);

  const decide = useCallback(async (direction) => {
    if (!candidate || lock.current || loading || busy || apiError) return;
    if (Date.parse(candidate.expiresAt) <= Date.now()) { setMessage('This student is no longer online.'); setIndex(previous => previous + 1); return; }
    lastSwipeAt.current = Date.now();
    decisionVersion.current += 1;
    lock.current = true; setBusy(true); setApiError('');
    try {
    setExitDirection(direction);
    const [result] = await Promise.all([
      request(`/recommendations/${candidate.id}/${direction === 'right' ? 'accept' : 'reject'}`, 'POST', {}),
      new Promise(resolve => setTimeout(resolve, window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 0 : 280)),
    ]);
    if (direction === 'right') {
      setMessage(result?.matched ? `You matched with ${candidate.name}!` : `Choice saved. Keep browsing.`);
      if (result?.matched && result.chat?.id) window.dispatchEvent(new CustomEvent('mutual-match', { detail: { chatId: result.chat.id, name: candidate.name } }));
      window.dispatchEvent(new Event('matches-updated'));
    } else setMessage(`Passed on ${candidate.name}.`);
    setIndex(previous => previous + 1);
    setExitDirection(null);
    setDrag(0);
    gesture.current = null;
    } catch(error) { setApiError(error.message); setExitDirection(null); setDrag(0); gesture.current = null; } finally { lock.current = false; setBusy(false); }
  }, [candidate, loading, busy, apiError, candidates.length]);

  useEffect(() => {
    if (!active) return;
    function keydown(event) {
      if (event.repeat || event.ctrlKey || event.metaKey || event.altKey || event.shiftKey) return;
      if (event.target.closest('input, textarea, select, a, [contenteditable="true"]')) return;
      const key = event.key.toLowerCase();
      if (['a', 'arrowleft', 'd', 'arrowright'].includes(key)) {
        event.preventDefault();
        decide(key === 'a' || key === 'arrowleft' ? 'left' : 'right');
      }
    }
    window.addEventListener('keydown', keydown);
    return () => window.removeEventListener('keydown', keydown);
  }, [decide, active]);

  function cancelDrag() {
    cancelAnimationFrame(dragFrame.current);
    dragFrame.current = null;
    gesture.current = null;
    setDrag(0);
  }
  function pointerDown(event) {
    if (!event.isPrimary || event.button !== 0 || lock.current || busy || loading || apiError) return;
    gesture.current = { id: event.pointerId, x: event.clientX, y: event.clientY, axis: null };
  }
  function pointerMove(event) {
    const start = gesture.current;
    if (!start || start.id !== event.pointerId) return;
    const dx = event.clientX - start.x;
    const dy = event.clientY - start.y;
    if (!start.axis && Math.max(Math.abs(dx), Math.abs(dy)) > 8) {
      start.axis = Math.abs(dx) > Math.abs(dy) * 1.2 ? 'x' : 'y';
      if (start.axis === 'x') event.currentTarget.setPointerCapture(event.pointerId);
    }
    if (start.axis !== 'x') return;
    pendingDrag.current = dx;
    if (dragFrame.current === null) dragFrame.current = requestAnimationFrame(() => {
      setDrag(pendingDrag.current);
      dragFrame.current = null;
    });
  }
  function pointerUp(event) {
    const start = gesture.current;
    cancelAnimationFrame(dragFrame.current);
    dragFrame.current = null;
    gesture.current = null;
    if (!start || start.id !== event.pointerId) return;
    const dx = event.clientX - start.x;
    const threshold = Math.min(90, event.currentTarget.clientWidth * .23);
    if (start.axis === 'x' && Math.abs(dx) > threshold) {
      setDrag(dx);
      decide(dx > 0 ? 'right' : 'left');
    } else setDrag(0);
  }

  return <section className="discovery" ref={region} tabIndex={-1} aria-label="Study buddy discovery">
    <div className="discovery-stop"><button className="home-secondary" onClick={end} disabled={busy}>Stop looking</button></div>
    {apiError && <p className="error" role="alert">{apiError} <button disabled={busy} onClick={() => setReload(value => value + 1)}>Retry loading</button></p>}{loading && <p role="status">Loading online study buddies...</p>}<div className="discovery-layout"><div className="discovery-main">
      {!loading && candidate ? <>
        <article key={candidate.id} className={`candidate-card ${exitDirection ? `card-exit-${exitDirection}` : drag ? "card-dragging" : "card-enter"}`} style={{ "--swipe-start": `translate3d(${drag}px, 0, 0) rotate(${drag / 35}deg)`, transform: exitDirection ? undefined : `translate3d(${drag}px, 0, 0) rotate(${drag / 35}deg)` }} onPointerDown={pointerDown} onPointerMove={pointerMove} onPointerUp={pointerUp} onPointerCancel={cancelDrag} onLostPointerCapture={() => { if (gesture.current) cancelDrag(); }}>
          <div className="candidate-color"><div className="candidate-portrait">{candidate.pictureUrl && failedPicture !== candidate.id ? <img src={candidate.pictureUrl} alt={candidate.name} draggable={false} onError={() => setFailedPicture(candidate.id)} /> : <AvatarArt avatar={candidate.avatar} label={candidate.name + ' avatar'} />}</div><span className="candidate-online">Online</span></div>
          <div className="candidate-content"><h3>{candidate.name}</h3>{(candidate.major || candidate.year) && <p className="candidate-detail">{[candidate.major, candidate.year && `${candidate.year} year`].filter(Boolean).join(' · ')}</p>}{candidate.comments && <aside className="candidate-comments" aria-label={`${candidate.name}'s additional info`}><span aria-hidden="true">✦</span><div><strong>Session note</strong><p>{candidate.comments}</p></div></aside>}<div className="candidate-common"><span>Classes you both chose for this session</span><ul>{candidate.classes.map((course) => <li key={course}>{course}</li>)}</ul></div>{candidate.bio && <p className="candidate-bio">{candidate.bio}</p>}{candidate.location && <p className="candidate-location">Study spot · {candidate.location}</p>}</div>
        </article>
        <div className="discovery-actions"><button className="discovery-pass" disabled={busy} onClick={() => decide('left')}>← Pass <kbd>A / ←</kbd></button><button className="discovery-request" disabled={busy} onClick={() => decide('right')}>Request to match ↗ <kbd>D / →</kbd></button></div>
      </> : !loading && <div className="discovery-empty"><p>No profiles available right now.</p><button className="home-secondary" onClick={() => setReload(value => value + 1)}>Refresh</button></div>}
      <p className="discovery-announcement" role="status" aria-live="polite">{message}</p>
    </div></div>
  </section>;
}

