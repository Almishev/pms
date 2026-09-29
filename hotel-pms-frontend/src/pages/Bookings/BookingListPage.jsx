import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getBookings, checkIn, checkOut, cancelBooking } from '../../api/bookingApi'
import { format, parseISO } from 'date-fns'
import './BookingListPage.css'

const statusLabels = {
  BOOKED: 'Резервирана',
  CHECKED_IN: 'Настанена',
  CHECKED_OUT: 'Напуснала',
  CANCELLED: 'Отменена',
}

const queues = [
  { id: 'active', label: 'Активни' },
  { id: 'arrivals', label: 'Пристигащи днес' },
  { id: 'inhouse', label: 'Настанени' },
  { id: 'departures', label: 'Заминаващи днес' },
  { id: 'upcoming', label: 'Предстоящи' },
  { id: 'past', label: 'Минали' },
  { id: 'cancelled', label: 'Отменени' },
  { id: 'all', label: 'Всички' },
]

const todayIso = () => {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${day}`
}

const isoDate = (value) => String(value).slice(0, 10)

const BookingListPage = () => {
  const [bookings, setBookings] = useState([])
  const [loading, setLoading] = useState(true)
  const [queue, setQueue] = useState('active')
  const [searchTerm, setSearchTerm] = useState('')
  const [arrivalFrom, setArrivalFrom] = useState('')
  const [arrivalTo, setArrivalTo] = useState('')
  const [sortField, setSortField] = useState('checkInDate')
  const [sortDirection, setSortDirection] = useState('asc')
  const [currentPage, setCurrentPage] = useState(1)
  const [itemsPerPage, setItemsPerPage] = useState(25)
  const navigate = useNavigate()

  useEffect(() => {
    loadBookings()
  }, [])

  useEffect(() => {
    setCurrentPage(1)
  }, [queue, searchTerm, arrivalFrom, arrivalTo, sortField, sortDirection, itemsPerPage])

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

  const handleSort = (field) => {
    if (sortField === field) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc')
    } else {
      setSortField(field)
      setSortDirection('asc')
    }
  }

  const sortMark = (field) => {
    if (sortField !== field) return ''
    return sortDirection === 'asc' ? ' ↑' : ' ↓'
  }

  const today = todayIso()

  const filteredAndSortedBookings = bookings
    .filter(booking => {
      const checkIn = isoDate(booking.checkInDate)
      const checkOut = isoDate(booking.checkOutDate)

      if (queue === 'active' && booking.status !== 'BOOKED' && booking.status !== 'CHECKED_IN') return false
      if (queue === 'arrivals' && (checkIn !== today || booking.status === 'CANCELLED')) return false
      if (queue === 'inhouse' && booking.status !== 'CHECKED_IN') return false
      if (queue === 'departures' && (checkOut !== today || booking.status === 'CANCELLED')) return false
      if (queue === 'upcoming' && (booking.status !== 'BOOKED' || checkIn < today)) return false
      if (queue === 'past' && booking.status !== 'CHECKED_OUT') return false
      if (queue === 'cancelled' && booking.status !== 'CANCELLED') return false

      if (arrivalFrom && checkIn < arrivalFrom) return false
      if (arrivalTo && checkIn > arrivalTo) return false

      if (searchTerm) {
        const searchLower = searchTerm.toLowerCase()
        const guestName = `${booking.guest.firstName} ${booking.guest.lastName}`.toLowerCase()
        const roomNumber = booking.room.roomNumber.toLowerCase()
        const bookingId = String(booking.id)
        if (!guestName.includes(searchLower) && !roomNumber.includes(searchLower) && !bookingId.includes(searchLower)) {
          return false
        }
      }

      return true
    })
    .sort((a, b) => {
      let aValue
      let bValue

      switch (sortField) {
        case 'id':
          aValue = a.id
          bValue = b.id
          break
        case 'roomNumber':
          aValue = a.room.roomNumber
          bValue = b.room.roomNumber
          break
        case 'guestName':
          aValue = `${a.guest.firstName} ${a.guest.lastName}`
          bValue = `${b.guest.firstName} ${b.guest.lastName}`
          break
        case 'checkOutDate':
          aValue = isoDate(a.checkOutDate)
          bValue = isoDate(b.checkOutDate)
          break
        case 'status':
          aValue = a.status
          bValue = b.status
          break
        default:
          aValue = isoDate(a.checkInDate)
          bValue = isoDate(b.checkInDate)
      }

      let result = 0
      if (typeof aValue === 'string') {
        result = aValue.localeCompare(bValue, 'bg', { numeric: true })
      } else if (aValue < bValue) {
        result = -1
      } else if (aValue > bValue) {
        result = 1
      }

      return sortDirection === 'asc' ? result : -result
    })

  const totalPages = Math.max(1, Math.ceil(filteredAndSortedBookings.length / itemsPerPage))
  const safePage = Math.min(currentPage, totalPages)
  const startIndex = (safePage - 1) * itemsPerPage
  const paginatedBookings = filteredAndSortedBookings.slice(startIndex, startIndex + itemsPerPage)
  const rangeFrom = filteredAndSortedBookings.length === 0 ? 0 : startIndex + 1
  const rangeTo = startIndex + paginatedBookings.length

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

      <div className="queue-tabs">
        {queues.map(item => (
          <button
            key={item.id}
            type="button"
            className={`queue-tab ${queue === item.id ? 'active' : ''}`}
            onClick={() => setQueue(item.id)}
          >
            {item.label}
          </button>
        ))}
      </div>

      <div className="filters-section">
        <input
          type="text"
          placeholder="Търсене по гост, стая или номер..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="search-input"
        />
        <div className="filter-controls">
          <label>
            Настаняване от
            <input
              type="date"
              value={arrivalFrom}
              onChange={(e) => setArrivalFrom(e.target.value)}
            />
          </label>
          <label>
            до
            <input
              type="date"
              value={arrivalTo}
              onChange={(e) => setArrivalTo(e.target.value)}
            />
          </label>
          {(searchTerm || arrivalFrom || arrivalTo) && (
            <button
              type="button"
              className="btn-secondary"
              onClick={() => {
                setSearchTerm('')
                setArrivalFrom('')
                setArrivalTo('')
              }}
            >
              Изчисти
            </button>
          )}
        </div>
      </div>

      <div className="results-info">
        Показани {rangeFrom}–{rangeTo} от {filteredAndSortedBookings.length} резервации
      </div>

      <table className="data-table">
        <thead>
          <tr>
            <th><button type="button" className="sort-header" onClick={() => handleSort('id')}>Номер{sortMark('id')}</button></th>
            <th><button type="button" className="sort-header" onClick={() => handleSort('roomNumber')}>Стая{sortMark('roomNumber')}</button></th>
            <th><button type="button" className="sort-header" onClick={() => handleSort('guestName')}>Гост{sortMark('guestName')}</button></th>
            <th><button type="button" className="sort-header" onClick={() => handleSort('checkInDate')}>Настаняване{sortMark('checkInDate')}</button></th>
            <th><button type="button" className="sort-header" onClick={() => handleSort('checkOutDate')}>Напускане{sortMark('checkOutDate')}</button></th>
            <th><button type="button" className="sort-header" onClick={() => handleSort('status')}>Статус{sortMark('status')}</button></th>
            <th>Действия</th>
          </tr>
        </thead>
        <tbody>
          {paginatedBookings.length === 0 ? (
            <tr>
              <td colSpan="7" className="no-results">Няма намерени резервации</td>
            </tr>
          ) : paginatedBookings.map(booking => (
            <tr key={booking.id}>
              <td>#{booking.id}</td>
              <td>{booking.room.roomNumber}</td>
              <td>{booking.guest.firstName} {booking.guest.lastName}</td>
              <td>{format(parseISO(isoDate(booking.checkInDate)), 'dd.MM.yyyy')}</td>
              <td>{format(parseISO(isoDate(booking.checkOutDate)), 'dd.MM.yyyy')}</td>
              <td>
                <span className={`status-badge status-${booking.status.toLowerCase()}`}>
                  {statusLabels[booking.status] || booking.status}
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
                  {booking.status === 'BOOKED' && (
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

      <div className="pagination">
        <button
          type="button"
          onClick={() => setCurrentPage(page => Math.max(1, page - 1))}
          disabled={safePage === 1}
          className="pagination-button"
        >
          ← Предишна
        </button>
        <span className="pagination-info">Страница {safePage} от {totalPages}</span>
        <button
          type="button"
          onClick={() => setCurrentPage(page => Math.min(totalPages, page + 1))}
          disabled={safePage === totalPages}
          className="pagination-button"
        >
          Следваща →
        </button>
        <select
          value={itemsPerPage}
          onChange={(e) => setItemsPerPage(parseInt(e.target.value, 10))}
          className="pagination-select"
        >
          <option value="10">10 на страница</option>
          <option value="25">25 на страница</option>
          <option value="50">50 на страница</option>
        </select>
      </div>
    </div>
  )
}

export default BookingListPage
