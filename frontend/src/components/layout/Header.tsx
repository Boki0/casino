import './Header.css'

type HeaderProps = {
  onLoginClick: () => void
}

function Header({ onLoginClick }: HeaderProps) {
  return (
    <header className="app-header">
      <div className="app-header__container">
        <div className="app-header__brand">
          <div className="app-header__logo">♠</div>
          <span className="app-header__title">Casino Platform</span>
        </div>

        <nav className="app-header__nav" aria-label="Main navigation">
          <a className="app-header__link" href="#">
            About Us
          </a>
          <a className="app-header__link" href="#">
            FAQ
          </a>
          <a className="app-header__link" href="#">
            VIP Club
          </a>
        </nav>

        <div className="app-header__actions">
          <button className="app-header__login" type="button" onClick={onLoginClick}>
            Log In
          </button>
          <button className="app-header__register" type="button">
            Register
          </button>
        </div>
      </div>
    </header>
  )
}

export default Header
