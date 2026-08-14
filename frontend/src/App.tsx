import { Navigate, Route, Routes } from 'react-router-dom'
import Footer from './components/layout/Footer'
import Header from './components/layout/Header'
import AccountLayout from './components/account/AccountLayout'
import HomePage from './pages/HomePage'
import GamePlayerPage from './pages/GamePlayerPage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import SlotsPage from './pages/SlotsPage'
import DepositPage from './pages/account/DepositPage'
import DepositSuccessPage from './pages/account/DepositSuccessPage'
import ProfilePage from './pages/account/ProfilePage'
import SettingsPage from './pages/account/SettingsPage'
import WithdrawPage from './pages/account/WithdrawPage'
import { ProtectedRoute } from './auth/ProtectedRoute'

function App() {
  return (
    <>
      <Header />
      <main>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/slots" element={<SlotsPage />} />
          <Route
            path="/play/:sessionId"
            element={
              <ProtectedRoute>
                <GamePlayerPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/account"
            element={
              <ProtectedRoute>
                <AccountLayout />
              </ProtectedRoute>
            }
          >
            <Route index element={<Navigate to="profile" replace />} />
            <Route path="profile" element={<ProfilePage />} />
            <Route path="deposit" element={<DepositPage />} />
            <Route path="deposit/success" element={<DepositSuccessPage />} />
            <Route path="withdraw" element={<WithdrawPage />} />
            <Route path="settings" element={<SettingsPage />} />
          </Route>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/profile" element={<Navigate to="/account/profile" replace />} />
          <Route path="/register" element={<RegisterPage />} />
        </Routes>
      </main>
      <Footer />
    </>
  )
}

export default App
