import { useAuth } from '../../auth/AuthContext'
import { useNavigate } from 'react-router-dom'
import './Header.css'

const Header = () => {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <header className="header">
      <div className="header-content">
        <h1 className="header-title">Хотел PMS</h1>
        <div className="header-user">
          <span>{user?.username} ({user?.role === 'ADMIN' ? 'Администратор' : user?.role === 'RECEPTIONIST' ? 'Рецепционист' : user?.role})</span>
          <button onClick={handleLogout} className="btn-logout">Изход</button>
        </div>
      </div>
    </header>
  )
}

export default Header

