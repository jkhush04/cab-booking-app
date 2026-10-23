import { useEffect, useRef } from 'react'
import { Client } from '@stomp/stompjs'
import { BASE_URL, getAuth } from '../api'

export default function useDriverRequests(enabled, onNewRide) {
  const callbackRef = useRef(onNewRide)

  useEffect(() => {
    callbackRef.current = onNewRide
  })

  useEffect(() => {
    if (!enabled) return
    const client = new Client({
      brokerURL: BASE_URL.replace(/^http/, 'ws') + '/ws',
      connectHeaders: { Authorization: 'Bearer ' + getAuth()?.token },
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe('/topic/drivers/requests', (m) => callbackRef.current?.(JSON.parse(m.body)))
      },
    })
    client.activate()
    return () => client.deactivate()
  }, [enabled])
}