export type RegisterRequest = {
  email: string
  password: string
  username: string
  promoCode?: string
}

export type RegisterResponse = {
  id: string
  email: string
  role: string
  status: string
}
