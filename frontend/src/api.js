const base = (import.meta.env?.VITE_API_BASE_URL || 'https://study.happyxd.dev/api/').replace(/\/+$/, '');
let token = sessionStorage.getItem('study-token');
export function setToken(value) {
  token = value;
  if (value) sessionStorage.setItem('study-token', value);
  else sessionStorage.removeItem('study-token');
}
export const hasToken = () => Boolean(token);
export async function request(path, method = 'GET', body) {
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
  return { ...user, pictureUrl: mediaUrl(user.pictureUrl), name: user.username || local.name, year: local.year || '', avatar: local.avatar || 'sage', photo: local.photo || null, classes: user.classes || [], major: user.major || '', bio: user.bio || '' };
}
export function profileBody(profile) {
  return { username: profile.name.trim(), classes: profile.classes, studying: (profile.studying || []).filter((course) => profile.classes.includes(course)), major: profile.major, bio: profile.bio, ...(!profile.pictureUrl ? { pictureUrl: null } : {}) };
}

export function mediaUrl(value) {
  if (!value) return null;
  if (value.startsWith('/uploads/')) {
    return /^https?:\/\//.test(base) ? new URL(value, base).href : `${base.replace(/\/$/, '')}${value}`;
  }
  return value;
}

