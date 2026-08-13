import type { RegisterRequest, RegisterResponse } from './authTypes'

type BackendErrorResponse = {
  message?: unknown
}

export class AuthApiError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'AuthApiError'
  }
}

async function readErrorMessage(response: Response): Promise<string> {
  if (response.status >= 500) {
    return 'Registration is temporarily unavailable. Please try again.'
  }

  try {
    const errorResponse = (await response.json()) as BackendErrorResponse
    if (typeof errorResponse.message === 'string' && errorResponse.message.trim()) {
      return errorResponse.message
    }
  } catch {
    // Fall back to a safe message when the response is empty or is not JSON.
  }

  return 'Registration failed. Please check your details and try again.'
}

async function register(request: RegisterRequest): Promise<RegisterResponse> {
  let response: Response

  try {
    response = await fetch('/api/auth/register', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
      },
      body: JSON.stringify(request),
    })
  } catch {
    throw new AuthApiError('Unable to reach the registration service. Please try again.')
  }

  if (!response.ok) {
    throw new AuthApiError(await readErrorMessage(response))
  }

  return response.json() as Promise<RegisterResponse>
}

export const authApi = {
  register,
}
