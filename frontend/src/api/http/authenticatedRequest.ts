import { authStorage } from '../../services/authStorage'

export class AuthenticationRequiredError extends Error {
  constructor() {
    super('Authentication is required for this request')
    this.name = 'AuthenticationRequiredError'
  }
}

export async function authenticatedRequest(
  input: RequestInfo | URL,
  init: RequestInit = {},
): Promise<Response> {
  const accessToken = authStorage.getAccessToken()
  if (!accessToken) {
    throw new AuthenticationRequiredError()
  }

  const headers = new Headers(init.headers)
  headers.set('Authorization', `Bearer ${accessToken}`)

  return fetch(input, {
    ...init,
    headers,
  })
}
