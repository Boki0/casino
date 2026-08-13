import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse,
} from './authTypes'

type BackendErrorResponse = {
  message?: unknown
}

export class AuthApiError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'AuthApiError'
  }
}

async function readErrorMessage(
  response: Response,
  serviceUnavailableMessage: string,
  fallbackMessage: string,
): Promise<string> {
  if (response.status >= 500) {
    return serviceUnavailableMessage
  }

  try {
    const errorResponse = (await response.json()) as BackendErrorResponse
    if (typeof errorResponse.message === 'string' && errorResponse.message.trim()) {
      return errorResponse.message
    }
  } catch {
    // Fall back to a safe message when the response is empty or is not JSON.
  }

  return fallbackMessage
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
    throw new AuthApiError(
      await readErrorMessage(
        response,
        'Registration is temporarily unavailable. Please try again.',
        'Registration failed. Please check your details and try again.',
      ),
    )
  }

  return response.json() as Promise<RegisterResponse>
}

async function login(request: LoginRequest): Promise<LoginResponse> {
  let response: Response

  try {
    response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
      },
      body: JSON.stringify(request),
    })
  } catch {
    throw new AuthApiError('Unable to reach the login service. Please try again.')
  }

  if (!response.ok) {
    throw new AuthApiError(
      await readErrorMessage(
        response,
        'Login is temporarily unavailable. Please try again.',
        'Login failed. Please check your email and password.',
      ),
    )
  }

  return response.json() as Promise<LoginResponse>
}

export const authApi = {
  register,
  login,
}
