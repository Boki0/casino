import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AuthApiError, authApi } from '../api/auth/authApi'
import type { RegisterRequest } from '../api/auth/authTypes'
import './LoginPage.css'
import './RegisterPage.css'

type RegisterFormState = {
  username: string
  email: string
  password: string
  repeatPassword: string
  promoCode: string
  agreementAccepted: boolean
}

const INITIAL_FORM_STATE: RegisterFormState = {
  username: '',
  email: '',
  password: '',
  repeatPassword: '',
  promoCode: '',
  agreementAccepted: false,
}

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function validateForm(form: RegisterFormState): string | null {
  const username = form.username.trim()
  const email = form.email.trim()

  if (!username) return 'Username is required.'
  if (username.length < 3 || username.length > 30) {
    return 'Username must be between 3 and 30 characters.'
  }
  if (!email) return 'Email is required.'
  if (!EMAIL_PATTERN.test(email)) return 'Enter a valid email address.'
  if (!form.password) return 'Password is required.'
  if (form.password.length < 8) return 'Password must be at least 8 characters.'
  if (!form.repeatPassword) return 'Please repeat your password.'
  if (form.password !== form.repeatPassword) return 'Passwords do not match.'
  if (!form.agreementAccepted) {
    return 'You must accept the User Agreement and confirm you are at least 18.'
  }

  return null
}

function RegisterPage() {
  const navigate = useNavigate()
  const [isPasswordVisible, setIsPasswordVisible] = useState(false)
  const [isRepeatPasswordVisible, setIsRepeatPasswordVisible] = useState(false)
  const [form, setForm] = useState<RegisterFormState>(INITIAL_FORM_STATE)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  function updateField<K extends keyof RegisterFormState>(
    field: K,
    value: RegisterFormState[K],
  ) {
    setForm((currentForm) => ({ ...currentForm, [field]: value }))
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (isSubmitting) return

    setErrorMessage(null)
    const validationError = validateForm(form)
    if (validationError) {
      setErrorMessage(validationError)
      return
    }

    const request: RegisterRequest = {
      username: form.username.trim(),
      email: form.email.trim(),
      password: form.password,
      ...(form.promoCode.trim() ? { promoCode: form.promoCode.trim() } : {}),
    }

    setIsSubmitting(true)

    try {
      await authApi.register(request)
      navigate('/login', {
        replace: true,
        state: { registrationMessage: 'Account created successfully. Please log in.' },
      })
    } catch (error) {
      setErrorMessage(
        error instanceof AuthApiError
          ? error.message
          : 'Unexpected registration failure. Please try again.',
      )
      setIsSubmitting(false)
    }
  }

  return (
    <section className="login-page register-page">
      <div className="login-card register-card">
        <div className="login-card__visual register-card__visual">
          <div className="login-card__brand">
            <div className="login-card__logo">♠</div>
            <span>Casino Platform</span>
          </div>

          <div className="login-card__visual-content">
            <p className="login-card__eyebrow">Create your player account</p>
            <h1>Welcome to Casino Platform</h1>
            <p>Begin your adventure today</p>
          </div>

          <div className="login-card__shape login-card__shape--one"></div>
          <div className="login-card__shape login-card__shape--two"></div>
        </div>

        <div className="login-card__form-panel register-card__form-panel">
          <form className="login-form register-form" onSubmit={handleSubmit} noValidate>
            <div className="login-form__header register-form__header">
              <p className="login-form__eyebrow">New player</p>
              <h2>Register</h2>
            </div>

            <label className="login-form__field register-form__field">
              <span>Username</span>
              <input
                type="text"
                name="username"
                autoComplete="username"
                minLength={3}
                maxLength={30}
                placeholder="Enter Username"
                value={form.username}
                onChange={(event) => updateField('username', event.target.value)}
              />
            </label>

            <label className="login-form__field register-form__field">
              <span>Email</span>
              <input
                type="email"
                name="email"
                autoComplete="email"
                placeholder="Enter Email"
                value={form.email}
                onChange={(event) => updateField('email', event.target.value)}
              />
            </label>

            <label className="login-form__field register-form__field">
              <span>Password</span>
              <div className="login-form__password-control">
                <input
                  type={isPasswordVisible ? 'text' : 'password'}
                  name="password"
                  autoComplete="new-password"
                  placeholder="Enter Password"
                  value={form.password}
                  onChange={(event) => updateField('password', event.target.value)}
                />
                <button
                  className="login-form__password-toggle"
                  type="button"
                  onClick={() => setIsPasswordVisible((isVisible) => !isVisible)}
                >
                  {isPasswordVisible ? 'Hide' : 'Show'}
                </button>
              </div>
            </label>

            <label className="login-form__field register-form__field">
              <span>Repeat Password</span>
              <div className="login-form__password-control">
                <input
                  type={isRepeatPasswordVisible ? 'text' : 'password'}
                  name="repeatPassword"
                  autoComplete="new-password"
                  placeholder="Repeat Password"
                  value={form.repeatPassword}
                  onChange={(event) => updateField('repeatPassword', event.target.value)}
                />
                <button
                  className="login-form__password-toggle"
                  type="button"
                  onClick={() => setIsRepeatPasswordVisible((isVisible) => !isVisible)}
                >
                  {isRepeatPasswordVisible ? 'Hide' : 'Show'}
                </button>
              </div>
            </label>

            <label className="login-form__field register-form__field">
              <span>
                Promo Code <span className="register-form__optional">(Optional)</span>
              </span>
              <input
                type="text"
                name="promoCode"
                placeholder="Enter Promo Code"
                value={form.promoCode}
                onChange={(event) => updateField('promoCode', event.target.value)}
              />
            </label>

            <label className="register-form__agreement">
              <input
                type="checkbox"
                checked={form.agreementAccepted}
                onChange={(event) => updateField('agreementAccepted', event.target.checked)}
              />
              <span>I agree to the User Agreement &amp; confirm I am at least 18 years old</span>
            </label>

            {errorMessage && (
              <p className="register-form__message register-form__message--error" role="alert">
                {errorMessage}
              </p>
            )}

            <button
              className="login-form__submit register-form__submit"
              type="submit"
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Creating account...' : 'Register'}
            </button>

            <div className="login-form__divider register-form__divider">
              <span>Already have an account?</span>
            </div>

            <Link className="login-form__secondary" to="/login">
              Log In
            </Link>
          </form>
        </div>
      </div>
    </section>
  )
}

export default RegisterPage
