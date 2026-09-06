import { useEffect } from 'react';
import { heartbeat, HEARTBEAT_INTERVAL_MS } from '../presence.js';

// Owned by App so queue presence remains fresh across every authenticated page.
export default function PresenceHeartbeat({ active, onError }) {
  useEffect(() => {
    if (!active) return;
    const controller = new AbortController();
    let timer;
    async function tick() {
      try { await heartbeat(controller.signal); }
      catch (error) { if (!controller.signal.aborted) onError(error.message); }
      if (!controller.signal.aborted) timer = setTimeout(tick, HEARTBEAT_INTERVAL_MS);
    }
    tick();
    return () => { controller.abort(); clearTimeout(timer); };
  }, [active, onError]);
  return null;
}
