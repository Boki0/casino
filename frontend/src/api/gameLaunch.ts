import { authenticatedRequest } from './http/authenticatedRequest'

export type GameLaunchResponse = {
  sessionId: string
  launchUrl: string
}

export class GameLaunchError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'GameLaunchError'
  }
}

export async function launchGame(
  gameId: string,
  currency: string,
): Promise<GameLaunchResponse> {
  let response: Response

  try {
    response = await authenticatedRequest(`/api/games/${gameId}/launch`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
      },
      body: JSON.stringify({ currency }),
    })
  } catch {
    throw new GameLaunchError('Unable to launch this game. Please try again.')
  }

  if (!response.ok) {
    throw new GameLaunchError('Unable to launch this game. Please try again.')
  }

  return response.json() as Promise<GameLaunchResponse>
}

export async function closeGameSession(
  sessionId: string,
  keepalive = false,
): Promise<void> {
  try {
    const response = await authenticatedRequest(`/api/games/sessions/${sessionId}/close`, {
      method: 'POST',
      keepalive,
    })

    if (!response.ok) {
      throw new GameLaunchError('Unable to close the game session.')
    }
  } catch (error) {
    if (error instanceof GameLaunchError) throw error
    throw new GameLaunchError('Unable to close the game session.')
  }
}
