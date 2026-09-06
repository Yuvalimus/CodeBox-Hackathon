import React, { useEffect, useRef, useState } from 'react';

const HOURS = Array.from({ length: 24 }, (_, index) => index);
const MINUTES = [0, 15, 30, 45];
const DAYS = [0, 1, 2, 3, 4, 5, 6];

function Wheel({ label, values, value, onChange, format }) {
  const wheel = useRef(null);
  const dragging = useRef(null);
  const [selectedIndex, setSelectedIndex] = useState(0);
  const itemHeight = 44;
  const items = Array.from({ length: values.length * 5 }, (_, index) => values[index % values.length]);
  useEffect(() => {
    const index = values.indexOf(value);
    if (!wheel.current || index < 0) return;
    const centeredIndex = values.length * 2 + index;
    wheel.current.scrollTo({ top: centeredIndex * itemHeight, behavior: 'smooth' });
    setSelectedIndex(centeredIndex);
  }, [value, values]);
  function selectFromScroll() {
    const rawIndex = Math.round(wheel.current.scrollTop / itemHeight);
    const index = Math.max(0, Math.min(items.length - 1, rawIndex));
    setSelectedIndex(index);
    onChange(items[index]);
    // Recenter after a long drag so the picker never reaches a hard end.
    if (index < values.length || index >= values.length * 4) {
      const centeredIndex = values.length * 2 + (index % values.length);
      requestAnimationFrame(() => {
        if (!wheel.current) return;
        wheel.current.style.scrollBehavior = 'auto';
        wheel.current.scrollTop = centeredIndex * itemHeight;
        wheel.current.style.scrollBehavior = '';
        setSelectedIndex(centeredIndex);
      });
    }
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
  return <div className="time-wheel-wrap"><span>{label}</span><div className="time-wheel" ref={wheel} onScroll={selectFromScroll} onPointerDown={pointerDown} onPointerMove={pointerMove} onPointerUp={pointerUp} onPointerCancel={pointerUp}><div className="time-wheel-spacer" aria-hidden="true" />{items.map((item, index) => <button type="button" key={`${item}-${index}`} className={index === selectedIndex ? 'selected' : ''} onClick={() => onChange(item)}>{format(item)}</button>)}<div className="time-wheel-spacer" aria-hidden="true" /></div></div>;
}

function PeriodPicker({ hour, onChange }) {
  const isPm = hour >= 12;
  function setPeriod(period) { onChange((hour % 12) + (period ? 12 : 0)); }
  return <div className="time-wheel-wrap"><span>Period</span><div className="period-picker"><button type="button" className={!isPm ? 'selected' : ''} onClick={() => setPeriod(false)}>AM</button><button type="button" className={isPm ? 'selected' : ''} onClick={() => setPeriod(true)}>PM</button></div></div>;
}

export default function StudyTimePicker({ day, hour, minute, onDayChange, onHourChange, onMinuteChange }) {
  return <div className="alarm-picker" aria-label="Study time picker">
    <div className="time-wheels"><Wheel label="Day" values={DAYS} value={day} onChange={onDayChange} format={item => ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'][item]} /><Wheel label="Hour" values={HOURS} value={hour} onChange={onHourChange} format={item => String(item % 12 || 12).padStart(2, '0')} /><span className="time-colon" aria-hidden="true">:</span><Wheel label="Minutes" values={MINUTES} value={minute} onChange={onMinuteChange} format={item => String(item).padStart(2, '0')} /><PeriodPicker hour={hour} onChange={onHourChange} /></div>
  </div>;
}
