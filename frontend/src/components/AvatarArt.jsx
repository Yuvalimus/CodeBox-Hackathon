import React, { useId } from 'react';

// Each quadrant contains one portrait. Clip before scaling to prevent neighboring
// portraits appearing in wide discovery cards. Keep IDs stable for profile choices.
const positions = { sage: [0, 0], blue: [1, 0], peach: [0, 1], lavender: [1, 1] };
export default function AvatarArt({ avatar = 'sage', label = 'Avatar' }) {
  const clipId = useId();
  const [x, y] = positions[avatar] || positions.sage;
  return <svg viewBox="0 0 1 1" preserveAspectRatio="xMidYMin slice" role="img" aria-label={label} style={{ display: 'block', width: '100%', height: '100%', overflow: 'hidden' }}>
    <defs><clipPath id={clipId}><rect width="1" height="1" /></clipPath></defs>
    <g clipPath={`url(#${clipId})`}><image href="/avatars/avatars-hq.png" x={-x} y={-y} width="2" height="2" /></g>
  </svg>;
}
