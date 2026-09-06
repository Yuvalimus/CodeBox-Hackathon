import { request } from './api.js';

export const HEARTBEAT_INTERVAL_MS = 30000;
export async function heartbeat(_signal) {
  return request('/queue/heartbeat', 'POST');
}
export async function goOffline() {
  return request('/queue', 'DELETE');
}
