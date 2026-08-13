import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
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
  const [wallet, setWallet] = useState<WalletResponse | null>(null)
  const [hasError, setHasError] = useState(false)

  useEffect(() => {
    const controller = new AbortController()

    walletApi
      .getMyWallet(controller.signal)
      .then((loadedWallet) => {
        setWallet(loadedWallet)
        setHasError(false)
      })
      .catch((error) => {
        if (error instanceof DOMException && error.name === 'AbortError') return
        setHasError(true)
      })

    return () => controller.abort()
  }, [])

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
