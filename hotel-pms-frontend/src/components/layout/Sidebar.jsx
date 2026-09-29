import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import './Sidebar.css'

const Sidebar = () => {
  const location = useLocation()
  const { isAdmin } = useAuth()

  const menuItems = [
    { path: '/dashboard', label: 'Табло', icon: '📊' },
    { path: '/rooms', label: 'Стаи', icon: '🛏️' },
    { path: '/room-types', label: 'Типове стаи', icon: '🏷️', adminOnly: true },
    { path: '/bookings', label: 'Резервации', icon: '📅' },
    { path: '/bookings/new', label: 'Нова резервация', icon: '➕' },
    { path: '/payments', label: 'Плащания', icon: '💳' },
    { path: '/reports/fiscal', label: 'Фискални отчети', icon: '🧾', adminOnly: true },
    { path: '/reports/occupancy', label: 'Отчет за заетост', icon: '📈' },
    { path: '/reports/room-calendar', label: 'Календар на стаите', icon: '📆' },
    { path: '/reports/nights', label: 'Начисления', icon: '🌙' },
    { path: '/reports/revenue', label: 'Плащания', icon: '💰' },
    { path: '/reports/nsi', label: 'Справка за НСИ', icon: '🏛️' },
  ]

  return (
    <aside className="sidebar">
      <nav className="sidebar-nav">
        {menuItems
          .filter(item => !item.adminOnly || isAdmin)
          .map((item) => (
            <Link
              key={item.path}
              to={item.path}
              className={`sidebar-link ${location.pathname === item.path ? 'active' : ''}`}
            >
              <span className="sidebar-icon">{item.icon}</span>
              <span>{item.label}</span>
            </Link>
          ))}
      </nav>
    </aside>
  )
}

export default Sidebar

