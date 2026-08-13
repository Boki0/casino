import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/useAuth'
import HeaderWalletBalance from './HeaderWalletBalance'
import { closeGameSession } from '../../api/gameLaunch'
import { activeGameStorage } from '../../services/activeGameStorage'
import './Header.css'

function Header() {
  const navigate = useNavigate()
  const { isAuthenticated, logout } = useAuth()

  async function handleLogout() {
    const activeSessionId = window.location.pathname.startsWith('/play/')
      ? window.location.pathname.slice('/play/'.length)
      : null
    if (activeSessionId) {
      await closeGameSession(activeSessionId).catch(() => undefined)
      activeGameStorage.clear()
    }
    logout()
    navigate('/', { replace: true })
  }

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
          {isAuthenticated && (
            <NavLink
              className={({ isActive }) =>
                `app-header__link${isActive ? ' app-header__link--active' : ''}`
              }
              to="/account/profile"
            >
              Profile
            </NavLink>
          )}
        </nav>

        <div className="app-header__actions">
          {isAuthenticated ? (
            <>
              <HeaderWalletBalance />
              <button
                className="app-header__logout"
                type="button"
                onClick={() => void handleLogout()}
              >
                Logout
              </button>
            </>
          ) : (
            <>
              <NavLink className="app-header__login" to="/login">
                Log In
              </NavLink>
              <NavLink className="app-header__register" to="/register">
                Register
              </NavLink>
            </>
          )}
        </div>
      </div>
    </header>
  )
}

export default Header
