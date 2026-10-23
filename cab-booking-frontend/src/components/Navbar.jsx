import {Link, useNavigate} from 'react-router-dom'
import {useAuth} from '../AuthContext'

export default function Navbar(){
    const {auth , logout}=useAuth()
    const navigate=useNavigate()

    const handleLogout=()=>{
        logout()
        navigate('/login')
        }


      return (
         <nav className="bg-black text-white px-6 py-3 flex items-center justify-between">
           <Link to="/" className="font-bold text-lg">🚖 CabBooking</Link>
           {auth && (
             <div className="flex items-center gap-4 text-sm">
               <span>{auth.email} ({auth.role})</span>
               <button onClick={handleLogout} className="bg-white text-black px-3 py-1 rounded">
                 Logout
               </button>
             </div>
           )}
         </nav>
       )
   }
