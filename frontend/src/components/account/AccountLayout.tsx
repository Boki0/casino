import { Outlet } from 'react-router-dom'
import AccountSidebar from './AccountSidebar'
import './AccountLayout.css'

function AccountLayout() {
  return (
    <section className="account-page">
      <header className="account-page__heading">
        <p>Account</p>
        <h1>Profile &amp; account</h1>
      </header>
      <div className="account-page__layout">
        <AccountSidebar />
        <div className="account-page__content">
          <Outlet />
        </div>
      </div>
    </section>
  )
}

export default AccountLayout
