import React, { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import './styles.css';

const PRODUCT_NAME = 'Study together';
const isSignupRoute = () => window.location.pathname === '/signup';

function BookIcon() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" aria-hidden="true"><path d="M12 5v15M3 4.5c3-1 6-.5 9 1.5 3-2 6-2.5 9-1.5v14c-3-1-6-.5-9 1.5-3-2-6-2.5-9-1.5z" /></svg>;
}

function PasswordField({ id, label, value, onChange, error, autoComplete, hint }) {
  const [visible, setVisible] = useState(false);
  return <div className="field">
    <label htmlFor={id}>{label}</label>
    <div className="password-input">
      <input id={id} name={id} type={visible ? 'text' : 'password'} value={value} onChange={onChange} autoComplete={autoComplete} required aria-invalid={Boolean(error)} aria-describedby={error ? `${id}-error` : hint ? `${id}-hint` : undefined} />
      <button type="button" className="reveal" onClick={() => setVisible(!visible)} aria-label={`${visible ? 'Hide' : 'Show'} ${label.toLowerCase()}`} aria-pressed={visible}>{visible ? 'Hide' : 'Show'}</button>
    </div>
    {error ? <p className="error" id={`${id}-error`}>{error}</p> : hint && <p className="hint" id={`${id}-hint`}>{hint}</p>}
  </div>;
}

function AuthForm({ signup, navigate }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [errors, setErrors] = useState({});
  const [submitted, setSubmitted] = useState(false);

  function update(setter, key) {
    return (event) => {
      setter(event.target.value);
      setErrors((previous) => ({ ...previous, [key]: undefined }));
      setSubmitted(false);
    };
  }

  function submit(event) {
    event.preventDefault();
    const nextErrors = {};
    if (!/^[^\s@]+@calpoly\.edu$/i.test(email.trim())) nextErrors.email = 'Enter your Cal Poly email (name@calpoly.edu).';
    if (!password) nextErrors.password = 'Enter your password.';
    if (signup && password.length < 8) nextErrors.password = 'Use at least 8 characters.';
    if (signup && (!confirmation || confirmation !== password)) nextErrors.confirmation = 'Your passwords must match.';
    setErrors(nextErrors);
    setSubmitted(false);
    if (Object.keys(nextErrors).length) {
      document.getElementById(Object.keys(nextErrors)[0])?.focus();
      return;
    }
    // TODO: Connect the login/signup API here. Do not store credentials locally.
    setSubmitted(true);
  }

  return <div className="form-content">
    <div className="eyebrow">YOUR NEXT STUDY SESSION STARTS HERE</div>
    <h2>{signup ? 'Find your study people.' : 'Welcome back.'}</h2>
    <p className="form-intro">{signup ? 'A little company. A lot more motivation.' : 'Good company makes the hard classes easier.'}</p>
    <nav className="auth-tabs" aria-label="Account access">
      <a href="/login" aria-current={!signup ? 'page' : undefined} onClick={navigate}>Log in</a>
      <a href="/signup" aria-current={signup ? 'page' : undefined} onClick={navigate}>Sign up</a>
    </nav>
    <form onSubmit={submit} noValidate>
      <div className="field">
        <label htmlFor="email">Cal Poly email</label>
        <input id="email" name="email" type="email" placeholder="you@calpoly.edu" autoComplete="username" autoCapitalize="none" spellCheck="false" value={email} onChange={update(setEmail, 'email')} required aria-invalid={Boolean(errors.email)} aria-describedby={errors.email ? 'email-error' : 'email-hint'} />
        {errors.email ? <p className="error" id="email-error">{errors.email}</p> : <p className="hint" id="email-hint">For Cal Poly San Luis Obispo students.</p>}
      </div>
      <PasswordField id="password" label="Password" value={password} onChange={update(setPassword, 'password')} autoComplete={signup ? 'new-password' : 'current-password'} error={errors.password} hint={signup ? 'Use at least 8 characters.' : undefined} />
      {signup && <PasswordField id="confirmation" label="Confirm password" value={confirmation} onChange={update(setConfirmation, 'confirmation')} autoComplete="new-password" error={errors.confirmation} />}
      <button className="submit" type="submit">{signup ? 'Create account' : 'Log in'}<span aria-hidden="true">↗</span></button>
      {submitted && <p className="notice" role="status">{signup ? 'Your details look ready, but account creation isn’t connected yet. No account has been created.' : 'Your details look ready, but login isn’t connected yet. You haven’t been signed in.'}</p>}
    </form>
    <p className="switch-prompt">{signup ? 'Already found us?' : 'New around here?'} <a href={signup ? '/login' : '/signup'} onClick={navigate}>{signup ? 'Log in' : 'Create an account'}</a></p>
    <p className="prototype-note">Frontend preview · Authentication coming soon</p>
  </div>;
}

function App() {
  const [signup, setSignup] = useState(isSignupRoute);
  useEffect(() => {
    const sync = () => setSignup(isSignupRoute());
    window.addEventListener('popstate', sync);
    return () => window.removeEventListener('popstate', sync);
  }, []);
  useEffect(() => { document.title = `${signup ? 'Sign up' : 'Log in'} · ${PRODUCT_NAME}`; }, [signup]);
  function navigate(event) {
    if (event.metaKey || event.ctrlKey || event.shiftKey || event.altKey || event.button !== 0) return;
    event.preventDefault();
    window.history.pushState({}, '', event.currentTarget.getAttribute('href'));
    setSignup(isSignupRoute());
  }
  return <main className="auth-layout">
    <section className="story-panel" aria-labelledby="story-heading">
      <a className="brand" href="/login" onClick={navigate}><span className="brand-icon"><BookIcon /></span>{PRODUCT_NAME}<span className="brand-dot">.</span></a>
      <div className="story-content">
        <span className="campus-label"><span />CAL POLY, SAN LUIS OBISPO</span>
        <h1 id="story-heading">Same class. <br />Same campus. <br /><span>Better together.</span></h1>
        <p>Find a study buddy who gets it. Connect over your classes and turn “I’ll study later” into “meet you at the library.”</p>
        <div className="study-illustration" aria-hidden="true">
          <div className="orbit orbit-one" /><div className="orbit orbit-two" />
          <span className="spark spark-one">✳</span><span className="spark spark-two">+</span>
          <div className="note-card"><span className="note-heading">THE STUDY PLAN</span><div><span className="check">✓</span> Find your people</div><div><span className="check">✓</span> Bring your questions</div><div><span className="empty-check" /> Figure it out together</div><span className="note-line" /></div>
          <div className="book book-back">A LITTLE FOCUS</div><div className="book book-front"><BookIcon /> A LOT OF POSSIBILITY</div>
          <div className="round-sticker">let’s<br /><strong>study.</strong><span>↗</span></div>
        </div>
      </div>
      <p className="story-footer">Made for the way Mustangs study.<span>One session at a time.</span></p>
    </section>
    <section className="form-panel" aria-label={signup ? 'Create an account' : 'Log in to your account'}><AuthForm key={signup ? 'signup' : 'login'} signup={signup} navigate={navigate} /><footer>Less studying solo. More figuring it out together.</footer></section>
  </main>;
}

createRoot(document.getElementById('root')).render(<App />);
