import { useEffect, useRef } from 'react';
import { request } from '../api.js';

// Chat-message sockets do not announce mutual matches. Detect new server chats
// across routes so the recipient also receives the match, even before a message.
export default function MatchWatcher({ userId, onMatch }) {
  const callback = useRef(onMatch);
  callback.current = onMatch;
  useEffect(() => {
    if (!userId) return;
    let stopped = false;
    let timer;
    let known;
    async function poll() {
      try {
        const { chats } = await request('/chats');
        if (stopped) return;
        const fresh = known ? chats.filter(chat => !known.has(chat.id)) : [];
        known = new Set(chats.map(chat => chat.id));
        for (const chat of fresh) callback.current({ chatId: chat.id, name: chat.username });
      } catch { /* Retry; interactive chat views report API failures. */ }
      if (!stopped) timer = setTimeout(poll, 5000);
    }
    poll();
    return () => { stopped = true; clearTimeout(timer); };
  }, [userId]);
  return null;
}
