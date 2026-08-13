import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { AuthApiError, authApi } from '../api/auth/authApi'
import type { LoginRequest } from '../api/auth/authTypes'
import { useAuth } from '../auth/useAuth'
import './LoginPage.css'

type LoginLocationState = {
  registrationMessage?: string
}

type LoginFormState = {
  email: string
  password: string
}

const INITIAL_FORM_STATE: LoginFormState = {
  email: '',
  password: '',
}

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function validateForm(form: LoginFormState): string | null {
  const email = form.email.trim()

  if (!email) return 'Email is required.'
  if (!EMAIL_PATTERN.test(email)) return 'Enter a valid email address.'
  if (!form.password) return 'Password is required.'

  return null
}

function LoginPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const auth = useAuth()
  const [isPasswordVisible, setIsPasswordVisible] = useState(false)
  const [form, setForm] = useState<LoginFormState>(INITIAL_FORM_STATE)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const registrationMessage = (location.state as LoginLocationState | null)
    ?.registrationMessage

  function updateField(field: keyof LoginFormState, value: string) {
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

    const request: LoginRequest = {
      email: form.email.trim(),
      password: form.password,
    }

    setIsSubmitting(true)

    try {
      const loginResponse = await authApi.login(request)
      await auth.login(loginResponse)
      navigate('/', { replace: true })
    } catch (error) {
      setErrorMessage(
        error instanceof AuthApiError
          ? error.message
          : 'Unexpected login failure. Please try again.',
      )
      setIsSubmitting(false)
    }
  }

  return (
    <section className="login-page" id="login">
      <div className="login-card">
        <div className="login-card__visual">
          <div className="login-card__brand">
            <div className="login-card__logo">♠</div>
            <span>Casino Platform</span>
          </div>

          <div className="login-card__visual-content">
            <p className="login-card__eyebrow">Premium gaming hub</p>
            <h1>Welcome to Casino Platform</h1>
            <p>Begin your adventure today</p>
          </div>

          <div className="login-card__shape login-card__shape--one"></div>
          <div className="login-card__shape login-card__shape--two"></div>
        </div>

        <div className="login-card__form-panel">
          <form className="login-form" onSubmit={handleSubmit} noValidate>
            <div className="login-form__header">
              <p className="login-form__eyebrow">Player access</p>
              <h2>Log In</h2>
            </div>

            {registrationMessage && (
              <p className="login-form__message login-form__message--success" role="status">
                {registrationMessage}
              </p>
            )}

            <label className="login-form__field">
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

            <label className="login-form__field">
              <span>Password</span>
              <div className="login-form__password-control">
                <input
                  type={isPasswordVisible ? 'text' : 'password'}
                  name="password"
                  autoComplete="current-password"
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

            {errorMessage && (
              <p className="login-form__message login-form__message--error" role="alert">
                {errorMessage}
              </p>
            )}

            <button className="login-form__submit" type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Signing in...' : 'Log In'}
            </button>

            <div className="login-form__divider">
              <span>Don't have an account yet?</span>
            </div>

            <Link className="login-form__secondary" to="/register">
              Register
            </Link>
          </form>
        </div>
      </div>
    </section>
  )
}

export default LoginPage
