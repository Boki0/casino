import { useCallback, useEffect, useMemo, useRef } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { closeGameSession } from '../api/gameLaunch'
import { activeGameStorage } from '../services/activeGameStorage'
import './GamePlayerPage.css'

function GamePlayerPage() {
  const navigate = useNavigate()
  const { sessionId = '' } = useParams()
  const activeGame = useMemo(() => activeGameStorage.get(sessionId), [sessionId])
  const closePromiseRef = useRef<Promise<void> | null>(null)
  const cleanupArmedRef = useRef(false)

  const closeOnce = useCallback(
    (keepalive = false) => {
      if (!sessionId) return Promise.resolve()
      if (!closePromiseRef.current) {
        closePromiseRef.current = closeGameSession(sessionId, keepalive).catch(() => undefined)
      }
      return closePromiseRef.current
    },
    [sessionId],
  )

  useEffect(() => {
    if (activeGame) return
    activeGameStorage.clear()
    navigate('/slots', { replace: true })
  }, [activeGame, navigate])

  useEffect(() => {
    const armCleanup = window.setTimeout(() => {
      cleanupArmedRef.current = true
    }, 0)

    function handlePageHide() {
      activeGameStorage.clear()
      void closeOnce(true)
    }

    window.addEventListener('pagehide', handlePageHide)
    return () => {
      window.clearTimeout(armCleanup)
      window.removeEventListener('pagehide', handlePageHide)
      if (cleanupArmedRef.current) {
        activeGameStorage.clear()
        void closeOnce()
      }
    }
  }, [closeOnce])

  async function handleExit() {
    activeGameStorage.clear()
    await closeOnce()
    navigate('/slots', { replace: true })
  }

  if (!activeGame) return null

  return (
    <section className="game-player-page">
      <div className="game-player-page__toolbar">
        <div>
          <p>Now playing</p>
          <h1>{activeGame.gameName}</h1>
        </div>
        <button type="button" onClick={handleExit}>
          Exit Game
        </button>
      </div>
      <div className="game-player-page__frame">
        <iframe
          src={activeGame.launchUrl}
          title={`${activeGame.gameName} game`}
          allow="fullscreen"
          referrerPolicy="strict-origin-when-cross-origin"
          sandbox="allow-forms allow-same-origin allow-scripts"
        />
      </div>
    </section>
  )
}

export default GamePlayerPage
