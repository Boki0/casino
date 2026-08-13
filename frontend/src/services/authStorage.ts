import type { AuthUser, LoginResponse } from '../api/auth/authTypes'

const ACCESS_TOKEN_KEY = 'casino.accessToken'
const REFRESH_TOKEN_KEY = 'casino.refreshToken'
const USER_KEY = 'casino.user'

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

function getUser(): AuthUser | null {
  const storedUser = localStorage.getItem(USER_KEY)
  if (!storedUser) return null

  try {
    return JSON.parse(storedUser) as AuthUser
  } catch {
    localStorage.removeItem(USER_KEY)
    return null
  }
}

function clearAuth() {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export const authStorage = {
  saveAuth,
  getAccessToken,
  getRefreshToken,
  getUser,
  clearAuth,
}
