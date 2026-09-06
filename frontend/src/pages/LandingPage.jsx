import React from 'react';
import BookIcon from '../components/BookIcon.jsx';
import BrandName from '../components/BrandName.jsx';
import './LandingPage.css';

const steps = [
  ['Pick a class.', 'Add the course you actually want to study for.'],
  ['See who’s around.', 'Swipe through people taking it with you.'],
  ['Make a plan.', 'Match when it feels right, then take it from there.'],
];

export default function LandingPage({ navigate, profile }) {
  const loggedIn = Boolean(profile);
  const destination = loggedIn ? '/home' : '/signup';
  return <div className="landing">
    <a className="lp-skip" href="#main-content">Skip to content</a>
    <header className="lp-header lp-container">
      <a className="brand" href="/" onClick={navigate}><span className="brand-icon"><BookIcon /></span><BrandName /></a>
      <nav className="lp-nav" aria-label="Main navigation">
        {loggedIn ? <a className="lp-button" href="/home" onClick={navigate}>Go to home <span aria-hidden="true">↗</span></a> : <><a className="lp-login" href="/login" onClick={navigate}>Log in</a><a className="lp-button" href="/signup" onClick={navigate}>Sign up <span aria-hidden="true">↗</span></a></>}
      </nav>
    </header>
    <main id="main-content">
      <section className="lp-hero lp-container" aria-labelledby="landing-title">
        <div className="lp-kicker"><span aria-hidden="true">•</span> CAL POLY, SAN LUIS OBISPO</div>
        <h1 id="landing-title">Find a<br /><em>study buddy.</em></h1>
        <p>See classmates in your courses who are down to meet up, work through homework, or study before an exam.</p>
        <a className="lp-button" href={destination} onClick={navigate}>{loggedIn ? 'Go to home' : 'Get started'} <span aria-hidden="true">↗</span></a>
      </section>
      <section className="lp-how" aria-labelledby="how-title">
        <div className="lp-container">
          <div className="lp-kicker">HOW IT WORKS</div>
          <h2 id="how-title">Skip the group-chat<br />guessing game.</h2>
          <div className="lp-steps">{steps.map(([title, text]) => <article className="lp-step" key={title}><h3>{title}</h3><p>{text}</p></article>)}</div>
        </div>
      </section>
    </main>
  </div>;
}
