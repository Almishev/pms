import { Navigate } from 'react-router-dom'
import { useAuth } from './AuthContext'

const RoleRoute = ({ children, requiredRole }) => {
  const { isAuthenticated, user, loading } = useAuth()

  if (loading) {
    return <div>Loading...</div>
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" />
  }

  if (requiredRole && user?.role !== requiredRole) {
    return <Navigate to="/dashboard" />
  }

  return children
}

export default RoleRoute

