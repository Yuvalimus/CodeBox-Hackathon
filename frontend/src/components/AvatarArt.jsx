import React from 'react';

// Display only the artwork rectangles from the supplied reference, without its UI.
const crops = { sage: [29, 115], blue: [165, 115], peach: [29, 261], lavender: [165, 261] };
export default function AvatarArt({ avatar = 'sage', label = 'Avatar' }) {
  const [x, y] = crops[avatar] || crops.sage;
  return <svg viewBox={`${x} ${y} 120 131`} role="img" aria-label={label} style={{ display: 'block', width: '100%', height: '100%' }}><image href="/avatars/avatar-reference.png" width="309" height="496" /></svg>;
}
