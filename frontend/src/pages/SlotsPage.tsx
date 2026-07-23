import './SlotsPage.css'

function SlotsPage() {
  return (
    <div className="slots-page">
      <header className="slots-page__header">
        <h1>Slots</h1>
        <p>Explore available games by provider.</p>
      </header>

      <section className="slots-page__games" aria-label="Available slot games">
        <p>Games will appear here.</p>
      </section>
    </div>
  )
}

export default SlotsPage
