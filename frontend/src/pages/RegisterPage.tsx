import { useState } from 'react'
import { Link } from 'react-router-dom'
import './LoginPage.css'
import './RegisterPage.css'

function RegisterPage() {
  const [isPasswordVisible, setIsPasswordVisible] = useState(false)
  const [isRepeatPasswordVisible, setIsRepeatPasswordVisible] = useState(false)

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
          <form className="login-form register-form">
            <div className="login-form__header register-form__header">
              <p className="login-form__eyebrow">New player</p>
              <h2>Register</h2>
            </div>

            <label className="login-form__field register-form__field">
              <span>Email</span>
              <input type="email" placeholder="Enter Email" />
            </label>

            <label className="login-form__field register-form__field">
              <span>Password</span>
              <div className="login-form__password-control">
                <input
                  type={isPasswordVisible ? 'text' : 'password'}
                  placeholder="Enter Password"
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
                  placeholder="Repeat Password"
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
              <input type="text" placeholder="Enter Promo Code" />
            </label>

            <div className="register-form__agreement">
              <span className="register-form__checkbox" aria-hidden="true">
                ✓
              </span>
              <span>I agree to the User Agreement &amp; confirm I am at least 18 years old</span>
            </div>

            <button className="login-form__submit register-form__submit" type="button">
              Register
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
