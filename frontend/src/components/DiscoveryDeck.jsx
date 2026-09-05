import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { getMockCandidates } from '../data/mockDiscovery.js';
import { AVATARS } from '../config/avatars.js';
import './DiscoveryDeck.css';

export default function DiscoveryDeck({ session, onEnd }) {
  const candidates = useMemo(() => getMockCandidates(session), [session]);
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

  const decide = useCallback((direction) => {
    if (!candidate || lock.current) return;
    lock.current = true;
    if (direction === 'right') {
      setRequests((previous) => [...previous, { id: candidate.id, name: candidate.name }]);
      setMessage(`Test request to ${candidate.name} is pending. This is not an accepted match.`);
    } else setMessage(`Passed on ${candidate.name}.`);
    setIndex((previous) => previous + 1);
    setDrag(0);
    gesture.current = null;
    timer.current = setTimeout(() => { lock.current = false; }, 300);
  }, [candidate]);

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
    <div className="discovery-heading"><div><div className="eyebrow">FIND YOUR STUDY PEOPLE</div><h2>A shared class. A fresh face.</h2></div><button className="home-secondary" onClick={onEnd}>Stop looking</button></div>
    <p className="discovery-session">{session.classes.join(' · ')} <span>at {session.location}</span></p>
    <p className="discovery-demo">Demo only · All profiles and online statuses are fictional. Requests stay in this preview.</p>
    <div className="discovery-layout"><div className="discovery-main">
      {candidate ? <>
        <div className="discovery-progress">{index + 1} of {candidates.length} test profiles · Shared classes first</div>
        <article className="candidate-card" style={{ transform: `translateX(${Math.max(-130, Math.min(130, drag))}px) rotate(${drag / 35}deg)` }} onPointerDown={pointerDown} onPointerMove={pointerMove} onPointerUp={pointerUp} onPointerCancel={() => { gesture.current = null; setDrag(0); }}>
          <div className="candidate-color" style={{ background: AVATARS.find((avatar) => avatar.id === candidate.avatar).color }}><span className="candidate-online">● Online · test profile</span><span className="candidate-monogram" aria-hidden="true">{candidate.name[0]}</span><span className="candidate-decoration" aria-hidden="true">✳</span>{Math.abs(drag) > 35 && <span className="candidate-swipe-hint">{drag > 0 ? 'REQUEST ↗' : '← PASS'}</span>}</div>
          <div className="candidate-content"><h3>{candidate.name}</h3>{(candidate.major || candidate.year) && <p className="candidate-detail">{[candidate.major, candidate.year && `${candidate.year} year`].filter(Boolean).join(' · ')}</p>}<div className="candidate-common"><span>Classes in common for this session</span><ul>{candidate.classes.map((course) => <li key={course}>{course}</li>)}</ul></div>{candidate.bio && <p className="candidate-bio">{candidate.bio}</p>}{candidate.location && <p className="candidate-location">Study spot · {candidate.location}</p>}</div>
        </article>
        <div className="discovery-actions"><button className="discovery-pass" onClick={() => decide('left')}>← Pass <kbd>A / ←</kbd></button><button className="discovery-request" onClick={() => decide('right')}>Request to match ↗ <kbd>D / →</kbd></button></div>
        <p className="discovery-help">Swipe the card, use the buttons, or press A / D or ← / →. Keyboard shortcuts pause while you’re using a form or button.</p>
      </> : <div className="discovery-empty"><span aria-hidden="true">✳</span><h3>You’ve seen this test deck.</h3><p>No more test profiles in this session. Your requests are still pending below.</p><button className="home-secondary" onClick={onEnd}>Back to home</button></div>}
      <p className="discovery-feedback" role="status" aria-live="polite">{message}</p>
    </div><aside className="discovery-pending"><h3>Outgoing requests <span>{requests.length}</span></h3><p>A right swipe requests a match. Chat opens only after an accepted match.</p>{requests.length ? <ul>{requests.map((request) => <li key={request.id}><strong>{request.name}</strong><span>Pending · test request</span></li>)}</ul> : <p className="discovery-no-requests">No requests yet. See someone you’d study with?</p>}</aside></div>
  </section>;
}
