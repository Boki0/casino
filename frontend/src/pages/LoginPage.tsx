import './LoginPage.css'

function LoginPage() {
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
          <form className="login-form">
            <div className="login-form__header">
              <p className="login-form__eyebrow">Player access</p>
              <h2>Log In</h2>
            </div>

            <label className="login-form__field">
              <span>Email</span>
              <input type="email" placeholder="Enter Email" />
            </label>

            <label className="login-form__field">
              <span>Password</span>
              <input type="password" placeholder="Enter Password" />
            </label>

            <button className="login-form__submit" type="button">
              Log In
            </button>

            <div className="login-form__divider">
              <span>Don't have an account yet?</span>
            </div>

            <button className="login-form__secondary" type="button">
              Register
            </button>
          </form>
        </div>
      </div>
    </section>
  )
}

export default LoginPage
