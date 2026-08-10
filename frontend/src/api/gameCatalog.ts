export type PublicGame = {
  id: string
  name: string
  providerCode: string
  providerName: string
  thumbnailUrl: string | null
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL?.trim().replace(/\/$/, '') ?? ''

export async function getPublicGames(signal?: AbortSignal): Promise<PublicGame[]> {
  const response = await fetch(`${API_BASE_URL}/api/games`, {
    method: 'GET',
    headers: {
      Accept: 'application/json',
    },
    signal,
  })

  if (!response.ok) {
    throw new Error(`Game catalog request failed with status ${response.status}`)
  }

  return response.json()
}
