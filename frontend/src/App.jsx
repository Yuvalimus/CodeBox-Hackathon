import PresenceHeartbeat from './components/PresenceHeartbeat.jsx';
import { goOffline } from './presence.js';
import { saveProfileMedia } from './profileMedia.js';
import { request, setToken, hasToken, authenticationBody, fromUser, profileBody } from './api.js';
import React, { useEffect, useState, useRef, useCallback } from 'react';
import MatchWatcher from './components/MatchWatcher.jsx';
import MatchPage from './pages/MatchPage.jsx';
import ChatPage from './pages/ChatPage.jsx';
import LoginPage from './pages/LoginPage.jsx';
import LandingPage from './pages/LandingPage.jsx';
import SignupPage from './pages/SignupPage.jsx';
import SetupAboutPage from './pages/SetupAboutPage.jsx';
import SetupAvatarPage from './pages/SetupAvatarPage.jsx';
import ProfileSetupPage from './pages/ProfileSetupPage.jsx';
import HomePage from './pages/HomePage.jsx';
import EditProfilePage from './pages/EditProfilePage.jsx';
import NotFoundPage from './pages/NotFoundPage.jsx';
import { PRODUCT_NAME } from './config/brand.js';

// Register future pages here, with their own page component and title.
const routes = {
  '/': { component: LandingPage, title: 'Find your study people' },
  '/login': { component: LoginPage, title: 'Log in' },
  '/signup': { component: SignupPage, title: 'Sign up' },
  '/profile-setup/about': { component: SetupAboutPage, title: 'About you' },
  '/profile-setup/avatar': { component: SetupAvatarPage, title: 'Pick an avatar' },
  '/profile-setup': { component: ProfileSetupPage, title: 'Set up your profile' },
  '/match': { component: MatchPage, title: 'You matched' },
  '/chat': { component: ChatPage, title: 'Chat' },
  '/home': { component: HomePage, title: 'Home' },
  '/profile': { component: EditProfilePage, title: 'Edit profile' },
};
const notFoundRoute = { component: NotFoundPage, title: 'Page not found' };

export default function App() {
  const [session, setSession] = useState(null);
  const [matchNotice, setMatchNotice] = useState(null);
  const [presenceError, setPresenceError] = useState('');
  const [logoutError, setLogoutError] = useState('');
  const [loggingOut, setLoggingOut] = useState(false);
  const [setupDraft, setSetupDraft] = useState(null);
  const [match, setMatch] = useState(null);
  const handledChats = useRef(new Set());
  const [loading, setLoading] = useState(hasToken);
  const [loadError, setLoadError] = useState('');
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

  const goTo = useCallback((path) => {
    window.history.pushState({}, '', path);
    setPathname(path);
    window.scrollTo(0, 0);
  }, []);

  async function authenticate({ email, password, name }, signup) {
    const body = await authenticationBody({ email, password, name }, signup);
    const result = await request(signup ? '/register' : '/login', 'POST', body);
    setSession(null); setMatchNotice(null); setPresenceError('');
    setSetupDraft(null);
    setMatch(null);
    handledChats.current.clear();
    setLoadError('');
    setToken(result.token);
    setProfile(fromUser(result.user, { name }));
    goTo(result.user.classes?.length ? '/home' : '/profile-setup');
  }
  async function logout() {
    if (loggingOut) return;
    setLoggingOut(true); setLogoutError('');
    try {
      await goOffline();
      await request('/logout', 'POST');
      setSession(null); setMatchNotice(null); setToken(null); setProfile(null); setSetupDraft(null); setMatch(null); handledChats.current.clear();
      goTo('/login');
    } catch(error) { setLogoutError(error.message); }
    finally { setLoggingOut(false); }
  }
  async function saveProfile(draft) {
    const current = await request('/me');
    let user = await request('/me', 'PATCH', profileBody({ ...draft, studying: current.studying }));
    // Keep the server-returned picture URL and release the temporary file preview.
    user = await saveProfileMedia({ file: draft.photo }) || user;
    setProfile(fromUser(user, { ...draft, photo: null }));
  }
  useEffect(() => {
    if (hasToken()) request('/me').then(user => setProfile(fromUser(user))).catch(error => { if (hasToken()) setLoadError(error.message); }).finally(() => setLoading(false));
    const expired = () => { setSession(null); setMatchNotice(null); setProfile(null); setSetupDraft(null); setMatch(null); handledChats.current.clear(); setLoadError(''); goTo('/login'); };
    window.addEventListener('auth-expired', expired);
    return () => window.removeEventListener('auth-expired', expired);
  }, []);
  function onMatch(next) {
    if (!next.chatId || handledChats.current.has(next.chatId)) return;
    handledChats.current.add(next.chatId);
    setSession(null);
    setPresenceError('');
    if (pathname === '/home' && session) {
      setMatch(next); setMatchNotice(null); goTo('/chat');
    } else {
      setMatchNotice(next);
    }
  }
  useEffect(() => {
    const accepted = event => onMatch(event.detail);
    window.addEventListener('mutual-match', accepted);
    return () => window.removeEventListener('mutual-match', accepted);
  }, [pathname, session]);
  if (loading) return <main className="home-missing" role="status">Loading your profile...</main>;
  if (loadError) return <main className="home-missing"><p role="alert">{loadError}</p><button onClick={() => window.location.reload()}>Retry</button><button onClick={() => { setToken(null); setLoadError(''); goTo('/login'); }}>Go to login</button></main>;
  const sharedProps = { onLogout: logout, loggingOut, logoutError, setupDraft, onSetupDraft: setSetupDraft, match, navigate, goTo, onSignup: (values) => authenticate(values, true), onLogin: (values) => authenticate(values, false), profile, onProfileChange: saveProfile };
  return <>
    <MatchWatcher userId={profile?.id} onMatch={onMatch} />
    <PresenceHeartbeat session={session} onError={setPresenceError} />
    {matchNotice && <aside className="match-notice" aria-label="New match"><div role="status"><strong>You have a new study buddy!</strong><p>{matchNotice.name ? `You matched with ${matchNotice.name}.` : 'You both chose to study together.'}</p></div><button onClick={() => { setMatch(matchNotice); setMatchNotice(null); goTo('/chat'); }}>Open chat ↗</button><button className="match-notice-dismiss" aria-label="Dismiss match notification" onClick={() => setMatchNotice(null)}>×</button></aside>}
    {presenceError && <p role="alert" className="error">{presenceError}</p>}
    {profile && <div hidden={pathname !== '/home'}><HomePage {...sharedProps} session={session} setSession={setSession} active={pathname === '/home'} /></div>}
    {pathname !== '/home' ? <Page {...sharedProps} /> : !profile && <HomePage {...sharedProps} session={session} setSession={setSession} />}
  </>;
}

