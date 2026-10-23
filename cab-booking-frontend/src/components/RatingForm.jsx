import { useState } from 'react'
import { api } from '../api'

export default function RatingForm({ rideId }) {
  const [stars, setStars] = useState(5)
  const [comment, setComment] = useState('')
  const [message, setMessage] = useState('')
  const [done, setDone] = useState(false)

  const submit = async () => {
    setMessage('')
    try {
      await api(`/api/rides/${rideId}/rate`, { method: 'POST', body: { stars, comment } })
      setDone(true)
    } catch (e) {
      setMessage(e.message)
    }
  }

  if (done) return <div className="bg-green-50 border border-green-200 rounded-xl p-4">Thanks for rating! ⭐</div>

  return (
    <div className="bg-white border rounded-xl p-4 space-y-3">
      <h3 className="font-semibold">Rate your driver</h3>
      <div className="flex gap-1">
        {[1, 2, 3, 4, 5].map((n) => (
          <button key={n} onClick={() => setStars(n)}
            className={`text-2xl ${n <= stars ? 'text-yellow-500' : 'text-gray-300'}`}>
            ★
          </button>
        ))}
      </div>
      <textarea placeholder="Comment (optional)" className="w-full border rounded px-3 py-2"
        value={comment} onChange={(e) => setComment(e.target.value)} />
      {message && <p className="text-red-600 text-sm">{message}</p>}
      <button onClick={submit} className="bg-black text-white px-4 py-2 rounded">Submit rating</button>
    </div>
  )
}