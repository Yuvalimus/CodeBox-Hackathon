import { request } from './api.js';

export async function saveProfileMedia({ file }) {
  if (!file) return null;
  const body = new FormData();
  body.append('file', file);
  return request('/me/picture', 'POST', body);
}
// Photo bytes are uploaded to backend storage; only its URL belongs in the DB.
