import { useCallback, useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { walletApi } from '../../api/wallet/walletApi'
import type { WalletResponse } from '../../api/wallet/walletTypes'

function formatBalance(wallet: WalletResponse): string {
  try {
    return new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency: wallet.currency,
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(wallet.balance)
  } catch {
    return `${wallet.balance.toFixed(2)} ${wallet.currency}`
  }
}

function HeaderWalletBalance() {
  const location = useLocation()
  const [wallet, setWallet] = useState<WalletResponse | null>(null)
  const [hasError, setHasError] = useState(false)
  const isGameActive = location.pathname.startsWith('/play/')

  const loadWallet = useCallback((signal?: AbortSignal) => {
    return walletApi
      .getMyWallet(signal)
      .then((loadedWallet) => {
        setWallet(loadedWallet)
        setHasError(false)
      })
      .catch((error) => {
        if (error instanceof DOMException && error.name === 'AbortError') return
        setHasError(true)
      })
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    void loadWallet(controller.signal)

    return () => controller.abort()
  }, [loadWallet])

  useEffect(() => {
    if (!isGameActive) return

    const controllers = new Set<AbortController>()
    const refreshWallet = () => {
      const controller = new AbortController()
      controllers.add(controller)
      void loadWallet(controller.signal).finally(() => controllers.delete(controller))
    }

    refreshWallet()
    const intervalId = window.setInterval(refreshWallet, 8000)

    return () => {
      window.clearInterval(intervalId)
      controllers.forEach((controller) => controller.abort())
    }
  }, [isGameActive, loadWallet])

  return (
    <div className="app-header__wallet" aria-live="polite">
      <Link className="app-header__balance" to="/account/profile">
        <span>Your Balance</span>
        <strong>{hasError ? 'Unavailable' : wallet ? formatBalance(wallet) : '...'}</strong>
      </Link>
      <Link className="app-header__deposit" to="/account/deposit">
        <span aria-hidden="true">+</span>
        Deposit
      </Link>
    </div>
  )
}

export default HeaderWalletBalance
