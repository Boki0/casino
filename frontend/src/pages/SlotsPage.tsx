import { useEffect, useMemo, useState } from 'react'
import { getPublicGames, type PublicGame } from '../api/gameCatalog'
import './SlotsPage.css'

type ProviderGroup = {
  code: string
  name: string
  games: PublicGame[]
}

function groupGamesByProvider(games: PublicGame[]): ProviderGroup[] {
  const groups = new Map<string, ProviderGroup>()

  for (const game of games) {
    const existingGroup = groups.get(game.providerCode)
    if (existingGroup) {
      existingGroup.games.push(game)
    } else {
      groups.set(game.providerCode, {
        code: game.providerCode,
        name: game.providerName,
        games: [game],
      })
    }
  }

  return Array.from(groups.values()).sort((first, second) =>
    first.name.localeCompare(second.name),
  )
}

function GameCard({ game }: { game: PublicGame }) {
  const [imageFailed, setImageFailed] = useState(false)
  const showImage = Boolean(game.thumbnailUrl) && !imageFailed

  return (
    <article className="game-card">
      <div className="game-card__media">
        {showImage ? (
          <img
            src={game.thumbnailUrl!}
            alt=""
            onError={() => setImageFailed(true)}
          />
        ) : (
          <div className="game-card__image-fallback" aria-hidden="true">
            ♠
          </div>
        )}
      </div>
      <h3>{game.name}</h3>
    </article>
  )
}

function SlotsPage() {
  const [games, setGames] = useState<PublicGame[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [hasError, setHasError] = useState(false)

  useEffect(() => {
    const controller = new AbortController()

    async function loadGames() {
      try {
        const loadedGames = await getPublicGames(controller.signal)
        setGames(loadedGames)
        setHasError(false)
      } catch (error) {
        if (error instanceof DOMException && error.name === 'AbortError') {
          return
        }
        setHasError(true)
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false)
        }
      }
    }

    void loadGames()
    return () => controller.abort()
  }, [])

  const providerGroups = useMemo(() => groupGamesByProvider(games), [games])

  return (
    <div className="slots-page">
      <header className="slots-page__header">
        <h1>Slots</h1>
        <p>Explore available games by provider.</p>
      </header>

      <div className="slots-page__catalog" aria-live="polite">
        {isLoading && <p className="slots-page__status">Loading games...</p>}
        {!isLoading && hasError && (
          <p className="slots-page__status">Games could not be loaded.</p>
        )}
        {!isLoading && !hasError && games.length === 0 && (
          <p className="slots-page__status">No games are currently available.</p>
        )}
        {!isLoading &&
          !hasError &&
          providerGroups.map((provider) => (
            <section className="provider-games" key={provider.code}>
              <div className="provider-games__heading">
                <h2>{provider.name}</h2>
                <span>
                  {provider.games.length} {provider.games.length === 1 ? 'game' : 'games'}
                </span>
              </div>
              <div className="provider-games__grid">
                {provider.games.map((game) => (
                  <GameCard game={game} key={game.id} />
                ))}
              </div>
            </section>
          ))}
      </div>
    </div>
  )
}

export default SlotsPage
