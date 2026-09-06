import { request } from '../api.js';
import { goOffline } from '../presence.js';
import { startStudySession } from '../studySession.js';
import AvatarArt from '../components/AvatarArt.jsx';
import React, { useEffect, useRef, useState } from 'react';
import DiscoveryDeck from '../components/DiscoveryDeck.jsx';
import BookIcon from '../components/BookIcon.jsx';
import BrandName from '../components/BrandName.jsx';
import { AVATARS } from '../config/avatars.js';
import './HomePage.css';

export default function HomePage({ profile, navigate, onLogout, loggingOut, logoutError, session, setSession, active = true, onPresenceError, unreadMatchCount = 0 }) {
  const [generating, setGenerating] = useState(false);
  const [generated, setGenerated] = useState(false);
  const [durationMinutes, setDurationMinutes] = useState(null);
  const [busy, setBusy] = useState(false);
  const [queueMode, setQueueMode] = useState(null);
  const choosing = Boolean(queueMode);
  const [selected, setSelected] = useState([]);
  const [location, setLocation] = useState('');
  const [additionalInfo, setAdditionalInfo] = useState('');

  const [error, setError] = useState('');
  const [photo, setPhoto] = useState('');
  const heading = useRef(null);
  const launchButton = useRef(null);
  useEffect(() => {
    if (!profile?.photo) { setPhoto(profile?.pictureUrl || ''); return; }
    const url = URL.createObjectURL(profile.photo);
    setPhoto(url);
    return () => URL.revokeObjectURL(url);
  }, [profile?.photo, profile?.pictureUrl]);
  useEffect(() => { if (choosing) heading.current?.focus(); }, [choosing]);
  useEffect(() => {
    if (active || session?.queueMode !== 'active') return;
    void goOffline().catch(error => onPresenceError?.(error.message));
    setSession(null);
  }, [active, session?.queueMode, setSession, onPresenceError]);

  if (!profile) return <main className="home-missing"><h1>Your study circle starts here.</h1><p>Log in to open your homepage.</p><a href="/login" onClick={navigate}>Go to login →</a></main>;

  async function generateProfiles() {
    if (generating || generated) return;
    setGenerating(true); setError('');
    try {
      await request('/test/profiles?count=100', 'POST');
      setGenerated(true);
    } catch (error) {
      setError(error.status === 404 ? 'Test generation is disabled on this backend. Enable TEST_DATA_ENABLED and restart the backend.' : error.message);
    } finally { setGenerating(false); }
  }

  async function start(event) {
    event.preventDefault();
    if (busy) return;
    if (selected.length > 20) { setError('Choose up to 20 classes.'); return; }
    if (!selected.length) { setError('Choose at least one class to study.'); return; }
    if (!durationMinutes) { setError('Choose how long you want to study.'); document.querySelector('input[name=study-duration]')?.focus(); return; }
    setBusy(true); setError('');
    try {
      await startStudySession({ classes: selected, location, comments: additionalInfo, durationMinutes, active: queueMode === 'active' });
      setSession({ queueMode, durationMinutes, durationIsMinimum: durationMinutes === 120, classes: [...selected], location: location.trim() || 'Kennedy Library' });
    } catch (error) { setError(error.message); return; } finally { setBusy(false); }
    setQueueMode(null);
    setError('');
  }

  return <div className="buddy-home">
    <header className="home-header"><a className="brand" href="/home" onClick={navigate}><span className="brand-icon"><BookIcon /></span><BrandName /></a><nav className="home-account-nav" aria-label="Account"><a className="matches-link" href="/chat" onClick={navigate}>Matches & chats{unreadMatchCount > 0 && <span className="match-count" aria-label={`${unreadMatchCount} unread ${unreadMatchCount === 1 ? 'match' : 'matches'}`}>{unreadMatchCount > 99 ? '99+' : unreadMatchCount}</span>}</a><a className="home-profile-link" href="/profile" onClick={navigate}><span className="home-avatar" style={{ background: AVATARS.find((avatar) => avatar.id === profile.avatar)?.color || AVATARS[0].color }}>{photo ? <img src={photo} alt="" /> : <AvatarArt avatar={profile.avatar} />}</span>My profile <span aria-hidden="true">↗</span></a><button className="home-secondary" disabled={loggingOut} onClick={onLogout}>{loggingOut ? 'Logging out...' : 'Log out'}</button></nav></header>{logoutError && <p className="error" role="alert">{logoutError}</p>}
    <main className={session ? "home-main" : "home-launch-stage"}>
      {!session && !choosing && <div className="queue-launches"><button ref={launchButton} className="home-launch-button" onClick={() => { setQueueMode('active'); setError(''); }}><span>Find study buddy Now</span></button><button className="home-launch-button offline-launch" onClick={() => { const classes = profile.studying?.length ? profile.studying : profile.classes; setSession({ queueMode: 'offline', durationMinutes: profile.studyDurationMinutes || 60, classes, location: profile.preferredStudyLocations?.[0] || 'Kennedy Library' }); setError(''); }}><span>Find study buddy Later</span></button></div>}
      {choosing && <section className="home-session home-session-reveal" aria-labelledby="session-title"><h2 id="session-title" ref={heading} tabIndex={-1}>{queueMode === 'active' ? 'Find active study buddies' : 'Browse offline study buddies'}</h2><p className="hint">{queueMode === 'active' ? 'You will be visible only while you keep looking.' : 'Browse people who opted in to offline discovery. They can match when they return.'}</p><form onSubmit={start}><fieldset><legend>Classes to study</legend><div className="home-checks">{profile.classes.map((course) => <label key={course}><input type="checkbox" checked={selected.includes(course)} onChange={(event) => { setSelected(event.target.checked ? [...selected, course] : selected.filter((item) => item !== course)); setError(''); }} />{course}</label>)}</div></fieldset><fieldset className="study-duration"><legend>How long are you studying?</legend><div className="duration-options">{[30, 60, 90, 120].map(minutes => <label key={minutes} className={durationMinutes === minutes ? "selected" : ""}><input type="radio" name="study-duration" value={minutes} required checked={durationMinutes === minutes} onChange={() => { setDurationMinutes(minutes); setError(''); }} /><span>{minutes === 120 ? '2+' : minutes / 60} {minutes === 60 ? 'hour' : 'hours'}</span></label>)}</div></fieldset>{error && <p className="error" role="alert">{error}</p>}<label htmlFor="study-location">Study location <span>(optional)</span></label><input id="study-location" maxLength={100} value={location} onChange={(event) => setLocation(event.target.value)} placeholder="Kennedy Library" /><label htmlFor="additional-info">Additional info <span>(optional)</span></label><textarea id="additional-info" rows={3} maxLength={500} value={additionalInfo} onChange={event => setAdditionalInfo(event.target.value)} placeholder="e.g. I’m reviewing for an hour before my exam." /><button className="home-secondary home-test-option" type="button" disabled={busy || generating || generated} onClick={generateProfiles}>{generating ? 'Generating profiles...' : generated ? '100 test profiles generated' : 'Generate 100 test profiles'}</button><div className="home-form-actions"><button className="home-primary" type="submit" disabled={busy || generating}>{busy ? 'Starting...' : 'Start browsing'} <span aria-hidden="true">↗</span></button><button type="button" className="home-secondary" disabled={busy} onClick={() => { setQueueMode(null); requestAnimationFrame(() => launchButton.current?.focus()); }}>Cancel</button></div></form></section>}
      {session && <DiscoveryDeck active={active} session={session} onEnd={async () => { if (session.queueMode === 'active') await goOffline(); setSession(null); setSelected([]); setDurationMinutes(null); setLocation(''); setAdditionalInfo(''); }} />}
    </main>
  </div>;
}
