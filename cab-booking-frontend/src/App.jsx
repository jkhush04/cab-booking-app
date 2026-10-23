import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './AuthContext'
import Navbar from './components/Navbar'
import Login from './pages/Login'
import Register from './pages/Register'
import RiderDashboard from './pages/RiderDashboard'
import DriverDashboard from './pages/DriverDashboard'

function Protected({ role, children }) {
  const { auth } = useAuth()
  if (!auth) return <Navigate to="/login" replace />
  if (auth.role !== role) return <Navigate to="/" replace />
  return children
}

function Home() {
  const { auth } = useAuth()
  if (!auth) return <Navigate to="/login" replace />
  return <Navigate to={auth.role === 'RIDER' ? '/rider' : '/driver'} replace />
}

export default function App() {
  return (
    <div className="min-h-screen bg-gray-50 text-gray-900">
      <Navbar />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/rider" element={<Protected role="RIDER"><RiderDashboard /></Protected>} />
        <Route path="/driver" element={<Protected role="DRIVER"><DriverDashboard /></Protected>} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </div>
  )
}