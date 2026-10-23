import { useState } from 'react'

export default function LocationSearch({ label, onSelect }) {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState([])
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')

  const search = async () => {
    if (query.trim().length < 3) return
    setLoading(true)
    setMessage('')
    try {
      const res = await fetch(
        `https://nominatim.openstreetmap.org/search?format=jsonv2&limit=5&countrycodes=in&q=${encodeURIComponent(query)}`
      )
      const data = await res.json()
      setResults(data)
      if (data.length === 0) setMessage('No places found. Try a more specific name.')
    } catch {
      setMessage('Search failed, please try again.')
    } finally {
      setLoading(false)
    }
  }

  const pick = (place) => {
    onSelect({
      lat: parseFloat(place.lat),
      lng: parseFloat(place.lon),
      address: place.display_name.split(',').slice(0, 3).join(','),
    })
    setResults([])
    setQuery('')
  }

  return (
    <div className="relative">
      <div className="flex gap-2">
        <input
          className="flex-1 border rounded px-3 py-2 text-sm"
          placeholder={label}
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') {
              e.preventDefault()
              search()
            }
          }}
        />
        <button onClick={search} disabled={loading} className="bg-black text-white px-3 rounded text-sm disabled:opacity-50">
          {loading ? '...' : 'Search'}
        </button>
      </div>
      {message && <p className="text-xs text-gray-500 mt-1">{message}</p>}
      {results.length > 0 && (
        <ul className="absolute z-[1000] w-full bg-white border rounded shadow mt-1 max-h-60 overflow-auto">
          {results.map((p) => (
            <li
              key={p.place_id}
              onClick={() => pick(p)}
              className="px-3 py-2 text-sm hover:bg-gray-100 cursor-pointer"
            >
              {p.display_name}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}