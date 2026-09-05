import test from 'node:test';
import assert from 'node:assert/strict';

const store = new Map();
globalThis.sessionStorage = { getItem: key => store.get(key), setItem: (key, value) => store.set(key, value), removeItem: key => store.delete(key) };
globalThis.window = new EventTarget();
const { request, setToken, hasToken, authenticationBody, profileBody, fromUser } = await import('./api.js');

test('documented registration uses a display name and login uses email', async () => {
  const input = { name: "María O'Connor", email: ' Student@CalPoly.edu ', password: ' a long test password ' };
  const signup = await authenticationBody(input, true);
  const login = await authenticationBody(input, false);
  assert.equal(signup.username, input.name);
  assert.equal(login.email, 'student@calpoly.edu');
  assert.equal('username' in login, false);
  assert.equal(signup.email, 'student@calpoly.edu');
  assert.equal(login.password, 'a long test password');
  assert.equal('name' in signup, false);
});

test('profile writes exclude unsupported fields and keep studying within classes', () => {
  assert.deepEqual(profileBody({ name: 'Name', classes: ['CSC 2001'], studying: ['CSC 2001', 'MATH 2001'], major: '', bio: '', year: 'First', photo: {}, avatar: 'blue' }), { username: 'Name', classes: ['CSC 2001'], studying: ['CSC 2001'], major: '', bio: '', pictureUrl: null });
  assert.equal(fromUser({ username: 'student', classes: [] }).name, 'student');
  assert.equal(fromUser({ username: 'Saved name' }, { name: 'Old local name' }).name, 'Saved name');
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

test('matching lifecycle uses documented responses without inventing acceptance', async () => {
  setToken('matching-test-token');
  const calls = [
    ['/recommendations/12/accept', 'POST', 200, { decision: 'accepted', matched: false }],
    ['/recommendations/13/accept', 'POST', 200, { decision: 'accepted', matched: true, match: { id: 4 }, chat: { id: 9 } }],
    ['/looking-now', 'DELETE', 204, null],
    ['/chats/9', 'GET', 200, { id: 9, messages: [], nextCursor: null }],
    ['/chats/9/messages', 'POST', 201, { id: 31, chatId: 9, senderUserId: 1, body: 'Hello' }],
    ['/logout', 'POST', 204, null],
  ];
  globalThis.fetch = async (url, options) => {
    const [path, method, status, body] = calls.shift();
    assert.equal(url, `/api${path}`);
    assert.equal(options.method, method);
    assert.equal(options.headers.Authorization, 'Bearer matching-test-token');
    if (path.endsWith('/messages')) assert.deepEqual(JSON.parse(options.body), { message: 'Hello' });
    return new Response(status === 204 ? null : JSON.stringify(body), { status });
  };
  assert.equal((await request('/recommendations/12/accept', 'POST', {})).matched, false);
  const match = await request('/recommendations/13/accept', 'POST', {});
  assert.equal(match.matched, true);
  assert.equal(match.chat.id, 9);
  await request('/looking-now', 'DELETE');
  assert.equal((await request('/chats/9')).id, 9);
  assert.equal((await request('/chats/9/messages', 'POST', { message: 'Hello' })).body, 'Hello');
  await request('/logout', 'POST');
  setToken(null);
  assert.equal(calls.length, 0);
});
