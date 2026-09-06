import React, { useEffect, useRef, useState } from 'react';
import { AVATARS } from '../config/avatars.js';
import AvatarArt from '../components/AvatarArt.jsx';
import BookIcon from '../components/BookIcon.jsx';
import StudyTimePicker from '../components/StudyTimePicker.jsx';
import { PRODUCT_NAME } from '../config/brand.js';
import './ProfileSetupPage.css';
import './SetupFlow.css';

const DAYS = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
function formatStudyTime(slot) {
  const day = DAYS[Math.floor(slot / 96)];
  const minutes = (slot % 96) * 15;
  const hour = Math.floor(minutes / 60);
  return day + ' ' + (hour % 12 || 12) + ':' + String(minutes % 60).padStart(2, '0') + ' ' + (hour < 12 ? 'AM' : 'PM');
}

export default function ProfileSetupPage({ profile: savedProfile, onProfileChange: saveProfile, onAvatarSelect, navigate, goTo, editing = false, step = 1, setupDraft, onSetupDraft }) {
  const [profile, onProfileChange] = useState(editing ? savedProfile : setupDraft || savedProfile);
  const [nameError, setNameError] = useState('');
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState('');
  const [course, setCourse] = useState('');
  const [error, setError] = useState('');
  const [photoError, setPhotoError] = useState('');
  const [photoUrl, setPhotoUrl] = useState('');
  const [studyDay, setStudyDay] = useState(1);
  const [studyHour, setStudyHour] = useState(12);
  const [studyMinute, setStudyMinute] = useState(0);
  const courseInput = useRef(null);
  const photoInput = useRef(null);

  useEffect(() => {
    if (!profile?.photo) { setPhotoUrl(profile?.pictureUrl || ''); return; }
    const url = URL.createObjectURL(profile.photo);
    setPhotoUrl(url);
    return () => URL.revokeObjectURL(url);
  }, [profile?.photo, profile?.pictureUrl]);

  useEffect(() => {
    if (!editing && profile) onSetupDraft(profile);
  }, [profile, editing, onSetupDraft]);
  useEffect(() => {
    if (!editing && step > 1 && profile && !profile.classes.length) goTo('/profile-setup');
  }, [step, editing, profile?.classes.length]);
  if (!profile) return <main className="profile-setup profile-missing">
    <BookIcon /><h1>Let’s start with your name.</h1>
    <p>Log in or sign up to set up your profile.</p>
    <a href="/signup" onClick={navigate}>Go to signup →</a>
  </main>;

  function update(field, value) {
    onProfileChange((previous) => ({ ...previous, [field]: value }));
  }

  function addCourse() {
    const match = course.trim().toUpperCase().match(/^([A-Z]{2,4})\s*(\d{4})$/);
    if (!match) { setError('Use a subject and four-digit course number, like CSC 2001.'); courseInput.current?.focus(); return false; }
    const normalized = `${match[1]} ${match[2]}`;
    if (profile.classes.includes(normalized)) { setError('You’ve already added that class.'); courseInput.current?.focus(); return false; }
    if (profile.classes.length >= 30) { setError('You can add up to 30 classes.'); return false; }
    update('classes', [...profile.classes, normalized]);
    setCourse('');
    setError('');
    courseInput.current?.focus();
    return true;
  }

  function addStudyTime() {
    const slot = studyDay * 96 + studyHour * 4 + studyMinute / 15;
    const studyTimes = profile.studyTimes || [];
    if (studyTimes.includes(slot)) return;
    update('studyTimes', [...studyTimes, slot].sort((first, second) => first - second));
  }

  async function finish(event) {
    event.preventDefault();
    if (saving) return;
    if (editing && (!profile.name.trim() || profile.name.trim().length > 32)) { setNameError('Enter your name.'); document.getElementById('profile-name')?.focus(); return; }
    let classes = profile.classes;
    if (editing || step === 1) {
    if (course.trim()) {
      const match = course.trim().toUpperCase().match(/^([A-Z]{2,4})\s*(\d{4})$/);
      if (!match || classes.includes(`${match[1]} ${match[2]}`)) { addCourse(); return; }
      classes = [...classes, `${match[1]} ${match[2]}`];
    }
    else if (!profile.classes.length) { setError('Add at least one current class to continue.'); courseInput.current?.focus(); return; }
    }
    if (classes.length > 30) { setSaveError('You can add up to 30 classes.'); return; }
    if (!classes.length) { setSaveError('Add at least one class.'); return; }
    const draft = { ...profile, name: profile.name.trim(), classes };
    if (!editing && step < 3) { onSetupDraft(draft); goTo(step === 1 ? '/profile-setup/about' : '/profile-setup/avatar'); return; }
    setSaving(true); setSaveError('');
    try { await saveProfile({ ...profile, name: profile.name.trim(), classes }); if (!editing) onSetupDraft(null); goTo('/home'); }
    catch (error) { setSaveError(error.message); }
    finally { setSaving(false); }
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

  async function selectAvatar(avatar) {
    if (saving || (!profile.photo && !profile.pictureUrl && profile.avatar === avatar)) return;
    onProfileChange(previous => ({ ...previous, photo: null, pictureUrl: null, avatar }));
    setPhotoError('');
    setSaveError('');
    try { await onAvatarSelect?.(avatar); }
    catch (error) { setSaveError(`We couldn't save your avatar: ${error.message}`); }
  }

  return <div className={`profile-setup setup-flow${editing ? ' profile-edit' : ''}`}>
    <header className="profile-header"><a className="brand" href="/" onClick={navigate}><span className="brand-icon"><BookIcon /></span>{PRODUCT_NAME}.</a></header>
    <main className="profile-grid">
      <form className="profile-card" onSubmit={finish} noValidate><p className="setup-progress">{editing ? 'Your profile' : `Step ${step} of 3`}</p><h1>{editing ? 'Edit profile.' : step === 1 ? 'Your classes.' : step === 2 ? 'A little about you.' : 'Pick an avatar.'}</h1>{saveError && <p role="alert" className="error">{saveError}</p>}
        {editing && <div className="field"><label htmlFor="profile-name">Name (required)</label><input id="profile-name" maxLength={32} autoComplete="name" required value={profile.name} onChange={(event) => { update('name', event.target.value); setNameError(''); }} aria-invalid={Boolean(nameError)} aria-describedby={nameError ? 'profile-name-error' : undefined} />{nameError && <p id="profile-name-error" className="error">{nameError}</p>}</div>}
        {(editing || step === 1) && <section aria-labelledby="classes-title"><div className="field"><label htmlFor="major">Major (optional)</label><input id="major" maxLength={100} value={profile.major} onChange={event => update('major', event.target.value)} /></div><div className="profile-section-title"><h2 id="classes-title">Your current classes</h2><span>Required</span></div><label htmlFor="course">Class code</label><div className="profile-class-entry"><input ref={courseInput} id="course" value={course} placeholder="CSC 2001" autoCapitalize="characters" spellCheck="false" aria-invalid={Boolean(error)} aria-describedby={error ? 'course-error' : 'course-hint'} onChange={(event) => { setCourse(event.target.value); setError(''); }} onKeyDown={(event) => { if (event.key === 'Enter') { event.preventDefault(); addCourse(); } }} /><button type="button" onClick={addCourse}>Add class +</button></div><p className="hint" id="course-hint">e.g. CSC 2001</p>{error && <p className="error" id="course-error" role="alert">{error}</p>}<ul className="profile-class-list" aria-label="Added classes">{profile.classes.map((item) => <li key={item}>{item}<button type="button" aria-label={`Remove ${item}`} onClick={() => { update('classes', profile.classes.filter((value) => value !== item)); setError(''); }}>×</button></li>)}</ul></section>}
        {(editing || step === 2) && <section><div className="field"><label htmlFor="year">Year (optional)</label><select id="year" value={profile.year} onChange={event => update('year', event.target.value)}><option value="">Select year</option>{['First', 'Second', 'Third', 'Fourth', 'Fifth+'].map(year => <option key={year}>{year}</option>)}</select></div><div className="field"><label htmlFor="bio">Bio (optional)</label><textarea id="bio" rows={4} maxLength={500} value={profile.bio} onChange={event => update('bio', event.target.value)} /></div><div className="profile-study-times"><div className="profile-section-title"><h2 id="study-times-title">When do you like to study?</h2><span>Optional</span></div><p className="hint">Drag the wheels to choose 15-minute availability windows.</p><StudyTimePicker day={studyDay} hour={studyHour} minute={studyMinute} onDayChange={setStudyDay} onHourChange={setStudyHour} onMinuteChange={setStudyMinute} /><button className="profile-add-time" type="button" onClick={addStudyTime}>Add this time</button><ul className="profile-time-list" aria-label="Added study times">{(profile.studyTimes || []).map(slot => <li key={slot}>{formatStudyTime(slot)}<button type="button" aria-label={'Remove ' + formatStudyTime(slot)} onClick={() => update('studyTimes', profile.studyTimes.filter(value => value !== slot))}>×</button></li>)}</ul></div></section>}
        {(editing || step === 3) && <section aria-label="Profile picture"><h2 className="profile-picture-title">Profile picture</h2><fieldset className="setup-avatars"><legend className="sr-only">Choose your avatar</legend>{AVATARS.map((avatar) => <label key={avatar.id} className={!profile.photo && !profile.pictureUrl && profile.avatar === avatar.id ? 'selected' : ''}><input type="radio" name="avatar" checked={!profile.photo && !profile.pictureUrl && profile.avatar === avatar.id} onChange={() => selectAvatar(avatar.id)} /><AvatarArt avatar={avatar.id} label={avatar.label} />{!profile.photo && !profile.pictureUrl && profile.avatar === avatar.id && <span className="avatar-selected" aria-hidden="true">&#10003;</span>}</label>)}</fieldset><div className="profile-upload-panel">
  <input ref={photoInput} id="photo" type="file" hidden accept="image/jpeg,image/png,image/webp" onChange={selectPhoto} aria-label="Upload profile photo" />
  {photoUrl && <img className="profile-upload-preview" src={photoUrl} alt="Selected profile photo" onError={() => { setPhotoError('Choose a valid image.'); onProfileChange(previous => ({ ...previous, photo: null, pictureUrl: null })); }} />}
  <div className="profile-upload-content">
    <p className="profile-upload-title">{photoUrl ? 'Your photo' : 'Prefer your own photo?'}</p>
    <p id="photo-hint" className="hint">JPG, PNG or WebP · Up to 5 MB</p>
    <div className="profile-upload-actions">
      <button className="profile-upload-button" type="button" aria-describedby="photo-hint" onClick={() => photoInput.current?.click()}>
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" aria-hidden="true"><path d="M12 16V4m-5 5 5-5 5 5M4 15v5h16v-5" strokeLinecap="round" strokeLinejoin="round" /></svg>
        {photoUrl ? 'Change photo' : 'Upload photo'}
      </button>
      {photoUrl && <button className="profile-remove-photo" type="button" onClick={() => { onProfileChange(previous => ({ ...previous, photo: null, pictureUrl: null })); setPhotoError(''); }}>Remove</button>}
    </div>
  </div>
</div>
{photoError && <p className="error" role="alert">{photoError}</p>}
<p className="hint profile-media-note">Your selected avatar saves automatically. Photos save with your profile.</p></section>}
        <button className="submit" type="submit" disabled={saving}>{saving ? 'Saving...' : editing ? 'Save changes' : step === 3 ? 'Finish' : 'Next'} <span aria-hidden="true">↗</span></button>
        {!editing && step > 1 && <button className="setup-back" type="button" disabled={saving} onClick={() => { onSetupDraft(profile); goTo(step === 3 ? '/profile-setup/about' : '/profile-setup'); }}>Back</button>}
        {editing && <a className="profile-cancel" href="/home" onClick={navigate}>Cancel changes</a>}
      </form>
    </main>
  </div>;
}
