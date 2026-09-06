const base = (import.meta.env?.VITE_API_BASE_URL || 'https://study.happyxd.dev/api/').replace(/\/+$/, '');
// Vite proxies this path during development, keeping browser API requests same-origin.
const apiBase = (import.meta.env?.VITE_API_BASE_URL || (import.meta.env?.DEV ? '/api' : 'https://study.happyxd.dev/api/')).replace(/\/+$/, '');
let token = sessionStorage.getItem('study-token');
const YEAR_OFFSETS = { First: 4, Second: 3, Third: 2, Fourth: 1, 'Fifth+': 0 };
const currentYear = () => new Date().getFullYear();
export const gradYearFor = (year) => Object.hasOwn(YEAR_OFFSETS, year) ? currentYear() + YEAR_OFFSETS[year] : null;
export const yearForGradYear = (gradYear) => Object.entries(YEAR_OFFSETS).find(([, offset]) => currentYear() + offset === gradYear)?.[0] || '';
export function setToken(value) {
  token = value;
  if (value) sessionStorage.setItem('study-token', value);
  else sessionStorage.removeItem('study-token');
}
export const hasToken = () => Boolean(token);
export async function chatEventsUrl() {
  const { ticket } = await request('/ws/chat-ticket', 'POST');
  const endpoint = new URL(apiBase + '/ws/chat', window.location.origin);
  endpoint.protocol = endpoint.protocol === 'https:' ? 'wss:' : 'ws:';
  endpoint.searchParams.set('ticket', ticket);
  return endpoint.href;
}
export async function request(path, method = 'GET', body) {
  const base = apiBase;
  const requestToken = token;
  const multipart = body instanceof FormData;
  let response;
  try {
    response = await fetch(`${base}${path}`, { method, headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}), ...(body === undefined || multipart ? {} : { 'Content-Type': 'application/json' }) }, body: body === undefined ? undefined : multipart ? body : JSON.stringify(body), signal: AbortSignal.timeout(15000) });
  } catch { throw new Error('Cannot reach the API. Check that the backend is running and try again.'); }
  const text = await response.text();
  let data;
  try { data = text ? JSON.parse(text) : null; } catch { throw new Error('The API returned an unexpected response. Check your API URL.'); }
  if (!response.ok) {
    if (response.status === 401 && path !== '/login' && path !== '/register' && requestToken === token) { setToken(null); window.dispatchEvent(new Event('auth-expired')); }
    const error = new Error(data?.error?.message || `Request failed (${response.status}). Please try again.`); error.status = response.status; error.code = data?.error?.code; throw error;
  }
  return data;
}

export async function authenticationBody({ email, password, name }, signup) {
  if (signup) {
    return {
      username: name.trim(),
      password: password.trim(),
      email: email.trim().toLowerCase(),
    };
  }

  return {
     email: email.trim().toLowerCase(),
     password: password.trim(),
  };
}
export function fromUser(user, local = {}) {
  return { ...user, pictureUrl: mediaUrl(user.pictureUrl), name: user.username || local.name, year: local.year || yearForGradYear(user.gradYear), avatar: user.avatar || local.avatar || 'sage', photo: local.photo || null, classes: user.classes || [], studyTimes: user.studyTimes || [], major: user.major || '', bio: user.bio || '' };
}
export function profileBody(profile) {
  return { username: profile.name.trim(), classes: profile.classes, studying: (profile.studying || []).filter((course) => profile.classes.includes(course)), studyTimes: profile.studyTimes || [], major: profile.major, bio: profile.bio, gradYear: gradYearFor(profile.year), avatar: profile.avatar || 'sage', ...(!profile.pictureUrl ? { pictureUrl: null } : {}) };
}

export function mediaUrl(value) {
  const base = apiBase;
  if (!value) return null;
  if (value.startsWith('/uploads/')) {
    return /^https?:\/\//.test(base) ? new URL(value, base).href : `${base.replace(/\/$/, '')}${value}`;
  }
  return value;
}

