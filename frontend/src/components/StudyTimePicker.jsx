import React, { useEffect, useRef } from 'react';

const HOURS = Array.from({ length: 24 }, (_, index) => index);
const MINUTES = [0, 15, 30, 45];

function Wheel({ label, values, value, onChange, format }) {
  const wheel = useRef(null);
  const dragging = useRef(null);
  const itemHeight = 44;
  useEffect(() => {
    const index = values.indexOf(value);
    if (wheel.current && index >= 0) wheel.current.scrollTop = index * itemHeight;
  }, [value, values]);
  function selectFromScroll() {
    const index = Math.round(wheel.current.scrollTop / itemHeight);
    onChange(values[Math.max(0, Math.min(values.length - 1, index))]);
  }
  function pointerDown(event) {
    dragging.current = { y: event.clientY, top: wheel.current.scrollTop };
    wheel.current.setPointerCapture(event.pointerId);
  }
  function pointerMove(event) {
    if (!dragging.current) return;
    wheel.current.scrollTop = dragging.current.top - (event.clientY - dragging.current.y);
  }
  function pointerUp() {
    dragging.current = null;
    selectFromScroll();
  }
  return <div className="time-wheel-wrap"><span>{label}</span><div className="time-wheel" ref={wheel} onScroll={selectFromScroll} onPointerDown={pointerDown} onPointerMove={pointerMove} onPointerUp={pointerUp} onPointerCancel={pointerUp}>{values.map(item => <button type="button" key={item} className={item === value ? 'selected' : ''} onClick={() => onChange(item)}>{format(item)}</button>)}</div></div>;
}

export default function StudyTimePicker({ day, hour, minute, onDayChange, onHourChange, onMinuteChange }) {
  return <div className="alarm-picker" aria-label="Study time picker">
    <label>Day<select value={day} onChange={event => onDayChange(Number(event.target.value))}>{['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'].map((name, index) => <option key={name} value={index}>{name}</option>)}</select></label>
    <div className="time-wheels"><Wheel label="Hour" values={HOURS} value={hour} onChange={onHourChange} format={item => String(item % 12 || 12).padStart(2, '0')} /><span className="time-colon" aria-hidden="true">:</span><Wheel label="Minutes" values={MINUTES} value={minute} onChange={onMinuteChange} format={item => String(item).padStart(2, '0')} /><Wheel label="" values={[0, 1]} value={hour >= 12 ? 1 : 0} onChange={period => onHourChange((hour % 12) + (period ? 12 : 0))} format={period => period ? 'PM' : 'AM'} /></div>
  </div>;
}
