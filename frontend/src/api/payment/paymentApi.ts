import { authenticatedRequest } from '../http/authenticatedRequest'
import type { CreateDepositRequest, DepositResponse } from './paymentTypes'

export class PaymentApiError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'PaymentApiError'
  }
}

function userMessageForStatus(status: number): string {
  if (status === 400) return 'Please check the deposit amount.'
  if (status === 401) return 'Your session has expired. Please log in again.'
  if (status === 403) return 'You are not allowed to create this deposit.'
  return 'Payment could not be started. Please try again.'
}

async function createDeposit(request: CreateDepositRequest): Promise<DepositResponse> {
  let response: Response

  try {
    response = await authenticatedRequest('/api/payments/deposits', {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    })
  } catch {
    throw new PaymentApiError('Unable to reach the payment service. Please try again.')
  }

  if (!response.ok) {
    throw new PaymentApiError(userMessageForStatus(response.status))
  }

  return response.json() as Promise<DepositResponse>
}

export const paymentApi = {
  createDeposit,
}
