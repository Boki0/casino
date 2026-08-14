import { useState, type FormEvent } from 'react'

const SERBIAN_ACCOUNT_PATTERN = /^\d{3}-\d{1,13}-\d{2}$/
const MAX_ACCOUNT_DIGITS = 18

function accountDigits(value: string): string {
  return value.replace(/\D/g, '').slice(0, MAX_ACCOUNT_DIGITS)
}

function formatAccountWhileTyping(value: string): string {
  const digits = accountDigits(value)
  if (digits.length < 3) return digits

  const bankCode = digits.slice(0, 3)
  const accountNumber = digits.slice(3, 16)
  const checkNumber = digits.slice(16, 18)

  if (!accountNumber) return `${bankCode}-`
  if (digits.length < 16) return `${bankCode}-${accountNumber}`
  return `${bankCode}-${accountNumber}-${checkNumber}`
}

function formatCompleteAccount(value: string): string {
  const digits = accountDigits(value)
  if (digits.length < 6) return formatAccountWhileTyping(digits)

  return `${digits.slice(0, 3)}-${digits.slice(3, -2)}-${digits.slice(-2)}`
}

type WithdrawalErrors = {
  amount?: string
  accountHolder?: string
  iban?: string
}

function WithdrawPage() {
  const [amount, setAmount] = useState('')
  const [accountHolder, setAccountHolder] = useState('')
  const [iban, setIban] = useState('')
  const [errors, setErrors] = useState<WithdrawalErrors>({})
  const [informationMessage, setInformationMessage] = useState<string | null>(null)

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const nextErrors: WithdrawalErrors = {}
    const numericAmount = Number(amount.trim())
    const normalizedAccount = formatCompleteAccount(iban)
    setIban(normalizedAccount)

    if (!amount.trim() || !Number.isFinite(numericAmount) || numericAmount <= 0) {
      nextErrors.amount = 'Enter an amount greater than zero.'
    }
    if (!accountHolder.trim()) {
      nextErrors.accountHolder = 'Enter the account holder name.'
    }
    if (!normalizedAccount) {
      nextErrors.iban = 'Enter a domestic bank account number.'
    } else if (!SERBIAN_ACCOUNT_PATTERN.test(normalizedAccount)) {
      nextErrors.iban = 'Use the Serbian account format: 3 digits, up to 13 digits, then 2 digits.'
    }

    setErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) {
      setInformationMessage(null)
      return
    }

    setInformationMessage('Withdrawal functionality will be connected in a later step.')
  }

  return (
    <section className="account-section">
      <header className="account-section__header">
        <h2>Withdraw</h2>
        <p>Enter bank details for a future withdrawal request.</p>
      </header>

      <form className="withdraw-form" onSubmit={handleSubmit} noValidate>
        <div className="withdraw-form__intro">
          <span aria-hidden="true">↓</span>
          <div>
            <strong>Withdrawal details</strong>
            <p>This is a visual preview. No funds will be moved or reserved.</p>
          </div>
        </div>

        <div className="withdraw-form__field">
          <label htmlFor="withdraw-amount">Withdrawal amount</label>
          <div className="withdraw-form__amount">
            <span>€</span>
            <input
              id="withdraw-amount"
              type="text"
              inputMode="decimal"
              autoComplete="off"
              placeholder="100.00"
              value={amount}
              onChange={(event) => {
                setAmount(event.target.value)
                setErrors((current) => ({ ...current, amount: undefined }))
                setInformationMessage(null)
              }}
              aria-invalid={Boolean(errors.amount)}
              aria-describedby={errors.amount ? 'withdraw-amount-error' : undefined}
            />
            <strong>EUR</strong>
          </div>
          {errors.amount && (
            <p className="withdraw-form__error" id="withdraw-amount-error" role="alert">
              {errors.amount}
            </p>
          )}
        </div>

        <div className="withdraw-form__field">
          <label htmlFor="withdraw-method">Payment method</label>
          <select id="withdraw-method" value="BANK_TRANSFER" disabled>
            <option value="BANK_TRANSFER">Bank transfer</option>
          </select>
        </div>

        <div className="withdraw-form__field">
          <label htmlFor="account-holder">Account holder</label>
          <input
            id="account-holder"
            type="text"
            autoComplete="name"
            placeholder="John Doe"
            value={accountHolder}
            onChange={(event) => {
              setAccountHolder(event.target.value)
              setErrors((current) => ({ ...current, accountHolder: undefined }))
              setInformationMessage(null)
            }}
            aria-invalid={Boolean(errors.accountHolder)}
            aria-describedby={errors.accountHolder ? 'account-holder-error' : undefined}
          />
          {errors.accountHolder && (
            <p className="withdraw-form__error" id="account-holder-error" role="alert">
              {errors.accountHolder}
            </p>
          )}
        </div>

        <div className="withdraw-form__field">
          <label htmlFor="withdraw-iban">Domestic bank account number</label>
          <input
            id="withdraw-iban"
            type="text"
            inputMode="numeric"
            autoComplete="off"
            placeholder="169-32332323-32"
            maxLength={20}
            value={iban}
            onChange={(event) => {
              setIban(formatAccountWhileTyping(event.target.value))
              setErrors((current) => ({ ...current, iban: undefined }))
              setInformationMessage(null)
            }}
            onPaste={(event) => {
              event.preventDefault()
              setIban(formatCompleteAccount(event.clipboardData.getData('text')))
              setErrors((current) => ({ ...current, iban: undefined }))
              setInformationMessage(null)
            }}
            onBlur={() => setIban((current) => formatCompleteAccount(current))}
            onKeyDown={(event) => {
              if (event.key === 'Backspace' && iban.endsWith('-')) {
                event.preventDefault()
                setIban(formatAccountWhileTyping(accountDigits(iban).slice(0, -1)))
              }
            }}
            aria-invalid={Boolean(errors.iban)}
            aria-describedby={errors.iban ? 'withdraw-iban-error' : undefined}
          />
          {errors.iban && (
            <p className="withdraw-form__error" id="withdraw-iban-error" role="alert">
              {errors.iban}
            </p>
          )}
        </div>

        {informationMessage && (
          <p className="withdraw-form__notice" role="status">
            {informationMessage}
          </p>
        )}

        <button type="submit">Withdraw</button>
      </form>
    </section>
  )
}

export default WithdrawPage
