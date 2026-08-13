import type { UserProfile } from './profileTypes'

async function getCurrentProfile(accessToken: string): Promise<UserProfile> {
  const response = await fetch('/api/users/me', {
    method: 'GET',
    headers: {
      Accept: 'application/json',
      Authorization: `Bearer ${accessToken}`,
    },
  })

  if (!response.ok) {
    throw new Error(`Profile request failed with status ${response.status}`)
  }

  return response.json() as Promise<UserProfile>
}

export const profileApi = {
  getCurrentProfile,
}
