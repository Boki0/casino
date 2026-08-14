import type { UserProfile } from './profileTypes'
import { authenticatedRequest } from '../http/authenticatedRequest'

async function getCurrentProfile(): Promise<UserProfile> {
  const response = await authenticatedRequest('/api/users/me', {
    method: 'GET',
    headers: {
      Accept: 'application/json',
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
