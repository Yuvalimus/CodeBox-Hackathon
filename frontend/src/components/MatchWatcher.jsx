import { useEffect, useRef } from 'react';
import { request } from '../api.js';

// New chats are server-confirmed mutual matches. Never infer acceptance from a swipe.
export default function MatchWatcher({ userId, onMatch }) {
  const callback = useRef(onMatch);
  callback.current = onMatch;
  useEffect(() => {
    if (!userId) return;
    let stopped = false;
    let timeout;
    let known = null;
    const key = `known-chats:${userId}`;
    try { const saved = JSON.parse(sessionStorage.getItem(key)); if (Array.isArray(saved)) known = new Set(saved); } catch { /* Establish a baseline from the server. */ }
    async function poll() {
      try {
        const { chats } = await request('/chats');
        if (stopped) return;
        const fresh = known ? chats.filter(chat => !known.has(chat.id)) : [];
        known = new Set(chats.map(chat => chat.id));
        sessionStorage.setItem(key, JSON.stringify([...known]));
        if (fresh.length) callback.current({ chatId: fresh[0].id });
      } catch { /* Polling retries; interactive pages expose request errors. */ }
      if (!stopped) timeout = setTimeout(poll, 5000);
    }
    poll();
    return () => { stopped = true; clearTimeout(timeout); };
  }, [userId]);
  return null;
}
