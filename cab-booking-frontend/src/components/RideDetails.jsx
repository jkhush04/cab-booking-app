import { useState } from 'react'
import { api } from '../api'
import useRideSocket from '../hooks/useRideSocket'
import RideMap from './RideMap'
import StatusBadge from './StatusBadge'
import PaymentPanel from './PaymentPanel'
import RatingForm from './RatingForm'

export default function RideDetails({ ride, onChanged, onClose }) {
  const [events, setEvents] = useState([])
  const [driverPos, setDriverPos] = useState(null)
  const [paymentKey, setPaymentKey] = useState(0)
  const [error, setError] = useState('')

  const addEvent = (text, danger = false) =>
    setEvents((prev) =>
      [{ text, danger, time: new Date().toLocaleTimeString() }, ...prev].slice(0, 20))

  const { publish } = useRideSocket(ride.id, {
    onStatus: (e) => {
      addEvent(e.message)
      if (e.type === 'PAYMENT_SUCCESS') setPaymentKey((k) => k + 1)
      onChanged()
    },
    onLocation: (loc) => setDriverPos({ lat: loc.lat, lng: loc.lng }),
    onSos: (e) => addEvent('🚨 ' + e.message, true),
    onError: (msg) => setError(msg),
  })

  const markers = [
    { lat: ride.pickupLat, lng: ride.pickupLng, label: 'Pickup' },
    { lat: ride.dropLat, lng: ride.dropLng, label: 'Drop' },
    driverPos && { ...driverPos, label: 'Driver' },
  ].filter(Boolean)

  const cancel = async () => {
    setError('')
    try {
      await api(`/api/rides/${ride.id}/cancel`, { method: 'POST' })
      onChanged()
    } catch (e) {
      setError(e.message)
    }
  }

  const downloadInvoice = async () => {
    try {
      const blob = await api(`/api/rides/${ride.id}/invoice`, { blob: true })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `invoice-${ride.id}.pdf`
      a.click()
      URL.revokeObjectURL(url)
    } catch (e) {
      setError(e.message)
    }
  }

  const isActive = ['REQUESTED', 'ACCEPTED', 'STARTED'].includes(ride.status)

  return (
    <div className="space-y-4">
      <div className="bg-white rounded-xl shadow p-5 space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="text-xl font-bold">Ride #{ride.id}</h2>
          <StatusBadge status={ride.status} />
        </div>
        <p className="text-sm"><b>From:</b> {ride.pickupAddress}</p>
        <p className="text-sm"><b>To:</b> {ride.dropAddress}</p>
        <p className="text-sm">
          <b>Distance:</b> {ride.distanceKm} km &nbsp; <b>Fare:</b> ₹{ride.fare}
        </p>
        <p className="text-sm">
          <b>Driver:</b> {ride.driverName || 'Waiting for a driver to accept...'}
        </p>
        {error && <p className="bg-red-100 text-red-700 p-2 rounded text-sm">{error}</p>}
        <div className="flex gap-2 flex-wrap">
          {['REQUESTED', 'ACCEPTED'].includes(ride.status) && (
            <button onClick={cancel} className="border border-red-500 text-red-600 px-3 py-1 rounded">
              Cancel ride
            </button>
          )}
          {['ACCEPTED', 'STARTED'].includes(ride.status) && (
            <button onClick={() => publish('sos')} className="bg-red-600 text-white px-3 py-1 rounded">
              🚨 SOS
            </button>
          )}
          {ride.status === 'COMPLETED' && (
            <button onClick={downloadInvoice} className="border px-3 py-1 rounded">
              Download invoice
            </button>
          )}
          {!isActive && (
            <button onClick={onClose} className="bg-black text-white px-3 py-1 rounded">
              Back / Book another ride
            </button>
          )}
        </div>
      </div>

      <RideMap markers={markers} />

      {events.length > 0 && (
        <div className="bg-white rounded-xl shadow p-4">
          <h3 className="font-semibold mb-2">Live updates</h3>
          <ul className="text-sm space-y-1">
            {events.map((e, i) => (
              <li key={i} className={e.danger ? 'text-red-600 font-semibold' : ''}>
                <span className="text-gray-400">{e.time}</span> {e.text}
              </li>
            ))}
          </ul>
        </div>
      )}

      {ride.status === 'COMPLETED' && (
        <>
          <PaymentPanel ride={ride} refreshKey={paymentKey} onPaid={onChanged} />
          <RatingForm rideId={ride.id} />
        </>
      )}
    </div>
  )
}