import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createBooking } from '../../api/bookingApi'
import { createGuest } from '../../api/guestApi'
import { getRooms } from '../../api/roomApi'
import './NewBookingPage.css'

const NewBookingPage = () => {
  const [rooms, setRooms] = useState([])
  const [formData, setFormData] = useState({
    roomId: '',
    guestFirstName: '',
    guestLastName: '',
    guestPhone: '',
    guestIdNumber: '',
    checkInDate: '',
    checkOutDate: '',
    customPricePerNight: ''
  })
  const [selectedRoom, setSelectedRoom] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    loadRooms()
  }, [])

  useEffect(() => {
    if (formData.checkInDate && formData.checkOutDate) {
      loadAvailableRooms()
    } else {
      loadRooms()
    }
  }, [formData.checkInDate, formData.checkOutDate])

  useEffect(() => {
    // Update selected room when rooms change
    if (formData.roomId) {
      const room = rooms.find(r => r.id === parseInt(formData.roomId))
      setSelectedRoom(room)
    } else {
      setSelectedRoom(null)
    }
  }, [rooms, formData.roomId])

  const loadRooms = async () => {
    try {
      const response = await getRooms()
      setRooms(response.data)
    } catch (error) {
      console.error('Error loading rooms:', error)
    }
  }

  const loadAvailableRooms = async () => {
    try {
      const response = await getRooms(formData.checkInDate, formData.checkOutDate)
      setRooms(response.data)
      // Clear room selection if selected room is no longer available
      if (formData.roomId && !response.data.find(r => r.id === parseInt(formData.roomId))) {
        setFormData({ ...formData, roomId: '' })
      }
    } catch (error) {
      console.error('Error loading available rooms:', error)
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      // Create guest first
      const guestRes = await createGuest({
        firstName: formData.guestFirstName,
        lastName: formData.guestLastName,
        phone: formData.guestPhone,
        idNumber: formData.guestIdNumber
      })

      // Create booking
      const bookingData = {
        roomId: parseInt(formData.roomId),
        guestId: guestRes.data.id,
        checkInDate: formData.checkInDate,
        checkOutDate: formData.checkOutDate
      }
      
      // Add custom price if provided
      if (formData.customPricePerNight && formData.customPricePerNight.trim() !== '') {
        bookingData.customPricePerNight = parseFloat(formData.customPricePerNight)
      }
      
      await createBooking(bookingData)

      navigate('/bookings')
    } catch (err) {
      setError(err.response?.data?.error || 'Неуспешно създаване на резервация')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="new-booking-page">
      <h1>Нова резервация</h1>

      <form onSubmit={handleSubmit} className="booking-form">
        {error && <div className="error-message">{error}</div>}

        <div className="form-section">
          <h2>Информация за гост</h2>
          <div className="form-row">
            <div className="form-group">
              <label>Име *</label>
              <input
                type="text"
                value={formData.guestFirstName}
                onChange={(e) => setFormData({...formData, guestFirstName: e.target.value})}
                required
              />
            </div>
            <div className="form-group">
              <label>Фамилия *</label>
              <input
                type="text"
                value={formData.guestLastName}
                onChange={(e) => setFormData({...formData, guestLastName: e.target.value})}
                required
              />
            </div>
          </div>
          <div className="form-row">
            <div className="form-group">
              <label>Телефон</label>
              <input
                type="tel"
                value={formData.guestPhone}
                onChange={(e) => setFormData({...formData, guestPhone: e.target.value})}
              />
            </div>
            <div className="form-group">
              <label>ЕГН</label>
              <input
                type="text"
                value={formData.guestIdNumber}
                onChange={(e) => setFormData({...formData, guestIdNumber: e.target.value})}
              />
            </div>
          </div>
        </div>

        <div className="form-section">
          <h2>Детайли на резервацията</h2>
          <div className="form-row">
            <div className="form-group">
              <label>Дата на настаняване *</label>
              <input
                type="date"
                value={formData.checkInDate}
                onChange={(e) => setFormData({...formData, checkInDate: e.target.value, roomId: ''})}
                required
                min={new Date().toISOString().split('T')[0]}
              />
            </div>
            <div className="form-group">
              <label>Дата на напускане *</label>
              <input
                type="date"
                value={formData.checkOutDate}
                onChange={(e) => setFormData({...formData, checkOutDate: e.target.value, roomId: ''})}
                required
                min={formData.checkInDate || new Date().toISOString().split('T')[0]}
              />
            </div>
          </div>
          <div className="form-row">
            <div className="form-group">
              <label>Стая *</label>
              <select
                value={formData.roomId}
                onChange={(e) => {
                  const roomId = e.target.value
                  const room = rooms.find(r => r.id === parseInt(roomId))
                  setSelectedRoom(room)
                  setFormData({...formData, roomId: roomId, customPricePerNight: ''})
                }}
                required
                disabled={!formData.checkInDate || !formData.checkOutDate}
              >
                <option value="">
                  {!formData.checkInDate || !formData.checkOutDate 
                    ? 'Моля, изберете първо дати' 
                    : rooms.length === 0 
                      ? 'Няма свободни стаи за избраните дати' 
                      : 'Избери стая'}
                </option>
                {rooms.map(room => (
                  <option key={room.id} value={room.id}>
                    {room.roomNumber} - {room.roomType.name} (€{room.roomType.basePrice}/нощ)
                  </option>
                ))}
              </select>
            </div>
            {selectedRoom && (
              <div className="form-group">
                <label>Персонализирана цена за нощ (опционално)</label>
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  value={formData.customPricePerNight}
                  onChange={(e) => setFormData({...formData, customPricePerNight: e.target.value})}
                  placeholder={`Базова цена: €${selectedRoom.roomType.basePrice}/нощ`}
                />
                <small className="form-hint">
                  Оставете празно за базова цена (€{selectedRoom.roomType.basePrice}/нощ) или въведете различна цена
                </small>
              </div>
            )}
          </div>
        </div>

        <div className="form-actions">
          <button type="button" onClick={() => navigate('/bookings')} className="btn-secondary">
            Отказ
          </button>
          <button type="submit" disabled={loading} className="btn-primary">
            {loading ? 'Създаване...' : 'Създай резервация'}
          </button>
        </div>
      </form>
    </div>
  )
}

export default NewBookingPage

