import { useState, type ReactNode } from 'react'
import type { AuthUser, LoginResponse } from '../api/auth/authTypes'
import { profileApi } from '../api/profile/profileApi'
import type { UserProfile } from '../api/profile/profileTypes'
import { authStorage } from '../services/authStorage'
import { AuthContext } from './AuthContext'

type AuthProviderProps = {
  children: ReactNode
}

function restoreStoredUser(): AuthUser | null {
  const accessToken = authStorage.getAccessToken()
  const user = authStorage.getUser()

  if (!accessToken || !user) {
    authStorage.clearAuth()
    return null
  }

  return user
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<AuthUser | null>(restoreStoredUser)
  const [profile, setProfile] = useState<UserProfile | null>(() => authStorage.getProfile())

  async function login(loginResponse: LoginResponse) {
    authStorage.saveAuth(loginResponse)

    try {
      const loadedProfile = await profileApi.getCurrentProfile(loginResponse.accessToken)
      authStorage.saveProfile(loadedProfile)
      setProfile(loadedProfile)
    } catch {
      setProfile(null)
    }

    setUser(loginResponse.user)
  }

  function logout() {
    authStorage.clearAuth()
    setUser(null)
    setProfile(null)
  }

  return (
    <AuthContext.Provider
      value={{
        user,
        profile,
        isAuthenticated: user !== null,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}
