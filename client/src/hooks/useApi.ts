import { useAuth0 } from '@auth0/auth0-react'
import { useMemo } from 'react'
import { API_BASE_URL, ApiError } from '../utils/api'

export function useApi() {
  const { getAccessTokenSilently } = useAuth0()

  return useMemo(() => {
    async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
      const token = await getAccessTokenSilently()
      const response = await fetch(`${API_BASE_URL}${path}`, {
        ...options,
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
          ...options.headers,
        },
      })

      if (!response.ok) {
        const body = await response.json().catch(() => ({ message: response.statusText }))
        throw new ApiError(response.status, body.message ?? 'Request failed')
      }
      if (response.status === 204) {
        return undefined as T
      }
      return (await response.json()) as T
    }

    async function getBlob(path: string): Promise<Blob> {
      const token = await getAccessTokenSilently()
      const response = await fetch(`${API_BASE_URL}${path}`, {
        headers: { Authorization: `Bearer ${token}` },
      })
      if (!response.ok) {
        throw new ApiError(response.status, response.statusText || 'Request failed')
      }
      return response.blob()
    }

    return {
      get: <T,>(path: string) => request<T>(path),
      post: <T,>(path: string, body?: unknown) =>
        request<T>(path, { method: 'POST', body: body !== undefined ? JSON.stringify(body) : undefined }),
      put: <T,>(path: string, body?: unknown) =>
        request<T>(path, { method: 'PUT', body: body !== undefined ? JSON.stringify(body) : undefined }),
      patch: <T,>(path: string, body?: unknown) =>
        request<T>(path, { method: 'PATCH', body: body !== undefined ? JSON.stringify(body) : undefined }),
      del: <T,>(path: string) => request<T>(path, { method: 'DELETE' }),
      getBlob,
    }
  }, [getAccessTokenSilently])
}
