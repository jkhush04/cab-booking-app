import { useCallback, useEffect, useState } from 'react'
import { loadStripe } from '@stripe/stripe-js'
import { Elements, PaymentElement, useElements, useStripe } from '@stripe/react-stripe-js'
import { api } from '../api'

const stripePromise = loadStripe(import.meta.env.VITE_STRIPE_PK)

function CheckoutForm({ onConfirmed, onError }) {
  const stripe = useStripe()
  const elements = useElements()
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
    const [ready, setReady] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!stripe || !elements) return
    setBusy(true)
    setError('')
    try{
    const result = await stripe.confirmPayment({ elements, redirect: 'if_required' })
    if (result.error) {
      setError(result.error.message)
      setBusy(false)
      onError?.()
    } else {
      onConfirmed()
    }
}catch(err){
    setError(err.message || 'Payment failed')
    setBusy(false)
    onError?.()
}
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <PaymentElement
        onReady={() => setReady(true)}
        onLoadError={(e) => setError(e.error?.message || 'Card form failed to load')}
      />
      {error && <p className="text-red-600 text-sm">{error}</p>}
      <button disabled={!stripe || busy || !ready} className="w-full bg-black text-white py-2 rounded disabled:opacity-50">
        {busy ? 'Processing...' : 'Pay now'}
      </button>
    </form>
  )
}

export default function PaymentPanel({ ride, refreshKey, onPaid }) {
  const [payment, setPayment] = useState(null)
  const [clientSecret, setClientSecret] = useState('')
  const [error, setError] = useState('')
  const [waiting, setWaiting] = useState(false)
  const [loading, setLoading] = useState(true)

  const loadPayment = useCallback(async () => {
    try {
      const p = await api(`/api/payments/ride/${ride.id}`)
      setPayment(p)
      setLoading(false)
      return p
    } catch (err) {
      console.error('Failed to load payment:', err)
      setPayment(null)
      setLoading(false)
      return null
    }
  }, [ride.id])

  useEffect(() => {
    loadPayment()
  }, [loadPayment, refreshKey])

  // Clear clientSecret if payment is already in terminal state
  useEffect(() => {
    if (payment?.status === 'SUCCEEDED' || payment?.status === 'FAILED') {
      setClientSecret('')
    }
  }, [payment])

  const startPayment = async () => {
    setError('')
    try {
      const res = await api(`/api/payments/ride/${ride.id}/intent`, { method: 'POST' })
      setClientSecret(res.clientSecret)
    } catch (e) {
      setError(e.message)
    }
  }

  const waitForWebhook = async () => {
    setWaiting(true)
    for (let i = 0; i < 10; i++) {
      const p = await loadPayment()
      if (p?.status === 'SUCCEEDED') {
        setClientSecret('')
        setWaiting(false)
        onPaid?.()
        return
      }
      await new Promise((r) => setTimeout(r, 1500))
    }
    setWaiting(false)
    setError('Payment is still processing. Check again in a moment.')
    setClientSecret('')
  }

  if (loading) {
    return (
      <div className="bg-white border rounded-xl p-4 space-y-3">
        <h3 className="font-semibold">Payment</h3>
        <p className="text-sm text-gray-600">Loading payment status...</p>
      </div>
    )
  }

  if (payment?.status === 'SUCCEEDED') {
    return (
      <div className="bg-green-50 border border-green-200 rounded-xl p-4">
        ✅ Paid ₹{payment.amount}
      </div>
    )
  }

  return (
    <div className="bg-white border rounded-xl p-4 space-y-3">
      <h3 className="font-semibold">Payment</h3>
      {error && <p className="text-red-600 text-sm">{error}</p>}
      {waiting && <p className="text-sm text-gray-600">Confirming payment...</p>}
      {!clientSecret && !waiting && (
        <button onClick={startPayment} className="bg-black text-white px-4 py-2 rounded">
          Pay ₹{ride.fare}
        </button>
      )}
      {clientSecret && !waiting && (
        <Elements stripe={stripePromise} options={{ clientSecret }}>
          <CheckoutForm onConfirmed={waitForWebhook} onError={() => setClientSecret('')} />
        </Elements>
      )}
      <p className="text-xs text-gray-500">Test card: 4242 4242 4242 4242, any future date, any CVC.</p>
    </div>
  )
}