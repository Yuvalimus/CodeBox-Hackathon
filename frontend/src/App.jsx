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
      await request('/looking-now', 'DELETE');
      await request('/logout', 'POST');
      setToken(null); setProfile(null); setSetupDraft(null); setMatch(null); handledChats.current.clear();
      goTo('/login');
    } catch(error) { setLogoutError(error.message); }
    finally { setLoggingOut(false); }
  }
  async function saveProfile(draft) {
    const current = await request('/me');
    const user = await request('/me', 'PATCH', profileBody({ ...draft, studying: current.studying }));
    // Media stays a temporary preview until the database API is implemented.
    await saveProfileMedia({ avatarId: draft.avatar, file: draft.photo });
    setProfile(fromUser(user, draft));
  }
  useEffect(() => {
    if (hasToken()) request('/me').then(user => setProfile(fromUser(user))).catch(error => { if (hasToken()) setLoadError(error.message); }).finally(() => setLoading(false));
    const expired = () => { setProfile(null); setSetupDraft(null); setMatch(null); handledChats.current.clear(); setLoadError(''); goTo('/login'); };
    window.addEventListener('auth-expired', expired);
    return () => window.removeEventListener('auth-expired', expired);
  }, []);
  function onMatch(next) {
    if (!next.chatId || handledChats.current.has(next.chatId)) return;
    handledChats.current.add(next.chatId);
    setMatch(next);
    goTo('/match');
  }
  useEffect(() => {
    const accepted = event => onMatch(event.detail);
    window.addEventListener('mutual-match', accepted);
    return () => window.removeEventListener('mutual-match', accepted);
  }, []);
  if (loading) return <main className="home-missing" role="status">Loading your profile...</main>;
  if (loadError) return <main className="home-missing"><p role="alert">{loadError}</p><button onClick={() => window.location.reload()}>Retry</button><button onClick={() => { setToken(null); setLoadError(''); goTo('/login'); }}>Go to login</button></main>;
  return <><MatchWatcher userId={profile?.id} onMatch={onMatch} /><Page onLogout={logout} loggingOut={loggingOut} logoutError={logoutError} setupDraft={setupDraft} onSetupDraft={setSetupDraft} match={match} navigate={navigate} goTo={goTo} onSignup={(values) => authenticate(values, true)} onLogin={(values) => authenticate(values, false)} profile={profile} onProfileChange={saveProfile} /></>;
}
