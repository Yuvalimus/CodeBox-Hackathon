import { request } from './api.js';

export async function startStudySession({ classes, location, comments, durationMinutes, active }) {
  // The API stores exact minutes; the UI's 2+ hours choice maps to 120.
  try {
  await request('/me', 'PATCH', {
    studying: classes,
    preferredStudyLocations: [location.trim() || 'Kennedy Library'],
    comments: comments.trim(),
    studyDurationMinutes: durationMinutes,
  });
  } catch (error) {
    throw new Error(`Could not save your study preferences: ${error.message}`);
  }
  if (active) {
    try {
      return await request('/queue', 'POST');
    } catch (error) {
      throw new Error(`Could not start looking: ${error.message}`);
    }
  }
}
