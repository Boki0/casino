import './HomePage.css'

function HomePage() {
  return (
    <section className="home-page">
      <div className="home-page__content">
        <p className="home-page__eyebrow">Casino Platform</p>
        <h1>Welcome to Casino Platform</h1>
        <p className="home-page__subtitle">
          A modern casino experience is taking shape. Games, bonuses, and player features will
          live here soon.
        </p>

        <div className="home-page__actions">
          <button className="home-page__primary" type="button">
            Play Now
          </button>
          <button className="home-page__secondary" type="button">
            Create Account
          </button>
        </div>
      </div>
    </section>
  )
}

export default HomePage
