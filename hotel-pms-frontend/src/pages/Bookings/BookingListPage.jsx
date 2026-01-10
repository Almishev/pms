import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getBookings, checkIn, checkOut, cancelBooking } from '../../api/bookingApi'
import { format } from 'date-fns'
import './BookingListPage.css'

const BookingListPage = () => {
  const [bookings, setBookings] = useState([])
  const [loading, setLoading] = useState(true)
  const navigate = useNavigate()

  useEffect(() => {
    loadBookings()
  }, [])

  const loadBookings = async () => {
    try {
      const response = await getBookings()
      setBookings(response.data)
    } catch (error) {
      console.error('Error loading bookings:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleCheckIn = async (id) => {
    try {
      await checkIn(id)
      loadBookings()
    } catch (error) {
      alert(error.response?.data?.error || 'Неуспешно настаняване')
    }
  }

  const handleCheckOut = async (id) => {
    try {
      await checkOut(id)
      loadBookings()
    } catch (error) {
      alert(error.response?.data?.error || 'Неуспешно напускане')
    }
  }

  const handleCancel = async (id) => {
    if (!window.confirm('Сигурни ли сте, че искате да отмените тази резервация?')) {
      return
    }
    try {
      await cancelBooking(id)
      loadBookings()
    } catch (error) {
      alert(error.response?.data?.error || 'Неуспешна отмяна на резервация')
    }
  }

  if (loading) {
    return <div className="page-loading">Зареждане...</div>
  }

  return (
    <div className="booking-list-page">
      <div className="page-header">
        <h1>Резервации</h1>
        <button onClick={() => navigate('/bookings/new')} className="btn-primary">
          Нова резервация
        </button>
      </div>

      <table className="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Стая</th>
            <th>Гост</th>
            <th>Настаняване</th>
            <th>Напускане</th>
            <th>Статус</th>
            <th>Действия</th>
          </tr>
        </thead>
        <tbody>
          {bookings.map(booking => (
            <tr key={booking.id}>
              <td>#{booking.id}</td>
              <td>{booking.room.roomNumber}</td>
              <td>{booking.guest.firstName} {booking.guest.lastName}</td>
              <td>{format(new Date(booking.checkInDate), 'MMM dd, yyyy')}</td>
              <td>{format(new Date(booking.checkOutDate), 'MMM dd, yyyy')}</td>
              <td>
                <span className={`status-badge status-${booking.status.toLowerCase()}`}>
                  {booking.status === 'BOOKED' ? 'Резервирана' : booking.status === 'CHECKED_IN' ? 'Настанена' : booking.status === 'CHECKED_OUT' ? 'Напуснала' : booking.status === 'CANCELLED' ? 'Отменена' : booking.status}
                </span>
              </td>
              <td>
                <div className="action-buttons">
                  {booking.status === 'BOOKED' && (
                    <button onClick={() => handleCheckIn(booking.id)} className="btn-sm btn-success">
                      Настани
                    </button>
                  )}
                  {booking.status === 'CHECKED_IN' && (
                    <button onClick={() => handleCheckOut(booking.id)} className="btn-sm btn-info">
                      Напускане
                    </button>
                  )}
                  {booking.status !== 'CHECKED_OUT' && booking.status !== 'CANCELLED' && (
                    <button onClick={() => handleCancel(booking.id)} className="btn-sm btn-danger">
                      Отмени
                    </button>
                  )}
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export default BookingListPage

