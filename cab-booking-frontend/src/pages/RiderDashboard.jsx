import { useCallback, useEffect, useState } from 'react'
import { api } from '../api'
import { reverseGeocode } from '../geo'
import RideMap from '../components/RideMap'
import RideDetails from '../components/RideDetails'
import StatusBadge from '../components/StatusBadge'
import LocationSearch from '../components/LocationSearch'

const ACTIVE = ['REQUESTED', 'ACCEPTED', 'STARTED']

export default function RiderDashboard() {
  const [history, setHistory] = useState([])
  const [current, setCurrent] = useState(null)
  const [pickup, setPickup] = useState(null)
  const [drop, setDrop] = useState(null)
  const [estimate, setEstimate] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [booking, setBooking] = useState(false)

  const loadHistory = useCallback(async () => {
    const rides = await api('/api/rides/my')
    setHistory(rides)
    return rides
  }, [])

  useEffect(() => {
    loadHistory()
      .then((rides) => {
        const active = rides.find((r) => ACTIVE.includes(r.status))
        if (active) setCurrent(active)
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }, [loadHistory])

  const refreshCurrent = useCallback(
    async (id) => {
      try {
        setCurrent(await api(`/api/rides/${id}`))
        loadHistory()
      } catch (e) {
        setError(e.message)
      }
    },
    [loadHistory]
  )

  const buildBody = () => ({
    pickupAddress: pickup.address,
    pickupLat: pickup.lat,
    pickupLng: pickup.lng,
    dropAddress: drop.address,
    dropLat: drop.lat,
    dropLng: drop.lng,
  })

  useEffect(() => {
    if (!pickup || !drop) return
    api('/api/rides/estimate', { method: 'POST', body: buildBody() })
      .then(setEstimate)
      .catch((e) => setError(e.message))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pickup?.lat, pickup?.lng, drop?.lat, drop?.lng])

  const handleMapClick = async ({ lat, lng }) => {
    setError('')
    setEstimate(null)
    const coords = `${lat.toFixed(5)}, ${lng.toFixed(5)}`
    if (!pickup || drop) {
      setDrop(null)
      setPickup({ lat, lng, address: coords })
      setPickup({ lat, lng, address: await reverseGeocode(lat, lng) })
    } else {
      setDrop({ lat, lng, address: coords })
      setDrop({ lat, lng, address: await reverseGeocode(lat, lng) })
    }
  }

    const selectPlace = (type, place) => {
      setError('')
      setEstimate(null)
      if (type === 'pickup') setPickup(place)
      else setDrop(place)
    }

  const bookRide = async () => {
    setBooking(true)
    setError('')
    try {
      const ride = await api('/api/rides/book', { method: 'POST', body: buildBody() })
      setCurrent(ride)
      setPickup(null)
      setDrop(null)
      setEstimate(null)
      loadHistory()
    } catch (e) {
      setError(e.message)
    } finally {
      setBooking(false)
    }
  }

  if (loading) return <div className="p-6">Loading...</div>

  const markers = [
    pickup && { ...pickup, label: 'Pickup' },
    drop && { ...drop, label: 'Drop' },
  ].filter(Boolean)

  return (
    <div className="max-w-3xl mx-auto p-6 space-y-6">
      {current ? (
        <RideDetails
          key={current.id}
          ride={current}
          onChanged={() => refreshCurrent(current.id)}
          onClose={() => setCurrent(null)}
        />
      ) : (
        <div className="bg-white rounded-xl shadow p-5 space-y-4">
          <h2 className="text-xl font-bold">Book a ride</h2>
          <LocationSearch label="Search pickup location" onSelect={(p) => selectPlace('pickup', p)} />
              <LocationSearch label="Search drop location" onSelect={(p) => selectPlace('drop', p)} />
          <p className="text-sm text-gray-600">

            Or click the map: first click sets <b>pickup</b>, second sets <b>drop</b>

          </p>
          <RideMap markers={markers} onClick={handleMapClick} />
          {pickup && <p className="text-sm"><b>Pickup:</b> {pickup.address}</p>}
          {drop && <p className="text-sm"><b>Drop:</b> {drop.address}</p>}
          {estimate && (
            <p className="text-sm bg-gray-100 p-3 rounded">
              Distance: <b>{estimate.distanceKm} km</b> &nbsp; Estimated fare: <b>₹{estimate.fare}</b>
            </p>
          )}
          {error && <p className="bg-red-100 text-red-700 p-2 rounded text-sm">{error}</p>}
          <button
            onClick={bookRide}
            disabled={!pickup || !drop || !estimate || booking}
            className="w-full bg-black text-white py-2 rounded disabled:opacity-40"
          >
            {booking ? 'Booking...' : 'Book ride'}
          </button>
        </div>
      )}

      <div className="bg-white rounded-xl shadow p-5">
        <h2 className="text-lg font-bold mb-3">Your rides</h2>
        {history.length === 0 && <p className="text-sm text-gray-500">No rides yet.</p>}
        <ul className="divide-y">
          {history.map((r) => (
            <li key={r.id} className="py-2 flex items-center justify-between text-sm">
              <div>
                <p className="font-medium">#{r.id} · {r.pickupAddress} → {r.dropAddress}</p>
                <p className="text-gray-500">₹{r.fare} · {r.distanceKm} km</p>
              </div>
              <div className="flex items-center gap-3">
                <StatusBadge status={r.status} />
                <button onClick={() => setCurrent(r)} className="text-blue-600 underline">View</button>
              </div>
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}