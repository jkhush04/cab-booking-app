import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api'
import { useAuth } from '../AuthContext'

export default function Register() {
  const [form, setForm] = useState({
    name: '', email: '', phone: '', password: '',
    role: 'RIDER', vehicleModel: '', vehicleNumber: '',
  })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const { login } = useAuth()
  const navigate = useNavigate()

  const set = (field) => (e) => setForm({ ...form, [field]: e.target.value })

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const data = await api('/api/auth/register', { method: 'POST', body: form })
      login(data)
      navigate('/')
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-md mx-auto mt-10 bg-white p-8 rounded-xl shadow">
      <h1 className="text-2xl font-bold mb-6">Create account</h1>
      {error && <p className="bg-red-100 text-red-700 p-2 rounded mb-4 text-sm">{error}</p>}
      <form onSubmit={handleSubmit} className="space-y-4">
        <input placeholder="Full name" required className="w-full border rounded px-3 py-2"
          value={form.name} onChange={set('name')} />
        <input type="email" placeholder="Email" required className="w-full border rounded px-3 py-2"
          value={form.email} onChange={set('email')} />
        <input placeholder="Phone" className="w-full border rounded px-3 py-2"
          value={form.phone} onChange={set('phone')} />
        <input type="password" placeholder="Password (min 6 chars)" required minLength={6}
          className="w-full border rounded px-3 py-2"
          value={form.password} onChange={set('password')} />
        <select className="w-full border rounded px-3 py-2" value={form.role} onChange={set('role')}>
          <option value="RIDER">I'm a rider</option>
          <option value="DRIVER">I'm a driver</option>
        </select>
        {form.role === 'DRIVER' && (
          <>
            <input placeholder="Vehicle model" required className="w-full border rounded px-3 py-2"
              value={form.vehicleModel} onChange={set('vehicleModel')} />
            <input placeholder="Vehicle number" required className="w-full border rounded px-3 py-2"
              value={form.vehicleNumber} onChange={set('vehicleNumber')} />
          </>
        )}
        <button disabled={loading} className="w-full bg-black text-white py-2 rounded disabled:opacity-50">
          {loading ? 'Creating...' : 'Register'}
        </button>
      </form>
      <p className="text-sm mt-4">
        Already have an account? <Link to="/login" className="text-blue-600 underline">Login</Link>
      </p>
    </div>
  )
}