import type { AuthUser, LoginResponse } from '../api/auth/authTypes'
import type { UserProfile } from '../api/profile/profileTypes'

const ACCESS_TOKEN_KEY = 'casino.accessToken'
const REFRESH_TOKEN_KEY = 'casino.refreshToken'
const USER_KEY = 'casino.user'
const PROFILE_KEY = 'casino.profile'

function saveAuth(loginResponse: LoginResponse) {
  try {
    localStorage.setItem(ACCESS_TOKEN_KEY, loginResponse.accessToken)
    localStorage.setItem(REFRESH_TOKEN_KEY, loginResponse.refreshToken)
    localStorage.setItem(USER_KEY, JSON.stringify(loginResponse.user))
  } catch (error) {
    clearAuth()
    throw error
  }
}

function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_KEY)
}

function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

function isAuthUser(value: unknown): value is AuthUser {
  if (typeof value !== 'object' || value === null) return false

  const user = value as Record<string, unknown>
  return (
    typeof user.id === 'string' &&
    Boolean(user.id) &&
    typeof user.email === 'string' &&
    Boolean(user.email) &&
    typeof user.role === 'string' &&
    Boolean(user.role) &&
    typeof user.status === 'string' &&
    Boolean(user.status)
  )
}

function getUser(): AuthUser | null {
  const storedUser = localStorage.getItem(USER_KEY)
  if (!storedUser) return null

  try {
    const user: unknown = JSON.parse(storedUser)
    if (isAuthUser(user)) return user
  } catch {
    // Invalid stored data is removed below.
  }

  localStorage.removeItem(USER_KEY)
  return null
}

function saveProfile(profile: UserProfile) {
  localStorage.setItem(PROFILE_KEY, JSON.stringify(profile))
}

function getProfile(): UserProfile | null {
  const storedProfile = localStorage.getItem(PROFILE_KEY)
  if (!storedProfile) return null

  try {
    const profile: unknown = JSON.parse(storedProfile)
    if (
      typeof profile === 'object' &&
      profile !== null &&
      typeof (profile as Record<string, unknown>).username === 'string' &&
      Boolean((profile as Record<string, unknown>).username)
    ) {
      return profile as UserProfile
    }
  } catch {
    // Invalid stored data is removed below.
  }

  localStorage.removeItem(PROFILE_KEY)
  return null
}

function clearAuth() {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem(PROFILE_KEY)
}

export const authStorage = {
  saveAuth,
  getAccessToken,
  getRefreshToken,
  getUser,
  saveProfile,
  getProfile,
  clearAuth,
}
