import {createContext, useContext, useState} from 'react'
import { getAuth } from './api'


const AuthContext=createContext(null)

export function AuthProvider({children}){
    const [auth, setAuth] = useState(getAuth())

    const login= (data) => {
         localStorage.setItem('auth',JSON.stringify(data))
         setAuth(data)
        }


    const logout=() => {
        localStorage.removeItem('auth')
        setAuth(null)
        }

     return <AuthContext.Provider value={{ auth, login, logout }}>{children}</AuthContext.Provider>
    }

export const useAuth = () => useContext(AuthContext)
