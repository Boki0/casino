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
