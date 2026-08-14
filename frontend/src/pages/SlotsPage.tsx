import { useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { getPublicGames, type PublicGame } from '../api/gameCatalog'
import { launchGame } from '../api/gameLaunch'
import { useAuth } from '../auth/useAuth'
import { activeGameStorage } from '../services/activeGameStorage'
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

type GameCardProps = {
  game: PublicGame
  isLaunching: boolean
  onPlay: (game: PublicGame) => void
}

function GameCard({ game, isLaunching, onPlay }: GameCardProps) {
  const [imageFailed, setImageFailed] = useState(false)
  const showImage = Boolean(game.thumbnailUrl) && !imageFailed

  return (
    <button
      className="game-card"
      type="button"
      aria-busy={isLaunching}
      aria-label={`${isLaunching ? 'Launching' : 'Play'} ${game.name}`}
      disabled={isLaunching}
      onClick={() => onPlay(game)}
    >
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
      <h3>{isLaunching ? 'Launching...' : game.name}</h3>
    </button>
  )
}

function SlotsPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const { isAuthenticated } = useAuth()
  const [games, setGames] = useState<PublicGame[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [hasError, setHasError] = useState(false)
  const [launchingGameId, setLaunchingGameId] = useState<string | null>(null)
  const [launchError, setLaunchError] = useState<string | null>(null)

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

  async function handlePlay(game: PublicGame) {
    if (!isAuthenticated) {
      const from = `${location.pathname}${location.search}${location.hash}`
      navigate('/login', {
        state: {
          from,
          loginMessage: 'Please log in to play.',
        },
      })
      return
    }

    if (launchingGameId) return

    const currency = game.supportedCurrencies.includes('EUR')
      ? 'EUR'
      : game.supportedCurrencies[0]
    if (!currency) {
      setLaunchError('This game does not have a supported currency.')
      return
    }

    setLaunchingGameId(game.id)
    setLaunchError(null)

    try {
      const launch = await launchGame(game.id, currency)
      activeGameStorage.save({
        sessionId: launch.sessionId,
        gameId: game.id,
        gameName: game.name,
        launchUrl: launch.launchUrl,
      })
      navigate(`/play/${launch.sessionId}`)
    } catch {
      setLaunchError('The game could not be launched. Please try again.')
      setLaunchingGameId(null)
    }
  }

  return (
    <div className="slots-page">
      <header className="slots-page__header">
        <h1>Slots</h1>
        <p>Explore available games by provider.</p>
      </header>

      <div className="slots-page__catalog" aria-live="polite">
        {launchError && (
          <p className="slots-page__launch-error" role="alert">
            {launchError}
          </p>
        )}
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
                  <GameCard
                    game={game}
                    isLaunching={launchingGameId === game.id}
                    key={game.id}
                    onPlay={handlePlay}
                  />
                ))}
              </div>
            </section>
          ))}
      </div>
    </div>
  )
}

export default SlotsPage
