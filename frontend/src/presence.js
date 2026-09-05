import { request } from './api.js';

export const HEARTBEAT_INTERVAL_MS = 30000; // Confirm cadence with backend.
export async function heartbeat(_session, _signal) {
  // TODO: call the authenticated heartbeat endpoint once its contract exists.
  // It must refresh existing presence only, never rejoin the pool after a match.
  // return request(...);
}
export async function goOffline() {
  // Existing endpoint works today. Replace here if the new offline API changes.
  return request('/looking-now', 'DELETE');
}
