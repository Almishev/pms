import { createContext, useContext, useState, useEffect } from 'react'
import { login as loginApi } from '../api/authApi'

const AuthContext = createContext(null)

export const useAuth = () => {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const token = localStorage.getItem('token')
    const userData = localStorage.getItem('user')
    if (token && userData) {
      setUser(JSON.parse(userData))
    }
    setLoading(false)
  }, [])

  const login = async (username, password) => {
    try {
      const response = await loginApi(username, password)
      localStorage.setItem('token', response.token)
      const userData = {
        id: response.id,
        username: response.username,
        role: response.role
      }
      localStorage.setItem('user', JSON.stringify(userData))
      setUser(userData)
      return response
    } catch (error) {
      throw error
    }
  }

  const logout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    setUser(null)
  }

  const value = {
    user,
    login,
    logout,
    loading,
    isAuthenticated: !!user,
    isAdmin: user?.role === 'ADMIN'
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

