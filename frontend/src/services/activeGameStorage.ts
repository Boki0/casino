export type ActiveGameLaunch = {
  sessionId: string
  gameId: string
  gameName: string
  launchUrl: string
}

const ACTIVE_GAME_KEY = 'casino.activeGame'

function save(activeGame: ActiveGameLaunch) {
  sessionStorage.setItem(ACTIVE_GAME_KEY, JSON.stringify(activeGame))
}

function get(sessionId: string): ActiveGameLaunch | null {
  const storedGame = sessionStorage.getItem(ACTIVE_GAME_KEY)
  if (!storedGame) return null

  try {
    const activeGame = JSON.parse(storedGame) as Partial<ActiveGameLaunch>
    if (
      activeGame.sessionId === sessionId &&
      typeof activeGame.gameId === 'string' &&
      typeof activeGame.gameName === 'string' &&
      typeof activeGame.launchUrl === 'string' &&
      activeGame.launchUrl.trim()
    ) {
      return activeGame as ActiveGameLaunch
    }
  } catch {
    // Invalid temporary data is removed below.
  }

  clear()
  return null
}

function clear() {
  sessionStorage.removeItem(ACTIVE_GAME_KEY)
}

export const activeGameStorage = {
  save,
  get,
  clear,
}
