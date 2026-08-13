import { NavLink } from 'react-router-dom'
import { useAuth } from '../../auth/useAuth'

const accountLinks = [
  { label: 'Profile', to: '/account/profile' },
  { label: 'Deposit', to: '/account/deposit' },
  { label: 'Withdraw', to: '/account/withdraw' },
  { label: 'Settings', to: '/account/settings' },
]

function AccountSidebar() {
  const { profile, user } = useAuth()
  const username = profile?.username ?? 'Player'

  return (
    <aside className="account-sidebar">
      <div className="account-sidebar__player">
        <span aria-hidden="true">{username.charAt(0).toUpperCase()}</span>
        <div>
          <strong>{username}</strong>
          <small>{profile?.email ?? user?.email}</small>
        </div>
      </div>
      <nav aria-label="Account navigation">
        {accountLinks.map((link) => (
          <NavLink
            className={({ isActive }) =>
              `account-sidebar__link${isActive ? ' account-sidebar__link--active' : ''}`
            }
            key={link.to}
            to={link.to}
          >
            {link.label}
          </NavLink>
        ))}
      </nav>
      <div className="account-sidebar__support">
        <strong>Have a question?</strong>
        <p>Our support team is here to help you.</p>
        <button type="button" disabled>
          Contact support
        </button>
      </div>
    </aside>
  )
}

export default AccountSidebar
