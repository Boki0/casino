import { Link, NavLink } from 'react-router-dom'
import './Header.css'

function Header() {
  return (
    <header className="app-header">
      <div className="app-header__container">
        <Link className="app-header__brand" to="/">
          <div className="app-header__logo">♠</div>
          <span className="app-header__title">Casino Platform</span>
        </Link>

        <nav className="app-header__nav" aria-label="Main navigation">
          <NavLink
            className={({ isActive }) =>
              `app-header__link${isActive ? ' app-header__link--active' : ''}`
            }
            to="/slots"
          >
            Slots
          </NavLink>
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
          <NavLink className="app-header__login" to="/login">
            Log In
          </NavLink>
          <NavLink className="app-header__register" to="/register">
            Register
          </NavLink>
        </div>
      </div>
    </header>
  )
}

export default Header
