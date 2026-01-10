import { useEffect, useState } from 'react'
import { getBookings } from '../../api/bookingApi'
import { getRooms } from '../../api/roomApi'
import { format } from 'date-fns'
import './DashboardPage.css'

const DashboardPage = () => {
  const [bookings, setBookings] = useState([])
  const [rooms, setRooms] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    try {
      const [bookingsRes, roomsRes] = await Promise.all([
        getBookings(),
        getRooms()
      ])
      setBookings(bookingsRes.data)
      setRooms(roomsRes.data)
    } catch (error) {
      console.error('Error loading data:', error)
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return <div className="page-loading">Зареждане...</div>
  }

  const todayBookings = bookings.filter(b => {
    const checkIn = new Date(b.checkInDate)
    const today = new Date()
    return checkIn.toDateString() === today.toDateString()
  })

  const activeBookings = bookings.filter(b => 
    b.status === 'BOOKED' || b.status === 'CHECKED_IN'
  )

  return (
    <div className="dashboard-page">
      <h1>Табло</h1>
      
      <div className="dashboard-stats">
        <div className="stat-card">
          <h3>Общо стаи</h3>
          <p className="stat-value">{rooms.length}</p>
        </div>
        <div className="stat-card">
          <h3>Активни резервации</h3>
          <p className="stat-value">{activeBookings.length}</p>
        </div>
        <div className="stat-card">
          <h3>Настанявания днес</h3>
          <p className="stat-value">{todayBookings.length}</p>
        </div>
      </div>

      <div className="dashboard-section">
        <h2>Настанявания за днес</h2>
        {todayBookings.length === 0 ? (
          <p>Няма настанявания за днес</p>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Стая</th>
                <th>Гост</th>
                <th>Настаняване</th>
                <th>Напускане</th>
                <th>Статус</th>
              </tr>
            </thead>
            <tbody>
              {todayBookings.map(booking => (
                <tr key={booking.id}>
                  <td>{booking.room.roomNumber}</td>
                  <td>{booking.guest.firstName} {booking.guest.lastName}</td>
                  <td>{format(new Date(booking.checkInDate), 'MMM dd, yyyy')}</td>
                  <td>{format(new Date(booking.checkOutDate), 'MMM dd, yyyy')}</td>
                  <td>
                    <span className={`status-badge status-${booking.status.toLowerCase()}`}>
                      {booking.status === 'BOOKED' ? 'Резервирана' : booking.status === 'CHECKED_IN' ? 'Настанена' : booking.status === 'CHECKED_OUT' ? 'Напуснала' : booking.status}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}

export default DashboardPage

