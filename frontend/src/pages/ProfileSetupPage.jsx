import React, { useEffect, useRef, useState } from 'react';
import BookIcon from '../components/BookIcon.jsx';
import { PRODUCT_NAME } from '../config/brand.js';
import './ProfileSetupPage.css';

export default function ProfileSetupPage({ profile, onProfileChange, navigate }) {
  const [course, setCourse] = useState('');
  const [error, setError] = useState('');
  const [photoError, setPhotoError] = useState('');
  const [photoUrl, setPhotoUrl] = useState('');
  const [complete, setComplete] = useState(false);
  const courseInput = useRef(null);
  const photoInput = useRef(null);

  useEffect(() => {
    if (!profile?.photo) { setPhotoUrl(''); return; }
    const url = URL.createObjectURL(profile.photo);
    setPhotoUrl(url);
    return () => URL.revokeObjectURL(url);
  }, [profile?.photo]);

  if (!profile) return <main className="profile-setup profile-missing">
    <BookIcon /><h1>Let’s start with your name.</h1>
    <p>Sign up to try profile setup. This preview resets when you refresh.</p>
    <a href="/signup" onClick={navigate}>Go to signup →</a>
  </main>;

  function update(field, value) {
    onProfileChange((previous) => ({ ...previous, [field]: value }));
    setComplete(false);
  }

  function addCourse() {
    const match = course.trim().toUpperCase().match(/^([A-Z]{2,4})\s*(\d{4})$/);
    if (!match) { setError('Use a subject and four-digit course number, like CSC 2001.'); courseInput.current?.focus(); return false; }
    const normalized = `${match[1]} ${match[2]}`;
    if (profile.classes.includes(normalized)) { setError('You’ve already added that class.'); courseInput.current?.focus(); return false; }
    update('classes', [...profile.classes, normalized]);
    setCourse('');
    setError('');
    courseInput.current?.focus();
    return true;
  }

  function finish(event) {
    event.preventDefault();
    if (course.trim()) { if (!addCourse()) return; }
    else if (!profile.classes.length) { setError('Add at least one current class to continue.'); courseInput.current?.focus(); return; }
    setComplete(true);
  }

  function selectPhoto(event) {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type) || file.size > 5 * 1024 * 1024) {
      setPhotoError('Choose a JPG, PNG, or WebP image under 5 MB.'); return;
    }
    setPhotoError('');
    update('photo', file);
  }

  return <div className="profile-setup">
    <header className="profile-header"><a className="brand" href="/" onClick={navigate}><span className="brand-icon"><BookIcon /></span>{PRODUCT_NAME}.</a><span>YOUR STUDY CIRCLE STARTS HERE</span></header>
    <main className="profile-grid">
      <aside className="profile-intro"><div className="eyebrow">STEP 2 OF 2 · YOUR PROFILE</div><h1>Hey, {profile.name}.<br /><span>Make yourself<br />at home.</span></h1><p>Start with your classes. The rest is a little space to tell your future study buddies about you.</p><div className="profile-tip"><BookIcon /><h2>A class in common.<br />A place to start.</h2><p>Add the classes you’re taking now. Later, you’ll pick which ones you want to study in each session.</p></div><p className="hint">Testing preview · No account is created. Your profile stays in memory until you refresh.</p></aside>
      <form className="profile-card" onSubmit={finish} noValidate>
        <section aria-labelledby="classes-title"><div className="profile-section-title"><h2 id="classes-title">Your current classes</h2><span>Required</span></div><p className="profile-description">Add at least one class to help you find common ground.</p><label htmlFor="course">Class code</label><div className="profile-class-entry"><input ref={courseInput} id="course" value={course} placeholder="CSC 2001" autoCapitalize="characters" spellCheck="false" aria-invalid={Boolean(error)} aria-describedby={error ? 'course-error' : 'course-hint'} onChange={(event) => { setCourse(event.target.value); setError(''); setComplete(false); }} onKeyDown={(event) => { if (event.key === 'Enter') { event.preventDefault(); addCourse(); } }} /><button type="button" onClick={addCourse}>Add class +</button></div><p className="hint" id="course-hint">Use the format CSC 2001. Class codes aren’t checked against a course catalog yet.</p>{error && <p className="error" id="course-error" role="alert">{error}</p>}<ul className="profile-class-list" aria-label="Added classes">{profile.classes.map((item) => <li key={item}>{item}<button type="button" aria-label={`Remove ${item}`} onClick={() => { update('classes', profile.classes.filter((value) => value !== item)); setError(''); }}>×</button></li>)}</ul></section>
        <section className="profile-optional" aria-labelledby="optional-title"><div className="profile-section-title"><h2 id="optional-title">A little about you</h2><span>All optional</span></div><p className="profile-description">Share as much or as little as you like.</p>
          <div className="profile-photo-row"><div className="profile-avatar">{photoUrl ? <img src={photoUrl} alt="Your profile preview" onError={() => { setPhotoError('This image could not be opened. Try another image.'); update('photo', null); }} /> : <span>{profile.name.slice(0, 1).toUpperCase()}</span>}</div><div><label htmlFor="photo">Profile picture <span>(optional)</span></label><input ref={photoInput} id="photo" type="file" accept="image/jpeg,image/png,image/webp" onChange={selectPhoto} aria-describedby={photoError ? 'photo-error' : 'photo-hint'} /><p className="hint" id="photo-hint">JPG, PNG, or WebP · Up to 5 MB · Local preview only</p>{profile.photo && <button className="profile-remove" type="button" onClick={() => { update('photo', null); setPhotoError(''); }}>Remove photo</button>}</div></div>{photoError && <p id="photo-error" className="error" role="alert">{photoError}</p>}
          <div className="profile-details"><div className="field"><label htmlFor="major">Major <span>(optional)</span></label><input id="major" value={profile.major} placeholder="e.g. Computer Science" onChange={(event) => update('major', event.target.value)} /></div><div className="field"><label htmlFor="year">Year <span>(optional)</span></label><select id="year" value={profile.year} onChange={(event) => update('year', event.target.value)}><option value="">Select your year</option>{['First', 'Second', 'Third', 'Fourth', 'Fifth+'].map((year) => <option key={year} value={year}>{year}</option>)}</select></div></div>
          <div className="field"><label htmlFor="bio">Bio <span>(optional)</span></label><textarea id="bio" rows="4" maxLength={500} value={profile.bio} placeholder="A bit about you, how you study, or what you’re excited to learn…" onChange={(event) => update('bio', event.target.value)} aria-describedby="bio-hint" /><p className="hint" id="bio-hint">{profile.bio.length}/500 characters</p></div>
        </section>
        <button className="submit" type="submit">Finish profile <span aria-hidden="true">↗</span></button>
        {complete && <p className="notice" role="status">Your test profile is ready, {profile.name}! Your details are kept for this preview only. Study-buddy discovery isn’t connected yet.</p>}
      </form>
    </main>
  </div>;
}
