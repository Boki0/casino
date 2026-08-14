export type RegisterRequest = {
  email: string
  password: string
  username: string
  promoCode?: string
}

export type AuthUser = {
  id: string
  email: string
  role: string
  status: string
}

export type RegisterResponse = AuthUser

export type LoginRequest = {
  email: string
  password: string
}

export type LoginResponse = {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: AuthUser
}
