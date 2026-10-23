import {useCallback, useEffect , useRef} from 'react'
import {Client} from '@stomp/stompjs'
import {BASE_URL, getAuth} from '../api'


export default function useRideSocket(rideId, handlers){
   const clientRef=useRef(null)
   const  handlersRef=useRef(handlers)

   useEffect(()=>{
   handlersRef.current=handlers
   })



    useEffect(() => {
       if (!rideId) return
       const client = new Client({
         brokerURL: BASE_URL.replace(/^http/, 'ws') + '/ws',
         connectHeaders: { Authorization: 'Bearer ' + getAuth()?.token },
         reconnectDelay: 5000,
         onConnect: () => {
           client.subscribe(`/topic/ride/${rideId}/status`, (m) =>
             handlersRef.current.onStatus?.(JSON.parse(m.body)))
           client.subscribe(`/topic/ride/${rideId}/location`, (m) =>
             handlersRef.current.onLocation?.(JSON.parse(m.body)))
           client.subscribe(`/topic/ride/${rideId}/sos`, (m) =>
             handlersRef.current.onSos?.(JSON.parse(m.body)))
           client.subscribe('/user/queue/errors', (m) =>
             handlersRef.current.onError?.(m.body))
         },
       })
       client.activate()
       clientRef.current = client
       return () => {
         client.deactivate()
         clientRef.current = null
       }
     }, [rideId])

     const publish = useCallback(
       (action, body = {}) => {
         clientRef.current?.publish({
           destination: `/app/ride/${rideId}/${action}`,
           body: JSON.stringify(body),
         })
       },
       [rideId]
     )

     return { publish }
}