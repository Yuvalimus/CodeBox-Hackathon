import React, { useEffect, useRef, useState } from 'react';
import { request } from '../api.js';

export default function ChatPanel({ profile, initialChatId }) {
  const [chats, setChats] = useState([]);
  const [matches, setMatches] = useState([]);
  const [chat, setChat] = useState(null);
  const [body, setBody] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(false);
  const sequence = useRef(0);
  async function refresh() {
    setLoading(true); setError('');
    try {
      const [c, m] = await Promise.all([request('/chats'), request('/matches')]);
      setChats(c.chats); setMatches(m.matches);
    } catch (error) { setError(error.message); }
    finally { setLoading(false); }
  }
  useEffect(() => { refresh(); window.addEventListener('matches-updated', refresh); return () => window.removeEventListener('matches-updated', refresh); }, []);
  async function open(id, older = false) {
    const current = ++sequence.current;
    setError(''); setLoading(true);
    try {
      const data = await request(`/chats/${id}${older && chat?.nextCursor ? `?cursor=${encodeURIComponent(chat.nextCursor)}` : ''}`);
      if (current !== sequence.current) return;
      setChat(previous => older ? { ...data, messages: [...previous.messages, ...data.messages] } : data);
      if (!older) setBody('');
    } catch (error) { setError(error.message); }
    finally { if (current === sequence.current) setLoading(false); }
  }
  useEffect(() => { if (initialChatId) open(initialChatId); }, [initialChatId]);
  useEffect(() => {
    if (!chat?.id) return;
    let active = true;
    let timer;
    const id = chat.id;
    async function poll() {
      try {
        const data = await request('/chats/' + id);
        if (active) setChat(previous => {
          if (previous?.id !== id) return previous;
          const messages = new Map(previous.messages.map(message => [message.id, message]));
          data.messages.forEach(message => messages.set(message.id, message));
          return { ...previous, messages: [...messages.values()].sort((a,b) => b.createdAt.localeCompare(a.createdAt) || b.id - a.id) };
        });
      } catch { /* Manual refresh displays errors; retry polling. */ }
      if (active) timer = setTimeout(poll, 5000);
    }
    timer = setTimeout(poll, 5000);
    return () => { active = false; clearTimeout(timer); };
  }, [chat?.id]);
  async function send(event) {
    event.preventDefault();
    if (busy || !body.trim()) return;
    setBusy(true); setError('');
    const id = chat.id;
    try {
      const message = await request(`/chats/${id}/messages`, 'POST', { message: body.trim() });
      setChat(previous => previous?.id === id ? { ...previous, messages: [message, ...previous.messages] } : previous);
      setBody('');
    } catch (error) { setError(error.message); }
    finally { setBusy(false); }
  }
  return <section className="home-session"><h2>Matches & chats</h2><button className="home-secondary" disabled={loading || busy} onClick={refresh}>Refresh matches</button>{loading && <p role="status">Loading…</p>}{error && <p className="error" role="alert">{error}</p>}<p>{matches.length ? `Matched with ${matches.map(match => match.user.username).join(', ')}` : 'No accepted matches yet. A mutual request opens a chat.'}</p><div className="home-form-actions">{chats.map(item => <button className="home-secondary" key={item.id} disabled={busy || loading} onClick={() => open(item.id)}>Chat #{item.id}{item.latestMessage ? ` · ${item.latestMessage.slice(0, 40)}` : ''}</button>)}</div>{chat && <div><h3>Chat #{chat.id}</h3><button disabled={busy || loading} onClick={() => open(chat.id)}>Refresh messages</button>{chat.nextCursor && <button disabled={busy || loading} onClick={() => open(chat.id, true)}>Load older messages</button>}<ol aria-label="Messages">{[...chat.messages].reverse().map(message => <li key={message.id} style={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere', margin: '16px 0' }}><strong>{message.senderUserId === profile.id ? 'You' : 'Study buddy'}: </strong>{message.body}<small style={{ display: 'block' }}>{new Date(message.createdAt).toLocaleString()}</small></li>)}</ol><form onSubmit={send}><label htmlFor="chat-message">Message</label><textarea id="chat-message" value={body} onChange={event => setBody(event.target.value)} maxLength={2000} rows={3} style={{ width: '100%', font: 'inherit' }} /><button className="home-primary" disabled={busy || !body.trim()}>{busy ? 'Sending…' : 'Send message'}</button></form></div>}</section>;
}
