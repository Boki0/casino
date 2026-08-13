import { createContext } from 'react'
import type { AuthUser, LoginResponse } from '../api/auth/authTypes'
import type { UserProfile } from '../api/profile/profileTypes'

export type AuthContextValue = {
  user: AuthUser | null
  profile: UserProfile | null
  isAuthenticated: boolean
  login: (loginResponse: LoginResponse) => Promise<void>
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
