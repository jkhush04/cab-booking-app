import { useCallback, useEffect, useRef, useState } from 'react'
import { api } from '../api'
import useRideSocket from '../hooks/useRideSocket'
import useDriverRequests from '../hooks/useDriverRequests'
import RideMap from '../components/RideMap'
import LocationSearch from '../components/LocationSearch'
import StatusBadge from '../components/StatusBadge'

const ACTIVE = ['ACCEPTED', 'STARTED']

export default function DriverDashboard() {
  const [profile, setProfile] = useState(null)
  const [history, setHistory] = useState([])
  const [earnings, setEarnings] = useState(null)
  const [ratings, setRatings] = useState([])
  const [requests, setRequests] = useState([])
  const [declined, setDeclined] = useState(new Set())
  const [active, setActive] = useState(null)
  const [myPos, setMyPos] = useState(null)
  const [events, setEvents] = useState([])
  const [simulating, setSimulating] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const timerRef = useRef(null)

  const addEvent = (text, danger = false) =>
    setEvents((prev) =>
      [{ text, danger, time: new Date().toLocaleTimeString() }, ...prev].slice(0, 15))

  const loadAll = useCallback(async () => {
    const [p, rides, e, r] = await Promise.all([
      api('/api/driver/me'),
      api('/api/driver/rides'),
      api('/api/driver/earnings'),
      api('/api/driver/ratings'),
    ])
    setProfile(p)
    setHistory(rides)
    setEarnings(e)
    setRatings(r)
    setActive(rides.find((x) => ACTIVE.includes(x.status)) || null)
    if (p.lat != null && p.lng != null) setMyPos((prev) => prev || { lat: p.lat, lng: p.lng })
  }, [])

  const loadRequests = useCallback(async () => {
    try {
      setRequests(await api('/api/driver/requests'))
    } catch {
      setRequests([])
    }
  }, [])

  useEffect(() => {
    loadAll()
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }, [loadAll])

  useEffect(() => {
    if (profile?.available && !active) loadRequests()
  }, [profile?.available, active, loadRequests])

  useEffect(() => {
    if (!profile?.available || active) return
    const t = setInterval(loadRequests, 10000)
    return () => clearInterval(t)
  }, [profile?.available, active, loadRequests])

  useEffect(() => {
    clearInterval(timerRef.current)
    setSimulating(false)
    setEvents([])
    return () => clearInterval(timerRef.current)
  }, [active?.id, active?.status])

  useDriverRequests(!!profile?.available && !active, () => loadRequests())

  const { publish } = useRideSocket(active?.id, {
    onStatus: (e) => {
      addEvent(e.message)
      if (e.type === 'CANCELLED') loadAll()
    },
    onSos: (e) => addEvent('🚨 ' + e.message, true),
    onError: (msg) => setError(msg),
  })

  const toggleOnline = async () => {
    setError('')
    try {
      await api(`/api/driver/status?available=${!profile.available}`, { method: 'PUT' })
      await loadAll()
    } catch (e) {
      setError(e.message)
    }
  }

  const moveTo = async (pos) => {
    setError('')
    setMyPos({ lat: pos.lat, lng: pos.lng })
    if (active) {
      publish('location', { lat: pos.lat, lng: pos.lng })
      return
    }
    try {
      await api('/api/driver/location', { method: 'PUT', body: { lat: pos.lat, lng: pos.lng } })
      loadRequests()
    } catch (e) {
      setError(e.message)
    }
  }

  const useGps = () => {
    navigator.geolocation.getCurrentPosition(
      (p) => moveTo({ lat: p.coords.latitude, lng: p.coords.longitude }),
      () => setError('Could not get your GPS location')
    )
  }

  const accept = async (id) => {
    setError('')
    try {
      await api(`/api/driver/rides/${id}/accept`, { method: 'POST' })
      await loadAll()
    } catch (e) {
      setError(e.message)
      loadRequests()
    }
  }

  const decline = async (id) => {
    try {
      await api(`/api/driver/rides/${id}/decline`, { method: 'POST' })
    } catch {
      // ignore, ride may already be taken
    }
    setDeclined((prev) => new Set(prev).add(id))
  }

  const rideAction = async (action) => {
    setError('')
    try {
      await api(`/api/driver/rides/${active.id}/${action}`, { method: 'POST' })
      await loadAll()
    } catch (e) {
      setError(e.message)
    }
  }

  const simulate = () => {
    if (!myPos) {
      setError('Set your location first')
      return
    }
    const target = active.status === 'ACCEPTED'
      ? { lat: active.pickupLat, lng: active.pickupLng }
      : { lat: active.dropLat, lng: active.dropLng }
    const start = { ...myPos }
    const steps = 12
    let step = 0
    clearInterval(timerRef.current)
    setSimulating(true)
    timerRef.current = setInterval(() => {
      step += 1
      moveTo({
        lat: start.lat + ((target.lat - start.lat) * step) / steps,
        lng: start.lng + ((target.lng - start.lng) * step) / steps,
      })
      if (step >= steps) {
        clearInterval(timerRef.current)
        setSimulating(false)
      }
    }, 1500)
  }

  if (loading) return <div className="p-6">Loading...</div>
  if (!profile) return <div className="p-6 text-red-600">{error || 'Could not load profile'}</div>

  const visibleRequests = requests.filter((r) => !declined.has(r.id))

  const markers = []
  if (myPos) markers.push({ ...myPos, label: 'You' })
  if (active) {
    markers.push({ lat: active.pickupLat, lng: active.pickupLng, label: 'Pickup' })
    markers.push({ lat: active.dropLat, lng: active.dropLng, label: 'Drop' })
  } else {
    visibleRequests.forEach((r) =>
      markers.push({ lat: r.pickupLat, lng: r.pickupLng, label: `Pickup #${r.id}` }))
  }

  return (
    <div className="max-w-4xl mx-auto p-6 space-y-6">
      <div className="bg-white rounded-xl shadow p-5 flex flex-wrap items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold">{profile.name}</h2>
          <p className="text-sm text-gray-600">{profile.vehicleModel} · {profile.vehicleNumber}</p>
        </div>
        <div className="flex gap-6 text-center">
          <div><p className="text-xs text-gray-500">Earnings</p><p className="font-bold">₹{earnings?.totalEarnings ?? 0}</p></div>
          <div><p className="text-xs text-gray-500">Rides</p><p className="font-bold">{earnings?.completedRides ?? 0}</p></div>
          <div><p className="text-xs text-gray-500">Rating</p><p className="font-bold">{profile.averageRating || '-'} ★</p></div>
        </div>
        {!active && (
          <button
            onClick={toggleOnline}
            className={`px-4 py-2 rounded text-white ${profile.available ? 'bg-green-600' : 'bg-gray-500'}`}
          >
            {profile.available ? '● Online (click to go offline)' : '○ Offline (click to go online)'}
          </button>
        )}
      </div>

      {error && <p className="bg-red-100 text-red-700 p-2 rounded text-sm">{error}</p>}

      <div className="bg-white rounded-xl shadow p-5 space-y-3">
        <h3 className="font-semibold">Your location</h3>
        <LocationSearch label="Search your location" onSelect={moveTo} />
        <div className="flex items-center gap-3 text-sm text-gray-600">
          <button onClick={useGps} className="border px-3 py-1 rounded">Use my GPS</button>
          <span>or click the map to move your car.</span>
        </div>
        <RideMap markers={markers} onClick={moveTo} />
      </div>

      {active ? (
        <div className="bg-white rounded-xl shadow p-5 space-y-3">
          <div className="flex items-center justify-between">
            <h3 className="text-lg font-bold">Current ride #{active.id}</h3>
            <StatusBadge status={active.status} />
          </div>
          <p className="text-sm"><b>From:</b> {active.pickupAddress}</p>
          <p className="text-sm"><b>To:</b> {active.dropAddress}</p>
          <p className="text-sm"><b>Distance:</b> {active.distanceKm} km &nbsp; <b>Fare:</b> ₹{active.fare}</p>
          <div className="flex flex-wrap gap-2">
            <button onClick={simulate} disabled={simulating} className="border px-3 py-1 rounded disabled:opacity-50">
              {simulating ? 'Driving...' : `Simulate drive to ${active.status === 'ACCEPTED' ? 'pickup' : 'drop'}`}
            </button>
            {active.status === 'ACCEPTED' && (
              <>
                <button onClick={() => publish('arrived')} className="border px-3 py-1 rounded">I've arrived</button>
                <button onClick={() => rideAction('start')} className="bg-black text-white px-3 py-1 rounded">Start ride</button>
              </>
            )}
            {active.status === 'STARTED' && (
              <button onClick={() => rideAction('complete')} className="bg-green-600 text-white px-3 py-1 rounded">
                Complete ride
              </button>
            )}
            <button onClick={() => publish('sos')} className="bg-red-600 text-white px-3 py-1 rounded">🚨 SOS</button>
          </div>
          {events.length > 0 && (
            <ul className="text-sm space-y-1 border-t pt-3">
              {events.map((e, i) => (
                <li key={i} className={e.danger ? 'text-red-600 font-semibold' : ''}>
                  <span className="text-gray-400">{e.time}</span> {e.text}
                </li>
              ))}
            </ul>
          )}
        </div>
      ) : (
        <div className="bg-white rounded-xl shadow p-5">
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-lg font-bold">Ride requests nearby</h3>
            <button onClick={loadRequests} className="text-blue-600 underline text-sm">Refresh</button>
          </div>
          {!profile.available && <p className="text-sm text-gray-500">Go online to receive requests.</p>}
          {profile.available && !myPos && <p className="text-sm text-gray-500">Set your location to see nearby requests.</p>}
          {profile.available && myPos && visibleRequests.length === 0 && (
            <p className="text-sm text-gray-500">No requests within 5 km right now.</p>
          )}
          <ul className="divide-y">
            {visibleRequests.map((r) => (
              <li key={r.id} className="py-3 flex items-center justify-between gap-3 text-sm">
                <div>
                  <p className="font-medium">#{r.id} · {r.pickupAddress} → {r.dropAddress}</p>
                  <p className="text-gray-500">{r.distanceKm} km · ₹{r.fare}</p>
                </div>
                <div className="flex gap-2">
                  <button onClick={() => accept(r.id)} className="bg-black text-white px-3 py-1 rounded">Accept</button>
                  <button onClick={() => decline(r.id)} className="border px-3 py-1 rounded">Decline</button>
                </div>
              </li>
            ))}
          </ul>
        </div>
      )}

      <div className="grid md:grid-cols-2 gap-6">
        <div className="bg-white rounded-xl shadow p-5">
          <h3 className="text-lg font-bold mb-3">Ride history</h3>
          {history.length === 0 && <p className="text-sm text-gray-500">No rides yet.</p>}
          <ul className="divide-y">
            {history.map((r) => (
              <li key={r.id} className="py-2 flex items-center justify-between text-sm">
                <span>#{r.id} · ₹{r.fare}</span>
                <StatusBadge status={r.status} />
              </li>
            ))}
          </ul>
        </div>
        <div className="bg-white rounded-xl shadow p-5">
          <h3 className="text-lg font-bold mb-3">Ratings</h3>
          {ratings.length === 0 && <p className="text-sm text-gray-500">No ratings yet.</p>}
          <ul className="divide-y">
            {ratings.map((r) => (
              <li key={r.rideId} className="py-2 text-sm">
                <span className="text-yellow-500">{'★'.repeat(r.stars)}</span>
                <span className="text-gray-300">{'★'.repeat(5 - r.stars)}</span>
                {r.comment && <p className="text-gray-600">{r.comment}</p>}
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  )
}