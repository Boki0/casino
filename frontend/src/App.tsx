import { useState } from 'react'
import Footer from './components/layout/Footer'
import Header from './components/layout/Header'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'

type CurrentPage = 'home' | 'login' | 'register'

function App() {
  const [currentPage, setCurrentPage] = useState<CurrentPage>('home')

  return (
    <>
      <Header
        onLoginClick={() => setCurrentPage('login')}
        onRegisterClick={() => setCurrentPage('register')}
      />
      <main>
        {currentPage === 'login' && <LoginPage />}
        {currentPage === 'register' && <RegisterPage />}
        {currentPage === 'home' && (
          <section className="home-placeholder">
            <h1>Casino Platform</h1>
            <p>Home page content will be added here.</p>
          </section>
        )}
      </main>
      <Footer />
    </>
  )
}

export default App
