import { useState } from 'react'
import Footer from './components/layout/Footer'
import Header from './components/layout/Header'
import LoginPage from './pages/LoginPage'

function App() {
  const [isLoginVisible, setIsLoginVisible] = useState(false)

  return (
    <>
      <Header onLoginClick={() => setIsLoginVisible(true)} />
      <main>
        {isLoginVisible ? (
          <LoginPage />
        ) : (
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
