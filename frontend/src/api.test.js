import test from 'node:test';
import assert from 'node:assert/strict';

const store = new Map();
globalThis.sessionStorage = { getItem: key => store.get(key), setItem: (key, value) => store.set(key, value), removeItem: key => store.delete(key) };
globalThis.window = new EventTarget();
const { request, setToken, hasToken, usernameForEmail, profileBody, fromUser } = await import('./api.js');

test('email login maps to a stable valid backend username', async () => {
  const username = await usernameForEmail(' Student@CalPoly.edu ');
  assert.equal(username, await usernameForEmail('student@calpoly.edu'));
  assert.match(username, /^[a-z0-9_-]{3,32}$/);
  assert.notEqual(username, await usernameForEmail('other@calpoly.edu'));
});
test('profile writes exclude unsupported fields and keep studying within classes', () => {
  assert.deepEqual(profileBody({ name: 'Name', classes: ['CSC 2001'], studying: ['CSC 2001', 'MATH 2001'], major: '', bio: '', year: 'First', photo: {}, avatar: 'blue' }), { classes: ['CSC 2001'], studying: ['CSC 2001'], major: '', bio: '', pictureUrl: null });
  assert.equal(fromUser({ username: 'student', classes: [] }).name, 'student');
});
test('API sends bearer JSON requests, handles 204 and server errors', async () => {
  setToken('test-token');
  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/looking-now');
    assert.equal(options.method, 'PUT');
    assert.equal(options.headers.Authorization, 'Bearer test-token');
    assert.deepEqual(JSON.parse(options.body), { subjects: ['CSC 2001'] });
    return new Response(null, { status: 204 });
  };
  assert.equal(await request('/looking-now', 'PUT', { subjects: ['CSC 2001'] }), null);
  globalThis.fetch = async () => new Response(JSON.stringify({ error: { message: 'Duplicate account' } }), { status: 409 });
  await assert.rejects(request('/register', 'POST', {}), /Duplicate account/);
  globalThis.fetch = async () => new Response('{}', { status: 401 });
  await assert.rejects(request('/me'));
  assert.equal(hasToken(), false);
  globalThis.fetch = async () => { throw new Error('network'); };
  await assert.rejects(request('/me'), /Cannot reach the API/);
});
