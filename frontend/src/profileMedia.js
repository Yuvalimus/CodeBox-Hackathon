// Backend TODO: provide a multipart photo upload endpoint and persist avatarId
// on the user record. Return the updated user (including its HTTPS pictureUrl)
// from that endpoint and expose avatarId in GET /me and public profiles.
// Never serialize File/blob URLs or profile data into cookies/browser storage.
export async function saveProfileMedia(_selection) {
  // Blank API integration until the backend defines the route and payload:
  // return request(...);
  return { implemented: false };
}
