import React, { useState } from 'react';

export default function PasswordField({ id, label, value, onChange, error, autoComplete, hint }) {
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
