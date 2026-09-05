import React, { useState } from 'react';
import PasswordField from '../PasswordField.jsx';

export default function AuthForm({ signup, navigate, onSignup, onLogin }) {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [errors, setErrors] = useState({});
  const [busy, setBusy] = useState(false);
  const [apiError, setApiError] = useState('');

  function update(setter, key) {
    return (event) => {
      setter(event.target.value);
      setErrors((previous) => ({ ...previous, [key]: undefined }));
      setApiError('');
    };
  }

  async function submit(event) {
    event.preventDefault();
    if (busy) return;
    const nextErrors = {};
    if (signup && (!name.trim() || name.trim().length > 32 || /[\u0000-\u001f\u007f-\u009f]/.test(name))) nextErrors.name = 'Enter a name of 1-32 characters, without control characters.';
    if (!/^[^\s@]+@calpoly\.edu$/i.test(email.trim())) nextErrors.email = 'Enter your Cal Poly email (name@calpoly.edu).';
    if (!password) nextErrors.password = 'Enter your password.';
    if (signup && (password.trim().length < 8 || password.trim().length > 200)) nextErrors.password = 'Use 8-200 characters.';
    if (signup && (!confirmation || confirmation !== password)) nextErrors.confirmation = 'Your passwords must match.';
    setErrors(nextErrors);
    setApiError('');
    if (Object.keys(nextErrors).length) {
      document.getElementById(Object.keys(nextErrors)[0])?.focus();
      return;
    }
    setBusy(true);
    try { await (signup ? onSignup : onLogin)({ name: name.trim(), email, password }); }
    catch (error) { setApiError(error.message); }
    finally { setBusy(false); }
  }

  return <div className="form-content">
    <div className="eyebrow">YOUR NEXT STUDY SESSION STARTS HERE</div>
    <h2>{signup ? 'Find your study people.' : 'Welcome back.'}</h2>
    <nav className="auth-tabs" aria-label="Account access">
      <a href="/login" aria-current={!signup ? 'page' : undefined} onClick={navigate}>Log in</a>
      <a href="/signup" aria-current={signup ? 'page' : undefined} onClick={navigate}>Sign up</a>
    </nav>
    <form onSubmit={submit} noValidate>
      {signup && <div className="field">
        <label htmlFor="name">Name</label>
        <input id="name" name="name" maxLength={32} autoComplete="name" value={name} onChange={update(setName, 'name')} required aria-invalid={Boolean(errors.name)} aria-describedby={errors.name ? 'name-error' : undefined} />
        {errors.name && <p className="error" id="name-error">{errors.name}</p>}
      </div>}
      <div className="field">
        <label htmlFor="email">Cal Poly email</label>
        <input id="email" name="email" type="email" placeholder="you@calpoly.edu" autoComplete="username" autoCapitalize="none" spellCheck="false" value={email} onChange={update(setEmail, 'email')} required aria-invalid={Boolean(errors.email)} aria-describedby={errors.email ? 'email-error' : 'email-hint'} />
        {errors.email ? <p className="error" id="email-error">{errors.email}</p> : <p className="hint" id="email-hint">For Cal Poly San Luis Obispo students.</p>}
      </div>
      <PasswordField id="password" label="Password" value={password} onChange={update(setPassword, 'password')} autoComplete={signup ? 'new-password' : 'current-password'} error={errors.password} hint={signup ? 'Use 8-200 characters.' : undefined} />
      {signup && <PasswordField id="confirmation" label="Confirm password" value={confirmation} onChange={update(setConfirmation, 'confirmation')} autoComplete="new-password" error={errors.confirmation} />}
      <button className="submit" type="submit" disabled={busy}>{busy ? 'Please wait...' : signup ? 'Create account' : 'Log in'}<span aria-hidden="true">↗</span></button>
      {apiError && <p className="error" role="alert">{apiError}</p>}
    </form>
    <p className="switch-prompt">{signup ? 'Already found us?' : 'New around here?'} <a href={signup ? '/login' : '/signup'} onClick={navigate}>{signup ? 'Log in' : 'Create an account'}</a></p>
  </div>;
}
