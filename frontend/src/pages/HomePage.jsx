import { request } from '../api.js';
import AvatarArt from '../components/AvatarArt.jsx';
import React, { useEffect, useRef, useState } from 'react';
import DiscoveryDeck from '../components/DiscoveryDeck.jsx';
import BookIcon from '../components/BookIcon.jsx';
import { PRODUCT_NAME } from '../config/brand.js';
import { AVATARS } from '../config/avatars.js';
import './HomePage.css';

export default function HomePage({ profile, navigate }) {
  const [testDeck, setTestDeck] = useState(false);
  const [busy, setBusy] = useState(false);
  const [choosing, setChoosing] = useState(false);
  const [selected, setSelected] = useState([]);
  const [location, setLocation] = useState('');
  const [session, setSession] = useState(null);
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

  if (!profile) return <main className="home-missing"><h1>Your study circle starts here.</h1><p>Log in to open your homepage.</p><a href="/login" onClick={navigate}>Go to login →</a></main>;

  async function start(event) {
    event.preventDefault();
    if (busy) return;
    if (!selected.length) { setError('Choose at least one class to study.'); return; }
    setBusy(true); setError('');
    try {
      if (!testDeck) {
      await request('/me', 'PATCH', { studying: selected, preferredStudyLocations: [location.trim() || 'Kennedy Library'] });
      await request('/looking-now', 'PUT', { subjects: selected });
      }
      setSession({ testDeck, classes: [...selected], location: location.trim() || 'Kennedy Library' });
    } catch (error) { setError(error.message); return; } finally { setBusy(false); }
    setChoosing(false);
    setError('');
  }

  return <div className="buddy-home">
    <header className="home-header"><a className="brand" href="/home" onClick={navigate}><span className="brand-icon"><BookIcon /></span>{PRODUCT_NAME}.</a><a className="home-profile-link" href="/profile" onClick={navigate}><span className="home-avatar" style={{ background: AVATARS.find((avatar) => avatar.id === profile.avatar)?.color || AVATARS[0].color }}>{photo ? <img src={photo} alt="" /> : <AvatarArt avatar={profile.avatar} />}</span>My profile <span aria-hidden="true">↗</span></a></header>
    <main className={session ? "home-main" : "home-launch-stage"}>
      {!session && !choosing && <button ref={launchButton} className="home-launch-button" onClick={() => { setChoosing(true); setError(''); }}><span>Find a study buddy</span></button>}
      {choosing && <section className="home-session home-session-reveal" aria-labelledby="session-title"><h2 id="session-title" ref={heading} tabIndex={-1}>What are we studying?</h2><form onSubmit={start}><fieldset><legend>Classes to study</legend><div className="home-checks">{profile.classes.map((course) => <label key={course}><input type="checkbox" checked={selected.includes(course)} onChange={(event) => { setSelected(event.target.checked ? [...selected, course] : selected.filter((item) => item !== course)); setError(''); }} />{course}</label>)}</div></fieldset>{!profile.classes.length && <p>Add a class in <a href="/profile" onClick={navigate}>your profile</a> first.</p>}{error && <p className="error" role="alert">{error}</p>}<label htmlFor="study-location">Study location <span>(optional)</span></label><input id="study-location" maxLength={100} value={location} onChange={(event) => setLocation(event.target.value)} placeholder="Kennedy Library" aria-describedby="location-hint" /><p id="location-hint" className="hint">Leave this blank and we’ll use Kennedy Library.</p><label className="home-test-option"><input type="checkbox" checked={testDeck} disabled={busy} onChange={event => setTestDeck(event.target.checked)} /> Use looping test profiles</label><div className="home-form-actions"><button className="home-primary" type="submit" disabled={busy}>{busy ? 'Starting...' : 'Start looking'} <span aria-hidden="true">↗</span></button><button type="button" className="home-secondary" disabled={busy} onClick={() => { setChoosing(false); requestAnimationFrame(() => launchButton.current?.focus()); }}>Cancel</button></div></form></section>}
      {session && <DiscoveryDeck session={session} onEnd={async () => { if (!session.testDeck) await request('/looking-now', 'DELETE'); setSession(null); setSelected([]); setLocation(''); }} />}
    </main>
  </div>;
}
