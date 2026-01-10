import { useEffect, useState } from 'react'
import { getRooms, getRoomTypes, createRoom, createRoomType } from '../../api/roomApi'
import { useAuth } from '../../auth/AuthContext'
import './RoomListPage.css'

const RoomListPage = () => {
  const [rooms, setRooms] = useState([])
  const [roomTypes, setRoomTypes] = useState([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [showTypeForm, setShowTypeForm] = useState(false)
  const [formData, setFormData] = useState({ roomNumber: '', roomTypeId: '' })
  const [typeFormData, setTypeFormData] = useState({ name: '', capacity: '', basePrice: '' })
  const [error, setError] = useState('')
  const [typeError, setTypeError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [submittingType, setSubmittingType] = useState(false)
  const { isAdmin } = useAuth()

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    try {
      const [roomsRes, typesRes] = await Promise.all([
        getRooms(),
        getRoomTypes()
      ])
      setRooms(roomsRes.data)
      setRoomTypes(typesRes.data)
    } catch (error) {
      console.error('Error loading rooms:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setSubmitting(true)

    if (!formData.roomNumber || !formData.roomTypeId) {
      setError('Моля, попълнете всички полета')
      setSubmitting(false)
      return
    }

    try {
      await createRoom(formData.roomNumber, parseInt(formData.roomTypeId))
      setFormData({ roomNumber: '', roomTypeId: '' })
      setShowForm(false)
      await loadData() // Reload rooms
    } catch (err) {
      setError(err.response?.data?.error || 'Неуспешно създаване на стая')
    } finally {
      setSubmitting(false)
    }
  }

  const handleTypeSubmit = async (e) => {
    e.preventDefault()
    setTypeError('')
    setSubmittingType(true)

    if (!typeFormData.name || !typeFormData.capacity || !typeFormData.basePrice) {
      setTypeError('Моля, попълнете всички полета')
      setSubmittingType(false)
      return
    }

    try {
      await createRoomType(
        typeFormData.name,
        parseInt(typeFormData.capacity),
        parseFloat(typeFormData.basePrice)
      )
      setTypeFormData({ name: '', capacity: '', basePrice: '' })
      setShowTypeForm(false)
      await loadData() // Reload room types
    } catch (err) {
      setTypeError(err.response?.data?.error || 'Неуспешно създаване на тип стая')
    } finally {
      setSubmittingType(false)
    }
  }

  if (loading) {
    return <div className="page-loading">Зареждане...</div>
  }

  return (
    <div className="room-list-page">
      <div className="page-header">
        <h1>Стаи</h1>
        {isAdmin && (
          <div className="header-buttons">
            <button 
              onClick={() => {
                setShowTypeForm(!showTypeForm)
                setShowForm(false)
              }} 
              className="btn-secondary"
            >
              {showTypeForm ? 'Отказ' : '+ Добави тип стая'}
            </button>
            <button 
              onClick={() => {
                setShowForm(!showForm)
                setShowTypeForm(false)
              }} 
              className="btn-primary"
            >
              {showForm ? 'Отказ' : '+ Добави стая'}
            </button>
          </div>
        )}
      </div>

      {isAdmin && showTypeForm && (
        <div className="create-room-form">
          <h2>Създай нов тип стая</h2>
          <form onSubmit={handleTypeSubmit}>
            {typeError && <div className="error-message">{typeError}</div>}
            <div className="form-group">
              <label>Име</label>
              <input
                type="text"
                value={typeFormData.name}
                onChange={(e) => setTypeFormData({ ...typeFormData, name: e.target.value })}
                placeholder="напр. Делюкс, Стандарт"
                required
              />
            </div>
            <div className="form-row">
              <div className="form-group">
                <label>Капацитет (гости)</label>
                <input
                  type="number"
                  min="1"
                  value={typeFormData.capacity}
                  onChange={(e) => setTypeFormData({ ...typeFormData, capacity: e.target.value })}
                  placeholder="напр. 2"
                  required
                />
              </div>
              <div className="form-group">
                <label>Базова цена (€/нощ)</label>
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  value={typeFormData.basePrice}
                  onChange={(e) => setTypeFormData({ ...typeFormData, basePrice: e.target.value })}
                  placeholder="напр. 100.00"
                  required
                />
              </div>
            </div>
            <button type="submit" disabled={submittingType} className="btn-primary">
              {submittingType ? 'Създаване...' : 'Създай тип стая'}
            </button>
          </form>
        </div>
      )}

      {isAdmin && showForm && (
        <div className="create-room-form">
          <h2>Създай нова стая</h2>
          <form onSubmit={handleSubmit}>
            {error && <div className="error-message">{error}</div>}
            <div className="form-group">
              <label>Номер на стая</label>
              <input
                type="text"
                value={formData.roomNumber}
                onChange={(e) => setFormData({ ...formData, roomNumber: e.target.value })}
                placeholder="напр. 101"
                required
              />
            </div>
            <div className="form-group">
              <label>Тип стая</label>
              <select
                value={formData.roomTypeId}
                onChange={(e) => setFormData({ ...formData, roomTypeId: e.target.value })}
                required
              >
                <option value="">Избери тип стая</option>
                {roomTypes.map(type => (
                  <option key={type.id} value={type.id}>
                    {type.name} - Капацитет: {type.capacity}, Цена: €{type.basePrice}/нощ
                  </option>
                ))}
              </select>
            </div>
            <button type="submit" disabled={submitting} className="btn-primary">
              {submitting ? 'Създаване...' : 'Създай стая'}
            </button>
          </form>
        </div>
      )}
      
      <div className="room-grid">
        {rooms.map(room => (
          <div key={room.id} className={`room-card ${room.active ? '' : 'inactive'}`}>
            <div className="room-header">
              <h3>Стая {room.roomNumber}</h3>
              <span className={`room-status ${room.active ? 'active' : 'inactive'}`}>
                {room.active ? 'Активна' : 'Неактивна'}
              </span>
            </div>
            <div className="room-details">
              <p><strong>Тип:</strong> {room.roomType.name}</p>
              <p><strong>Капацитет:</strong> {room.roomType.capacity} гости</p>
              <p><strong>Цена:</strong> €{room.roomType.basePrice}/нощ</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

export default RoomListPage

