import React, { useEffect, useRef, useState } from 'react';
import { request } from '../api.js';
import { mediaUrl } from '../api.js';
import AvatarArt from './AvatarArt.jsx';
import './ChatPanel.css';

export default function ChatPanel({ profile, initialChatId, onUnmatch }) {
  const [chats, setChats] = useState([]);
  const [matches, setMatches] = useState([]);
  const [chat, setChat] = useState(null);
  const [body, setBody] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(false);
  const sequence = useRef(0);
  function formatStudyTime(slot) {
    const day = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'][Math.floor(slot / 96)];
    const minutes = (slot % 96) * 15;
    const hour = Math.floor(minutes / 60);
    return `${day} ${hour % 12 || 12}:${String(minutes % 60).padStart(2, '0')} ${hour < 12 ? 'AM' : 'PM'}`;
  }
  function handleChatError(error, id) {
    if (error.code === 'not_chat_member' || error.status === 404) {
      const endedCurrentChat = chat?.id === id || initialChatId === id;
      setChat(previous => previous?.id === id ? null : previous);
      setChats(previous => previous.filter(item => item.id !== id));
      setMatches([]);
      setBody('');
      setError('This chat is no longer available. Chats expire after 24 hours; you can find another study buddy from Home.');
      if (endedCurrentChat) onUnmatch?.();
    } else setError(error.message);
  }
  async function refresh() {
    setLoading(true); setError('');
    try {
      const [c, m] = await Promise.all([request('/chats'), request('/matches')]);
      setChats(c.chats); setMatches(m.matches);
      setChat(previous => previous && c.chats.some(item => item.id === previous.id) ? previous : null);
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
    } catch (error) { if (current === sequence.current) handleChatError(error, id); } finally { if (current === sequence.current) setLoading(false); }
  }
  useEffect(() => { if (initialChatId) open(initialChatId); }, [initialChatId]);
  useEffect(() => {
    const receive = event => {
      const update = event.detail;
      if (update?.type !== 'chat.message' || !update.message?.id) return;
      setChats(previous => previous.map(item => item.id === update.chatId ? { ...item, latestMessage: update.message.body } : item));
      setChat(previous => {
        if (previous?.id !== update.chatId) return previous;
        const messages = new Map(previous.messages.map(message => [message.id, message]));
        messages.set(update.message.id, update.message);
        return { ...previous, messages: [...messages.values()].sort((a, b) => b.createdAt.localeCompare(a.createdAt) || b.id - a.id) };
      });
    };
    window.addEventListener('chat-message', receive);
    return () => window.removeEventListener('chat-message', receive);
  }, []);

  async function send(event) {
    event.preventDefault();
    if (busy || !body.trim()) return;
    setBusy(true); setError('');
    const id = chat.id;
    try {
      const message = await request(`/chats/${id}/messages`, 'POST', { message: body.trim() });
      setChat(previous => previous?.id === id ? { ...previous, messages: [message, ...previous.messages.filter(item => item.id !== message.id)] } : previous);
      setBody('');
    } catch (error) { handleChatError(error, id); } finally { setBusy(false); }
  }
  async function unmatch() {
    if (!chat || busy || !window.confirm(`Unmatch with ${chatUsername}? You can find a different study buddy afterward.`)) return;
    const id = chat.id;
    setBusy(true); setError('');
    try {
      await request(`/chats/${id}`, 'DELETE');
      sequence.current += 1;
      setChat(null);
      setChats(previous => previous.filter(item => item.id !== id));
      setMatches([]);
      setBody('');
      setError('You are unmatched and available to find another study buddy.');
      await onUnmatch?.();
    } catch (error) { handleChatError(error, id); } finally { setBusy(false); }
  }
  const chatUsername = chats.find(item => item.id === chat?.id)?.username || 'your study buddy';
  const buddyId = chats.find(item => item.id === chat?.id)?.userId;
  const buddy = chat?.buddy || matches.find(match => match.user.id === buddyId)?.user;
  return <section className="chat-shell"><header className="chat-topbar"><div><h2>{chat ? chatUsername : 'Messages'}</h2><p>{chat ? 'Study buddy chat' : 'Choose a conversation'}</p></div><button className="chat-action" disabled={loading || busy} onClick={refresh}>↻ Refresh</button></header>{loading && <p className="chat-status" role="status">Loading…</p>}{error && <p className="error chat-status" role="alert">{error}</p>}<div className="chat-picker">{chats.map(item => <button key={item.id} className={item.id === chat?.id ? 'active' : ''} disabled={busy || loading} onClick={() => open(item.id)}><span className="chat-list-avatar">{item.pictureUrl ? <img src={mediaUrl(item.pictureUrl)} alt="" /> : <AvatarArt avatar={item.avatar} />}</span><span>Chat with {item.username}<small>{item.latestMessage || 'No messages yet'}</small></span></button>)}</div>{!chats.length && !loading && <p className="chat-status">No chats yet. A mutual match will appear here.</p>}{chat && <><details className="chat-buddy" open><summary>About {buddy?.username || chatUsername}</summary>{buddy?.bio && <p>{buddy.bio}</p>}<p>{[buddy?.major, buddy?.gradYear && `Class of ${buddy.gradYear}`].filter(Boolean).join(' · ') || 'No additional profile details yet.'}</p>{buddy?.preferredStudyLocations?.length > 0 && <p>Study spots: {buddy.preferredStudyLocations.join(', ')}</p>}{buddy?.studyTimes?.length > 0 && <p>Preferred study times: {buddy.studyTimes.map(formatStudyTime).join(', ')}</p>}</details><div className="chat-conversation"><p className="chat-date">Today</p>{[...chat.messages].reverse().map(message => <div className={`chat-bubble-row ${message.senderUserId === profile.id ? 'mine' : ''}`} key={message.id}><div className="chat-bubble" style={{ whiteSpace: 'pre-wrap' }}>{message.body}<time>{new Date(message.createdAt).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })}</time></div></div>)}{chat.nextCursor && <button className="chat-action" disabled={busy || loading} onClick={() => open(chat.id, true)}>Load older messages</button>}</div><form className="chat-composer" onSubmit={send}><label className="sr-only" htmlFor="chat-message">Message</label><textarea id="chat-message" disabled={busy} value={body} onChange={event => setBody(event.target.value)} onKeyDown={event => { if (event.key === 'Enter' && !event.shiftKey) { event.preventDefault(); event.currentTarget.form?.requestSubmit(); } }} maxLength={2000} rows={1} placeholder="iMessage" /><button className="chat-send" aria-label="Send message" disabled={busy || !body.trim()}>{busy ? '…' : '↑'}</button></form><div className="chat-picker"><button className="chat-action" disabled={busy || loading} onClick={() => open(chat.id)}>Refresh messages</button><button className="chat-action chat-unmatch" disabled={busy || loading} onClick={unmatch}>Unmatch</button></div></>}</section>;
}
