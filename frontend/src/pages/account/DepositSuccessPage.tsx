import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { walletApi } from '../../api/wallet/walletApi'

function DepositSuccessPage() {
  const [secondsRemaining, setSecondsRemaining] = useState(3)

  useEffect(() => {
    void walletApi.getMyWallet().catch(() => undefined)

    const countdown = window.setInterval(() => {
      setSecondsRemaining((current) => Math.max(0, current - 1))
    }, 1000)
    const redirect = window.setTimeout(() => {
      window.location.replace('/slots')
    }, 3000)

    return () => {
      window.clearInterval(countdown)
      window.clearTimeout(redirect)
    }
  }, [])

  return (
    <section className="account-section deposit-success">
      <span className="deposit-success__icon" aria-hidden="true">✓</span>
      <h2>Deposit successful.</h2>
      <p>
        Stripe is confirming your payment. Your balance will update after the webhook is processed.
      </p>
      <p className="deposit-success__redirect">Opening Slots in {secondsRemaining}...</p>
      <Link to="/slots">Go to Slots now</Link>
    </section>
  )
}

export default DepositSuccessPage
