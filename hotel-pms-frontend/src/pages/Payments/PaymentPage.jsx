import { useEffect, useState } from 'react'
import { getBookings } from '../../api/bookingApi'
import { processPayment, getPaymentsByBooking } from '../../api/paymentApi'
import { getStayNightsByBooking, updateStayNightsPrice } from '../../api/stayApi'
import { format } from 'date-fns'
import './PaymentPage.css'

const PaymentPage = () => {
  const [bookings, setBookings] = useState([])
  const [selectedBooking, setSelectedBooking] = useState(null)
  const [payments, setPayments] = useState([])
  const [stayNights, setStayNights] = useState([])
  const [showPaymentModal, setShowPaymentModal] = useState(false)
  const [showPriceModal, setShowPriceModal] = useState(false)
  const [paymentData, setPaymentData] = useState({
    amount: '',
    paymentMethod: 'CASH'
  })
  const [priceData, setPriceData] = useState({
    pricePerNight: ''
  })
  const [loading, setLoading] = useState(false)
  const [priceLoading, setPriceLoading] = useState(false)
  const [error, setError] = useState('')
  const [priceError, setPriceError] = useState('')
  
  // Filtering and sorting
  const [searchTerm, setSearchTerm] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [sortField, setSortField] = useState('checkInDate')
  const [sortDirection, setSortDirection] = useState('desc')
  
  // Pagination
  const [currentPage, setCurrentPage] = useState(1)
  const [itemsPerPage, setItemsPerPage] = useState(10)

  useEffect(() => {
    loadBookings()
  }, [])

  useEffect(() => {
    if (selectedBooking) {
      loadPayments(selectedBooking.id)
      loadStayNights(selectedBooking.id)
    }
  }, [selectedBooking])

  const loadBookings = async () => {
    try {
      const response = await getBookings()
      setBookings(response.data.filter(b => b.status !== 'CANCELLED'))
    } catch (error) {
      console.error('Error loading bookings:', error)
    }
  }

  const loadPayments = async (bookingId) => {
    try {
      const response = await getPaymentsByBooking(bookingId)
      setPayments(response.data)
    } catch (error) {
      console.error('Грешка при зареждане на плащания:', error)
    }
  }

  const loadStayNights = async (bookingId) => {
    try {
      const response = await getStayNightsByBooking(bookingId)
      setStayNights(response.data)
    } catch (error) {
      console.error('Грешка при зареждане на нощувки:', error)
    }
  }

  const handlePayment = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      await processPayment({
        bookingId: selectedBooking.id,
        amount: parseFloat(paymentData.amount),
        paymentMethod: paymentData.paymentMethod
      })
      setShowPaymentModal(false)
      setPaymentData({ amount: '', paymentMethod: 'CASH' })
      loadPayments(selectedBooking.id)
    } catch (err) {
      setError(err.response?.data?.error || 'Неуспешно обработване на плащане')
    } finally {
      setLoading(false)
    }
  }

  const handleUpdatePrice = async (e) => {
    e.preventDefault()
    setPriceError('')
    setPriceLoading(true)

    try {
      await updateStayNightsPrice(selectedBooking.id, parseFloat(priceData.pricePerNight))
      setShowPriceModal(false)
      setPriceData({ pricePerNight: '' })
      loadStayNights(selectedBooking.id)
    } catch (err) {
      setPriceError(err.response?.data?.error || 'Неуспешна промяна на цената')
    } finally {
      setPriceLoading(false)
    }
  }

  // Calculate totals
  const totalAmount = stayNights.reduce((sum, night) => sum + parseFloat(night.price || 0), 0)
  const paidAmount = payments.reduce((sum, payment) => sum + parseFloat(payment.amount || 0), 0)
  const remainingAmount = totalAmount - paidAmount
  const currentPricePerNight = stayNights.length > 0 ? parseFloat(stayNights[0].price) : 0

  // Filter and sort bookings
  const filteredAndSortedBookings = bookings
    .filter(booking => {
      // Status filter
      if (statusFilter !== 'ALL' && booking.status !== statusFilter) {
        return false
      }
      
      // Search filter (by guest name or room number)
      if (searchTerm) {
        const searchLower = searchTerm.toLowerCase()
        const guestName = `${booking.guest.firstName} ${booking.guest.lastName}`.toLowerCase()
        const roomNumber = booking.room.roomNumber.toLowerCase()
        const bookingId = booking.id.toString()
        
        if (!guestName.includes(searchLower) && 
            !roomNumber.includes(searchLower) && 
            !bookingId.includes(searchLower)) {
          return false
        }
      }
      
      return true
    })
    .sort((a, b) => {
      let aValue, bValue
      
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
        case 'checkInDate':
          aValue = new Date(a.checkInDate)
          bValue = new Date(b.checkInDate)
          break
        case 'checkOutDate':
          aValue = new Date(a.checkOutDate)
          bValue = new Date(b.checkOutDate)
          break
        case 'status':
          aValue = a.status
          bValue = b.status
          break
        default:
          aValue = new Date(a.checkInDate)
          bValue = new Date(b.checkInDate)
      }
      
      if (aValue < bValue) return sortDirection === 'asc' ? -1 : 1
      if (aValue > bValue) return sortDirection === 'asc' ? 1 : -1
      return 0
    })

  // Pagination
  const totalPages = Math.ceil(filteredAndSortedBookings.length / itemsPerPage)
  const startIndex = (currentPage - 1) * itemsPerPage
  const endIndex = startIndex + itemsPerPage
  const paginatedBookings = filteredAndSortedBookings.slice(startIndex, endIndex)

  // Reset to first page when filters change
  useEffect(() => {
    setCurrentPage(1)
  }, [searchTerm, statusFilter, sortField, sortDirection])

  const handleSort = (field) => {
    if (sortField === field) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc')
    } else {
      setSortField(field)
      setSortDirection('asc')
    }
  }

  return (
    <div className="payment-page">
      <h1>Плащания</h1>

      <div className="payment-container">
        <div className="booking-list">
          <h2>Избери резервация</h2>
          
          {/* Filters and Search */}
          <div className="filters-section">
            <div className="search-box">
              <input
                type="text"
                placeholder="Търсене по гост, стая или номер..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="search-input"
              />
            </div>
            <div className="filter-controls">
              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                className="filter-select"
              >
                <option value="ALL">Всички статуси</option>
                <option value="BOOKED">Резервирана</option>
                <option value="CHECKED_IN">Настанена</option>
                <option value="CHECKED_OUT">Напуснала</option>
              </select>
              <select
                value={sortField}
                onChange={(e) => setSortField(e.target.value)}
                className="filter-select"
              >
                <option value="checkInDate">Сортиране по дата настаняване</option>
                <option value="checkOutDate">Сортиране по дата напускане</option>
                <option value="id">Сортиране по номер</option>
                <option value="roomNumber">Сортиране по стая</option>
                <option value="guestName">Сортиране по гост</option>
                <option value="status">Сортиране по статус</option>
              </select>
              <button
                onClick={() => handleSort(sortField)}
                className="sort-button"
                title={sortDirection === 'asc' ? 'Възходящо' : 'Низходящо'}
              >
                {sortDirection === 'asc' ? '↑' : '↓'}
              </button>
            </div>
          </div>

          {/* Results count */}
          <div className="results-info">
            <span>Показани {paginatedBookings.length} от {filteredAndSortedBookings.length} резервации</span>
          </div>

          {/* Booking Cards */}
          <div className="booking-cards">
            {paginatedBookings.length === 0 ? (
              <div className="no-results">
                <p>Няма намерени резервации</p>
              </div>
            ) : (
              paginatedBookings.map(booking => (
                <div
                  key={booking.id}
                  className={`booking-card ${selectedBooking?.id === booking.id ? 'selected' : ''}`}
                  onClick={() => setSelectedBooking(booking)}
                >
                  <div className="booking-card-header">
                    <span>#{booking.id}</span>
                    <span className={`status-badge status-${booking.status.toLowerCase()}`}>
                      {booking.status === 'BOOKED' ? 'Резервирана' : booking.status === 'CHECKED_IN' ? 'Настанена' : booking.status === 'CHECKED_OUT' ? 'Напуснала' : booking.status}
                    </span>
                  </div>
                  <p><strong>Стая:</strong> {booking.room.roomNumber}</p>
                  <p><strong>Гост:</strong> {booking.guest.firstName} {booking.guest.lastName}</p>
                  <p><strong>Настаняване:</strong> {format(new Date(booking.checkInDate), 'dd MMM, yyyy')}</p>
                  <p><strong>Напускане:</strong> {format(new Date(booking.checkOutDate), 'dd MMM, yyyy')}</p>
                </div>
              ))
            )}
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="pagination">
              <button
                onClick={() => setCurrentPage(prev => Math.max(1, prev - 1))}
                disabled={currentPage === 1}
                className="pagination-button"
              >
                ← Предишна
              </button>
              <span className="pagination-info">
                Страница {currentPage} от {totalPages}
              </span>
              <button
                onClick={() => setCurrentPage(prev => Math.min(totalPages, prev + 1))}
                disabled={currentPage === totalPages}
                className="pagination-button"
              >
                Следваща →
              </button>
              <select
                value={itemsPerPage}
                onChange={(e) => {
                  setItemsPerPage(parseInt(e.target.value))
                  setCurrentPage(1)
                }}
                className="pagination-select"
              >
                <option value="5">5 на страница</option>
                <option value="10">10 на страница</option>
                <option value="20">20 на страница</option>
                <option value="50">50 на страница</option>
              </select>
            </div>
          )}
        </div>

        {selectedBooking && (
          <div className="payment-details">
            <div className="payment-header">
              <h2>Детайли на плащане - Резервация #{selectedBooking.id}</h2>
              <div className="header-buttons">
                <button onClick={() => setShowPriceModal(true)} className="btn-secondary">
                  Промени цена
                </button>
                <button onClick={() => setShowPaymentModal(true)} className="btn-primary">
                  Добави плащане
                </button>
              </div>
            </div>

            <div className="payment-summary">
              <div className="summary-card">
                <h3>Обща сума</h3>
                <p className="summary-value">€{totalAmount.toFixed(2)}</p>
                <small>{stayNights.length} нощувки × €{currentPricePerNight.toFixed(2)}</small>
              </div>
              <div className="summary-card">
                <h3>Платена сума</h3>
                <p className="summary-value paid">€{paidAmount.toFixed(2)}</p>
              </div>
              <div className="summary-card">
                <h3>Оставаща сума</h3>
                <p className={`summary-value ${remainingAmount > 0 ? 'remaining' : 'paid-full'}`}>
                  €{remainingAmount.toFixed(2)}
                </p>
              </div>
            </div>

            <div className="payments-list">
              <h3>История на плащанията</h3>
              {payments.length === 0 ? (
                <p>Няма записани плащания</p>
              ) : (
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Дата</th>
                      <th>Сума</th>
                      <th>Метод</th>
                    </tr>
                  </thead>
                  <tbody>
                    {payments.map(payment => (
                      <tr key={payment.id}>
                        <td>{format(new Date(payment.paymentDate), 'dd MMM, yyyy HH:mm')}</td>
                        <td>€{payment.amount}</td>
                        <td>{payment.paymentMethod === 'CASH' ? 'В брой' : payment.paymentMethod === 'CARD' ? 'С карта' : payment.paymentMethod}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </div>
        )}
      </div>

      {showPaymentModal && (
        <div className="modal-overlay" onClick={() => setShowPaymentModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h2>Обработка на плащане</h2>
            {error && <div className="error-message">{error}</div>}
            <div className="payment-info">
              <p><strong>Обща сума:</strong> €{totalAmount.toFixed(2)}</p>
              <p><strong>Платена сума:</strong> €{paidAmount.toFixed(2)}</p>
              <p><strong>Оставаща сума:</strong> €{remainingAmount.toFixed(2)}</p>
            </div>
            <form onSubmit={handlePayment}>
              <div className="form-group">
                <label>Сума *</label>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  max={remainingAmount}
                  value={paymentData.amount}
                  onChange={(e) => setPaymentData({...paymentData, amount: e.target.value})}
                  placeholder={`Макс: €${remainingAmount.toFixed(2)}`}
                  required
                />
              </div>
              <div className="form-group">
                <label>Метод на плащане *</label>
                <select
                  value={paymentData.paymentMethod}
                  onChange={(e) => setPaymentData({...paymentData, paymentMethod: e.target.value})}
                  required
                >
                  <option value="CASH">В брой</option>
                  <option value="CARD">С карта</option>
                </select>
              </div>
              <div className="modal-actions">
                <button type="button" onClick={() => setShowPaymentModal(false)} className="btn-secondary">
                  Отказ
                </button>
                <button type="submit" disabled={loading} className="btn-primary">
                  {loading ? 'Обработване...' : 'Обработи плащане'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {showPriceModal && (
        <div className="modal-overlay" onClick={() => setShowPriceModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h2>Промяна на цена за нощ</h2>
            {priceError && <div className="error-message">{priceError}</div>}
            <form onSubmit={handleUpdatePrice}>
              <div className="form-group">
                <label>Текуща цена за нощ</label>
                <input
                  type="number"
                  step="0.01"
                  value={currentPricePerNight.toFixed(2)}
                  disabled
                  className="disabled-input"
                />
              </div>
              <div className="form-group">
                <label>Нова цена за нощ *</label>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  value={priceData.pricePerNight}
                  onChange={(e) => setPriceData({...priceData, pricePerNight: e.target.value})}
                  placeholder={`Въведете нова цена (текуща: €${currentPricePerNight.toFixed(2)})`}
                  required
                />
                <small className="form-hint">
                  Всички {stayNights.length} нощувки ще бъдат обновени с новата цена
                </small>
              </div>
              <div className="modal-actions">
                <button type="button" onClick={() => {
                  setShowPriceModal(false)
                  setPriceData({ pricePerNight: '' })
                  setPriceError('')
                }} className="btn-secondary">
                  Отказ
                </button>
                <button type="submit" disabled={priceLoading} className="btn-primary">
                  {priceLoading ? 'Запазване...' : 'Запази цена'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}

export default PaymentPage

