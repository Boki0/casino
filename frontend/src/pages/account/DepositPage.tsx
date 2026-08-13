import { useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { PaymentApiError, paymentApi } from '../../api/payment/paymentApi'

const AMOUNT_PATTERN = /^\d+(\.\d{1,2})?$/

function validateAmount(amount: string): string | null {
  const normalizedAmount = amount.trim()
  if (!normalizedAmount) return 'Enter a deposit amount.'
  if (!AMOUNT_PATTERN.test(normalizedAmount)) return 'Use a positive amount with up to 2 decimals.'
  if (Number(normalizedAmount) <= 0) return 'Deposit amount must be greater than zero.'
  return null
}

function DepositPage() {
  const [searchParams] = useSearchParams()
  const [amount, setAmount] = useState('')
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const wasCancelled = searchParams.get('cancelled') === 'true'

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (isSubmitting) return

    const validationMessage = validateAmount(amount)
    if (validationMessage) {
      setErrorMessage(validationMessage)
      return
    }

    setErrorMessage(null)
    setIsSubmitting(true)

    try {
      const normalizedAmount = Number(amount.trim())
      const deposit = await paymentApi.createDeposit({
        amount: normalizedAmount,
        currency: 'EUR',
        creditsAmount: normalizedAmount,
        provider: 'STRIPE',
        idempotencyKey: crypto.randomUUID(),
      })

      if (!deposit.checkoutUrl) {
        throw new PaymentApiError('Payment could not be started. Please try again.')
      }

      const checkoutUrl = new URL(deposit.checkoutUrl)
      if (checkoutUrl.protocol !== 'https:') {
        throw new PaymentApiError('Payment could not be started. Please try again.')
      }

      window.location.assign(checkoutUrl.toString())
    } catch (error) {
      setErrorMessage(
        error instanceof PaymentApiError
          ? error.message
          : 'Payment could not be started. Please try again.',
      )
      setIsSubmitting(false)
    }
  }

  return (
    <section className="account-section">
      <header className="account-section__header">
        <h2>Deposit</h2>
        <p>Add funds through secure Stripe Checkout.</p>
      </header>

      {wasCancelled && (
        <p className="deposit-notice" role="status">
          Deposit was cancelled. No funds were charged.
        </p>
      )}

      <form className="deposit-form" onSubmit={handleSubmit} noValidate>
        <div className="deposit-form__method">
          <span aria-hidden="true">↗</span>
          <div>
            <strong>Secure card payment</strong>
            <p>Card details are entered securely on Stripe. They never pass through this app.</p>
          </div>
        </div>

        <label htmlFor="deposit-amount">Deposit amount</label>
        <div className="deposit-form__amount">
          <span>€</span>
          <input
            id="deposit-amount"
            type="text"
            inputMode="decimal"
            autoComplete="off"
            placeholder="100.00"
            value={amount}
            onChange={(event) => {
              setAmount(event.target.value)
              setErrorMessage(null)
            }}
            disabled={isSubmitting}
            aria-describedby="deposit-currency deposit-error"
          />
          <strong id="deposit-currency">EUR</strong>
        </div>

        {errorMessage && (
          <p className="deposit-form__error" id="deposit-error" role="alert">
            {errorMessage}
          </p>
        )}

        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Preparing payment...' : 'Continue to secure payment'}
        </button>
      </form>
    </section>
  )
}

export default DepositPage
