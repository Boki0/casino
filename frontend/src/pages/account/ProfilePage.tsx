import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { walletApi } from '../../api/wallet/walletApi'
import type { WalletResponse } from '../../api/wallet/walletTypes'
import { useAuth } from '../../auth/useAuth'

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

function ProfilePage() {
  const { profile, user } = useAuth()
  const [wallet, setWallet] = useState<WalletResponse | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    walletApi.getMyWallet(controller.signal).then(setWallet).catch(() => undefined)
    return () => controller.abort()
  }, [])

  return (
    <section className="account-section">
      <div className="profile-summary">
        <div className="profile-summary__identity">
          <span aria-hidden="true">
            {(profile?.username ?? 'P').charAt(0).toUpperCase()}
          </span>
          <div>
            <small>Welcome back</small>
            <h2>{profile?.username ?? 'Player'}</h2>
            <p>{profile?.email ?? user?.email}</p>
          </div>
        </div>
        <div className="profile-summary__wallet">
          <small>Your balance</small>
          <strong>{wallet ? formatBalance(wallet) : 'Loading...'}</strong>
          <div className="profile-summary__actions">
            <Link to="/account/deposit">Deposit</Link>
            <Link to="/account/withdraw">Withdraw</Link>
          </div>
        </div>
      </div>

      <header className="account-section__header account-section__header--overview">
        <h2>Account overview</h2>
        <p>Your current account information.</p>
      </header>
      <dl className="account-overview">
        <div>
          <dt>Username</dt>
          <dd>{profile?.username ?? 'Unavailable'}</dd>
        </div>
        <div>
          <dt>Email</dt>
          <dd>{profile?.email ?? user?.email ?? 'Unavailable'}</dd>
        </div>
        <div>
          <dt>Balance</dt>
          <dd>{wallet ? formatBalance(wallet) : 'Loading...'}</dd>
        </div>
      </dl>
    </section>
  )
}

export default ProfilePage
