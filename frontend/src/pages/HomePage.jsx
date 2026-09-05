import React, { useEffect, useRef, useState } from 'react';
import DiscoveryDeck from '../components/DiscoveryDeck.jsx';
import BookIcon from '../components/BookIcon.jsx';
import { PRODUCT_NAME } from '../config/brand.js';
import { AVATARS } from '../config/avatars.js';
import './HomePage.css';

export default function HomePage({ profile, navigate }) {
  const [choosing, setChoosing] = useState(false);
  const [selected, setSelected] = useState([]);
  const [location, setLocation] = useState('');
  const [session, setSession] = useState(null);
  const [error, setError] = useState('');
  const [photo, setPhoto] = useState('');
  const heading = useRef(null);
  useEffect(() => {
    if (!profile?.photo) { setPhoto(''); return; }
    const url = URL.createObjectURL(profile.photo);
    setPhoto(url);
    return () => URL.revokeObjectURL(url);
  }, [profile?.photo]);
  useEffect(() => { if (choosing) heading.current?.focus(); }, [choosing]);

  if (!profile) return <main className="home-missing"><h1>Your study circle starts here.</h1><p>Sign up to try the homepage. Preview data resets on refresh.</p><a href="/signup" onClick={navigate}>Go to signup →</a></main>;

  function start(event) {
    event.preventDefault();
    if (!selected.length) { setError('Choose at least one class to study.'); return; }
    setSession({ classes: [...selected], location: location.trim() || 'Kennedy Library' });
    setChoosing(false);
    setError('');
  }

  return <div className="buddy-home">
    <header className="home-header"><a className="brand" href="/home" onClick={navigate}><span className="brand-icon"><BookIcon /></span>{PRODUCT_NAME}.</a><a className="home-profile-link" href="/profile" onClick={navigate}><span className="home-avatar" style={{ background: AVATARS.find((avatar) => avatar.id === profile.avatar)?.color || AVATARS[0].color }}>{photo && <img src={photo} alt="" />}</span>My profile <span aria-hidden="true">↗</span></a></header>
    <main className="home-main">
      <div className="eyebrow">A LITTLE COMPANY. A LOT MORE POSSIBILITY.</div>
      <h1>Hi, {profile.name}.<br /><span>What will you figure out today?</span></h1>
      <p className="home-intro">Your classes, your pace. Let’s make your next study session a little less solo.</p>
      {!session && <section className="home-find"><div><span className="eyebrow">GOOD COMPANY STARTS HERE</span><h2>Bring a question.<br />Find a buddy.</h2><p>Choose what you’re working on and where you’d like to study.</p><button className="home-primary" onClick={() => { setChoosing(true); setError(''); }}>Find a buddy <span aria-hidden="true">↗</span></button></div><div className="home-art" aria-hidden="true"><BookIcon /><span>same class.<br /><em>new perspective.</em></span><b>✳</b></div></section>}
      {choosing && <section className="home-session" aria-labelledby="session-title"><h2 id="session-title" ref={heading} tabIndex={-1}>What are we studying?</h2><p>Pick one or more of your current classes for this session.</p><form onSubmit={start}><fieldset><legend>Classes for this session (required)</legend><div className="home-checks">{profile.classes.map((course) => <label key={course}><input type="checkbox" checked={selected.includes(course)} onChange={(event) => { setSelected(event.target.checked ? [...selected, course] : selected.filter((item) => item !== course)); setError(''); }} />{course}</label>)}</div></fieldset>{!profile.classes.length && <p>Add a class in <a href="/profile" onClick={navigate}>your profile</a> first.</p>}{error && <p className="error" role="alert">{error}</p>}<label htmlFor="study-location">Study location <span>(optional)</span></label><input id="study-location" value={location} onChange={(event) => setLocation(event.target.value)} placeholder="Kennedy Library" aria-describedby="location-hint" /><p id="location-hint" className="hint">Leave this blank and we’ll use Kennedy Library.</p><div className="home-form-actions"><button className="home-primary" type="submit">Start looking <span aria-hidden="true">↗</span></button><button type="button" className="home-secondary" onClick={() => setChoosing(false)}>Cancel</button></div></form></section>}
      {session && <DiscoveryDeck session={session} onEnd={() => { setSession(null); setSelected([]); setLocation(''); }} />}
      <section className="home-classes"><div><h2>Your current classes</h2><a href="/profile" onClick={navigate}>Edit profile ↗</a></div><ul>{profile.classes.map((course) => <li key={course}>{course}</li>)}</ul><p>Pick any of these when you’re ready to study. Your session choices won’t change this list.</p></section>
      <p className="home-preview">Frontend preview · Profile and session data reset on refresh.</p>
    </main>
  </div>;
}
