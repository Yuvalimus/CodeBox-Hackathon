import { useEffect } from 'react';
import { heartbeat, HEARTBEAT_INTERVAL_MS } from '../presence.js';

// Owned by App so profile/chat navigation never stops the looking heartbeat.
export default function PresenceHeartbeat({ session, onError }) {
  useEffect(() => {
    if (!session || session.testDeck) return;
    const controller = new AbortController();
    let timer;
    async function tick() {
      try { await heartbeat(session, controller.signal); }
      catch (error) { if (!controller.signal.aborted) onError(error.message); }
      if (!controller.signal.aborted) timer = setTimeout(tick, HEARTBEAT_INTERVAL_MS);
    }
    tick();
    return () => { controller.abort(); clearTimeout(timer); };
  }, [session, onError]);
  return null;
}
