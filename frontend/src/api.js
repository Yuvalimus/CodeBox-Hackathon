const base = import.meta.env?.VITE_API_BASE_URL || '/api';
let token = sessionStorage.getItem('study-token');
export function setToken(value) {
  token = value;
  if (value) sessionStorage.setItem('study-token', value);
  else sessionStorage.removeItem('study-token');
}
export const hasToken = () => Boolean(token);
export async function request(path, method = 'GET', body) {
  const requestToken = token;
  let response;
  try {
    response = await fetch(`${base}${path}`, { method, headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}), ...(body === undefined ? {} : { 'Content-Type': 'application/json' }) }, body: body === undefined ? undefined : JSON.stringify(body), signal: AbortSignal.timeout(15000) });
  } catch { throw new Error('Cannot reach the API. Check that the backend is running and try again.'); }
  const text = await response.text();
  let data;
  try { data = text ? JSON.parse(text) : null; } catch { throw new Error('The API returned an unexpected response. Check your API URL.'); }
  if (!response.ok) {
    if (response.status === 401 && path !== '/login' && path !== '/register' && requestToken === token) { setToken(null); window.dispatchEvent(new Event('auth-expired')); }
    throw new Error(data?.error?.message || `Request failed (${response.status}). Please try again.`);
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
  return { ...user, name: user.username || local.name, year: local.year || '', avatar: local.avatar || 'sage', photo: local.photo || null, classes: user.classes || [], major: user.major || '', bio: user.bio || '' };
}
export function profileBody(profile) {
  return { username: profile.name.trim(), classes: profile.classes, studying: (profile.studying || []).filter((course) => profile.classes.includes(course)), major: profile.major, bio: profile.bio, pictureUrl: profile.pictureUrl || null };
}
