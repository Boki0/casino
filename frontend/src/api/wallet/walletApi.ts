import { authenticatedRequest } from '../http/authenticatedRequest'
import type { WalletResponse } from './walletTypes'

export class WalletApiError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'WalletApiError'
  }
}

async function getMyWallet(signal?: AbortSignal): Promise<WalletResponse> {
  let response: Response

  try {
    response = await authenticatedRequest('/api/wallet/me', {
      method: 'GET',
      headers: {
        Accept: 'application/json',
      },
      signal,
    })
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw error
    throw new WalletApiError('Unable to load wallet balance.')
  }

  if (!response.ok) {
    throw new WalletApiError('Unable to load wallet balance.')
  }

  return response.json() as Promise<WalletResponse>
}

export const walletApi = {
  getMyWallet,
}
