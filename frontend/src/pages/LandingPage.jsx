import React from 'react';
import BookIcon from '../components/BookIcon.jsx';
import { PRODUCT_NAME } from '../config/brand.js';
import './LandingPage.css';

const steps = [
  ['01', 'Add your classes', 'Tell us what you are taking this term.'],
  ['02', 'Find a buddy', 'See students studying the same thing right now.'],
  ['03', 'Study together', 'Send a request and make a little progress.'],
];

function Brand({ navigate }) {
  return <a className="brand" href="/" onClick={navigate}><span className="brand-icon"><BookIcon /></span>{PRODUCT_NAME}<span className="brand-dot">.</span></a>;
}

function SessionIllustration() {
  return <div className="lp-art" role="img" aria-label="A study plan with a class, a study buddy, and an aha moment.">
    <span className="lp-art-caption">A GOOD SESSION STARTS WITH GOOD COMPANY</span>
    <span className="lp-doodle" aria-hidden="true">✳</span>
    <div className="lp-paper">
      <div className="lp-paper-top"><span>TODAY’S STUDY PLAN</span><BookIcon /></div>
      <h3>A fresh page.<br />A familiar class.</h3>
      <div className="lp-task"><span>✓</span> Pick a class</div>
      <div className="lp-task"><span>✓</span> Find someone online</div>
      <div className="lp-task"><span> </span> Figure it out together</div>
      <div className="lp-paper-bottom">LESS “I’M STUCK.”<br /><strong>MORE “OH, I GET IT.”</strong><span>↗</span></div>
    </div>
    <div className="lp-sticker">better<br /><strong>together.</strong><span>✦</span></div>
    <div className="lp-book lp-book-light">ONE CLASS AT A TIME <span>01</span></div>
    <div className="lp-book lp-book-dark"><BookIcon />THE ART OF FIGURING IT OUT</div>
  </div>;
}

export default function LandingPage({ navigate }) {
  return <div className="landing">
    <a className="lp-skip" href="#main-content">Skip to content</a>
    <header className="lp-header lp-container">
      <Brand navigate={navigate} />
      <nav className="lp-nav" aria-label="Main navigation">
        <a className="lp-section-link" href="#how-it-works">How it works</a>
        <a className="lp-login" href="/login" onClick={navigate}>Log in</a>
        <a className="lp-button lp-button-small" href="/signup" onClick={navigate}>Sign up <span aria-hidden="true">↗</span></a>
      </nav>
    </header>
    <main id="main-content">
      <section className="lp-hero lp-container" aria-labelledby="landing-title">
        <div className="lp-hero-copy">
          <div className="lp-kicker"><span />FOR CAL POLY, SAN LUIS OBISPO</div>
          <h1 id="landing-title">Find your<br /><em>study people.</em></h1>
          <p>Match with classmates who are working through the same class and ready to study now.</p>
          <div className="lp-actions"><a className="lp-button" href="/signup" onClick={navigate}>Get started <span aria-hidden="true">↗</span></a><a className="lp-text-link" href="#how-it-works">How it works <span aria-hidden="true">↓</span></a></div>
          <div className="lp-email-note"><BookIcon /><span>Cal Poly students only · a simple place to start.</span></div>
        </div>
        <SessionIllustration />
      </section>
      <section className="lp-how lp-container" id="how-it-works" aria-labelledby="how-title">
        <div className="lp-section-heading"><div><div className="lp-kicker">HOW IT WORKS</div><h2 id="how-title">Less solo studying.<br />More momentum.</h2></div><p>Set up your profile in a minute, then find someone who is online and in your class.</p></div>
        <div className="lp-steps">{steps.map(([number, title, text]) => <article className="lp-step" key={number}><div className="lp-step-number">{number}<span aria-hidden="true">↗</span></div><h3>{title}</h3><p>{text}</p></article>)}</div>
      </section>
      <section className="lp-about lp-container" aria-label="About Study together"><p><strong>Made for the in-between moments:</strong> the homework question, the library table, and the nudge to actually get started.</p></section>
    </main>
    <footer className="lp-footer lp-container"><Brand navigate={navigate} /><p>A study-buddy project for Cal Poly SLO.</p></footer>
  </div>;
}
