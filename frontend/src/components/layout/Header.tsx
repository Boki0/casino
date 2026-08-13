import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/useAuth'
import './Header.css'

function Header() {
  const navigate = useNavigate()
  const { profile, isAuthenticated, logout } = useAuth()

  function handleLogout() {
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
              to="/profile"
            >
              Profile
            </NavLink>
          )}
        </nav>

        <div className="app-header__actions">
          {isAuthenticated ? (
            <>
              <span className="app-header__user" title={profile?.username ?? 'Player'}>
                {profile?.username ?? 'Player'}
              </span>
              <button className="app-header__logout" type="button" onClick={handleLogout}>
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
