export type CreateDepositRequest = {
  amount: number
  currency: string
  creditsAmount: number
  provider: 'STRIPE'
  idempotencyKey: string
}

export type DepositResponse = {
  depositId: string
  authUserId: string
  amount: number
  currency: string
  creditsAmount: number
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELLED' | 'EXPIRED'
  provider: 'STRIPE' | 'PAYPAL' | 'CRYPTO' | 'MANUAL'
  providerSessionId: string | null
  providerPaymentId: string | null
  checkoutUrl: string | null
  createdAt: string
  updatedAt: string
  completedAt: string | null
}
