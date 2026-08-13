import { Route, Routes } from 'react-router-dom'
import Footer from './components/layout/Footer'
import Header from './components/layout/Header'
import HomePage from './pages/HomePage'
import GamePlayerPage from './pages/GamePlayerPage'
import LoginPage from './pages/LoginPage'
import ProfilePage from './pages/ProfilePage'
import RegisterPage from './pages/RegisterPage'
import SlotsPage from './pages/SlotsPage'
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
          <Route path="/login" element={<LoginPage />} />
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="/register" element={<RegisterPage />} />
        </Routes>
      </main>
      <Footer />
    </>
  )
}

export default App
