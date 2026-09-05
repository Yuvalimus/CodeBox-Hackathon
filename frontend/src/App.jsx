import React, { useEffect, useState } from 'react';
import LoginPage from './pages/LoginPage.jsx';
import LandingPage from './pages/LandingPage.jsx';
import SignupPage from './pages/SignupPage.jsx';
import ProfileSetupPage from './pages/ProfileSetupPage.jsx';
import NotFoundPage from './pages/NotFoundPage.jsx';
import { PRODUCT_NAME } from './config/brand.js';

// Register future pages here, with their own page component and title.
const routes = {
  '/': { component: LandingPage, title: 'Find your study people' },
  '/login': { component: LoginPage, title: 'Log in' },
  '/signup': { component: SignupPage, title: 'Sign up' },
  '/profile-setup': { component: ProfileSetupPage, title: 'Set up your profile' },
};
const notFoundRoute = { component: NotFoundPage, title: 'Page not found' };

export default function App() {
  const [profile, setProfile] = useState(null);
  const [pathname, setPathname] = useState(() => window.location.pathname);
  const route = Object.hasOwn(routes, pathname) ? routes[pathname] : notFoundRoute;
  const Page = route.component;

  useEffect(() => {
    const sync = () => setPathname(window.location.pathname);
    window.addEventListener('popstate', sync);
    return () => window.removeEventListener('popstate', sync);
  }, []);

  useEffect(() => {
    document.title = `${route.title} · ${PRODUCT_NAME}`;
  }, [route.title]);

  function navigate(event) {
    if (event.metaKey || event.ctrlKey || event.shiftKey || event.altKey || event.button !== 0) return;
    event.preventDefault();
    const path = event.currentTarget.getAttribute('href');
    if (path === window.location.pathname) return;
    window.history.pushState({}, '', path);
    setPathname(window.location.pathname);
    window.scrollTo(0, 0);
  }

  function onSignup({ name }) {
    setProfile({ name, classes: [], major: '', bio: '', year: '', photo: null });
    window.history.pushState({}, '', '/profile-setup');
    setPathname('/profile-setup');
    window.scrollTo(0, 0);
  }

  return <Page navigate={navigate} onSignup={onSignup} profile={profile} onProfileChange={setProfile} />;
}
