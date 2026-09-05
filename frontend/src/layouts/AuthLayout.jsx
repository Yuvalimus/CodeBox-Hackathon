import React from 'react';
import BookIcon from '../components/BookIcon.jsx';
import { PRODUCT_NAME } from '../config/brand.js';

export default function AuthLayout({ children, label, navigate }) {
  return <main className="auth-layout">
    <section className="story-panel" aria-labelledby="story-heading">
      <a className="brand" href="/" onClick={navigate}><span className="brand-icon"><BookIcon /></span>{PRODUCT_NAME}<span className="brand-dot">.</span></a>
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
    <section className="form-panel" aria-label={label}>{children}<footer>Less studying solo. More figuring it out together.</footer></section>
  </main>;
}
