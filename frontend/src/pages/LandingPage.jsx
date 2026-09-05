import React from 'react';
import BookIcon from '../components/BookIcon.jsx';
import { PRODUCT_NAME } from '../config/brand.js';
import './LandingPage.css';

const steps = [
  ['01', 'Bring your classes.', 'Add your major and current classes so you can find people working through the same things.'],
  ['02', 'Find your people.', 'Pick what you’re studying today, go online, and browse study buddies who are online too.'],
  ['03', 'Make a little progress.', 'Send a match request. Once it’s accepted, chat and figure out where to start together.'],
];

function Brand({ navigate }) {
  return <a className="brand" href="/" onClick={navigate}><span className="brand-icon"><BookIcon /></span>{PRODUCT_NAME}<span className="brand-dot">.</span></a>;
}

function SessionIllustration() {
  return <div className="lp-art" role="img" aria-label="Illustrated study plan: something to work on, someone to work with, and a little more understanding.">
    <span className="lp-art-caption">A GOOD SESSION STARTS WITH GOOD COMPANY</span>
    <span className="lp-doodle" aria-hidden="true">✳</span>
    <div className="lp-paper">
      <div className="lp-paper-top"><span>TODAY’S STUDY PLAN</span><BookIcon /></div>
      <h3>A fresh page.<br />A familiar class.</h3>
      <div className="lp-task"><span>✓</span> Something to work on</div>
      <div className="lp-task"><span>✓</span> Someone to work with</div>
      <div className="lp-task"><span> </span> A little more understanding</div>
      <div className="lp-paper-bottom">LESS “I’M STUCK.”<br /><strong>MORE “OH, I GET IT.”</strong><span>↗</span></div>
    </div>
    <div className="lp-sticker">better<br /><strong>together.</strong><span>✦</span></div>
    <div className="lp-book lp-book-light">ONE CLASS AT A TIME <span>01</span></div>
    <div className="lp-book lp-book-dark"><BookIcon />THE ART OF FIGURING IT OUT</div>
    <span className="lp-art-footnote">Your next “aha” moment, shared.</span>
  </div>;
}

export default function LandingPage({ navigate }) {
  return <div className="landing">
    <a className="lp-skip" href="#main-content">Skip to content</a>
    <header className="lp-header lp-container">
      <Brand navigate={navigate} />
      <nav className="lp-nav" aria-label="Main navigation"><a className="lp-section-link" href="#how-it-works">How it works</a><a className="lp-section-link" href="#about">Our idea</a><a className="lp-login" href="/login" onClick={navigate}>Log in</a><a className="lp-button lp-button-small" href="/signup" onClick={navigate}>Sign up <span aria-hidden="true">↗</span></a></nav>
    </header>
    <main id="main-content">
      <section className="lp-hero lp-container" aria-labelledby="landing-title">
        <div className="lp-hero-copy"><div className="lp-kicker"><span />FOR CAL POLY, SAN LUIS OBISPO</div><h1 id="landing-title">Your classes.<br />Your people.<br /><em>Your next aha.</em></h1><p>Big ideas are easier to figure out together. Find a Mustang studying what you’re studying, right when you’re ready to get into it.</p><div className="lp-actions"><a className="lp-button" href="/signup" onClick={navigate}>Find your study people <span aria-hidden="true">↗</span></a><a className="lp-text-link" href="#how-it-works">See how it works <span aria-hidden="true">↓</span></a></div><div className="lp-email-note"><BookIcon /><span>Your Cal Poly email. A whole campus of possibility.</span></div></div>
        <SessionIllustration />
      </section>
      <div className="lp-values"><div className="lp-container"><span>Same classes, shared questions.</span><span aria-hidden="true">✳</span><span>Study when you’re ready.</span><span aria-hidden="true">✳</span><span>A little company goes a long way.</span></div></div>
      <section className="lp-how lp-container" id="how-it-works" aria-labelledby="how-title"><div className="lp-section-heading"><div><div className="lp-kicker">FROM SOLO TO STUDY BUDDIES</div><h2 id="how-title">Less planning.<br />More figuring it out.</h2></div><p>No schedules to coordinate in advance. Just what you’re studying, who’s around, and a place to start.</p></div><div className="lp-steps">{steps.map(([number, title, text]) => <article className="lp-step" key={number}><div className="lp-step-number">{number}<span aria-hidden="true">↗</span></div><h3>{title}</h3><p>{text}</p></article>)}</div></section>
      <section className="lp-about lp-container" id="about" aria-labelledby="about-title"><div className="lp-about-art" aria-hidden="true"><span className="lp-about-star">✳</span><div className="lp-about-note">You don’t have to<br />have it all<br /><em>figured out.</em><span>That’s why we’re here. ↗</span></div><span className="lp-about-caption">SAME CAMPUS. A LITTLE MORE CONNECTION.</span></div><div className="lp-about-copy"><div className="lp-kicker">THE IDEA BEHIND IT</div><h2 id="about-title">A big campus.<br />A smaller study circle.</h2><p>Somewhere on campus, someone is probably staring at the same kind of problem you are. We want to make it easier to find each other.</p><p>{PRODUCT_NAME} is a study-buddy project for Cal Poly SLO students, built around shared classes and the moments you’re actually ready to study. A question, a second perspective, or a bit of motivation can be all it takes to get going.</p><a className="lp-text-link" href="/signup" onClick={navigate}>Let’s figure it out together <span aria-hidden="true">↗</span></a></div></section>
      <section className="lp-cta lp-container" aria-labelledby="cta-title"><span className="lp-kicker">MAKE ROOM FOR A STUDY BUDDY</span><h2 id="cta-title">Your next session<br />could use a plus-one.</h2><a className="lp-button" href="/signup" onClick={navigate}>Get started <span aria-hidden="true">↗</span></a><p>Already around? <a href="/login" onClick={navigate}>Log in</a></p><span className="lp-cta-star" aria-hidden="true">✳</span></section>
    </main>
    <footer className="lp-footer lp-container"><Brand navigate={navigate} /><p>A study-buddy project for Cal Poly SLO.</p><span>One session at a time.</span></footer>
  </div>;
}
