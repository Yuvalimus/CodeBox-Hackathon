import React from 'react';

export default function StudyTimePicker({ hour, minute, onHourChange, onMinuteChange }) {
  return <div className="duration-wheels">
    <label>Hours<select size={3} aria-label="Study duration hours" value={hour} onChange={event => onHourChange(Number(event.target.value))}>{Array.from({ length: 24 }, (_, value) => <option key={value} value={value}>{value} {value === 1 ? 'hour' : 'hours'}</option>)}</select></label>
    <label>Minutes<select size={3} aria-label="Study duration minutes" value={minute} onChange={event => onMinuteChange(Number(event.target.value))}>{[0, 15, 30, 45].map(value => <option key={value} value={value}>{value} min</option>)}</select></label>
  </div>;
}
