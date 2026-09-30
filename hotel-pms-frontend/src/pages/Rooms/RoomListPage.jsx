import { useEffect, useState } from 'react'
import {
  getRooms,
  getRoomTypes,
  createRoom,
  createRoomType,
  updateRoom,
  deleteRoom,
  updateRoomType,
  deleteRoomType
} from '../../api/roomApi'
import { getOpenFolios } from '../../api/bookingApi'
import { useAuth } from '../../auth/AuthContext'
import './RoomListPage.css'

const RoomListPage = ({ roomTypeOnly = false }) => {
  const [rooms, setRooms] = useState([])
  const [roomTypes, setRoomTypes] = useState([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [showTypeForm, setShowTypeForm] = useState(false)
  const [showInactiveRooms, setShowInactiveRooms] = useState(false)
  const [editingRoomId, setEditingRoomId] = useState(null)
  const [editingTypeId, setEditingTypeId] = useState(null)
  const [folios, setFolios] = useState([])
  const [formData, setFormData] = useState({ roomNumber: '', roomTypeId: '' })
  const [editFormData, setEditFormData] = useState({ roomNumber: '', roomTypeId: '' })
  const [typeFormData, setTypeFormData] = useState({ name: '', capacity: '', basePrice: '' })
  const [editTypeFormData, setEditTypeFormData] = useState({ name: '', capacity: '', basePrice: '' })
  const [error, setError] = useState('')
  const [editError, setEditError] = useState('')
  const [typeError, setTypeError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [submittingType, setSubmittingType] = useState(false)
  const [editing, setEditing] = useState(false)
  const [editingType, setEditingType] = useState(false)
  const { isAdmin } = useAuth()

  useEffect(() => {
    loadData()
  }, [showInactiveRooms, roomTypeOnly])

  const loadData = async () => {
    try {
      const [roomsRes, typesRes, foliosRes] = await Promise.all([
        getRooms(undefined, undefined, showInactiveRooms || roomTypeOnly),
        getRoomTypes(true),
        roomTypeOnly ? Promise.resolve({ data: [] }) : getOpenFolios()
      ])
      setRooms(roomsRes.data)
      setRoomTypes(typesRes.data)
      setFolios(foliosRes.data)
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

  const handleEditRoom = (room) => {
    setEditingRoomId(room.id)
    setEditFormData({
      roomNumber: room.roomNumber,
      roomTypeId: String(room.roomType.id)
    })
    setEditError('')
    setEditing(true)
  }

  const handleEditSubmit = async (e) => {
    e.preventDefault()
    setEditError('')

    if (!editFormData.roomNumber || !editFormData.roomTypeId) {
      setEditError('Моля, попълнете всички полета')
      return
    }

    try {
      await updateRoom(editingRoomId, {
        roomNumber: editFormData.roomNumber,
        roomTypeId: parseInt(editFormData.roomTypeId)
      })
      setEditingRoomId(null)
      setEditing(false)
      setEditFormData({ roomNumber: '', roomTypeId: '' })
      await loadData()
    } catch (err) {
      setEditError(err.response?.data?.error || 'Неуспешно редактиране на стая')
    }
  }

  const handleEditType = (roomType) => {
    setEditingTypeId(roomType.id)
    setEditTypeFormData({
      name: roomType.name,
      capacity: roomType.capacity,
      basePrice: roomType.basePrice
    })
    setTypeError('')
    setEditingType(true)
  }

  const handleEditTypeSubmit = async (e) => {
    e.preventDefault()
    setTypeError('')

    if (!editTypeFormData.name || !editTypeFormData.capacity || !editTypeFormData.basePrice) {
      setTypeError('Моля, попълнете всички полета')
      return
    }

    try {
      await updateRoomType(editingTypeId, {
        name: editTypeFormData.name,
        capacity: parseInt(editTypeFormData.capacity),
        basePrice: parseFloat(editTypeFormData.basePrice)
      })
      setEditingTypeId(null)
      setEditingType(false)
      setEditTypeFormData({ name: '', capacity: '', basePrice: '' })
      await loadData()
    } catch (err) {
      setTypeError(err.response?.data?.error || 'Неуспешно редактиране на тип стая')
    }
  }

  const handleToggleRoomActive = async (room) => {
    try {
      await updateRoom(room.id, { active: !room.active })
      await loadData()
    } catch (err) {
      setError(err.response?.data?.error || 'Неуспешна промяна на статуса')
    }
  }

  const handleDeleteRoom = async (room) => {
    if (!window.confirm(`Сигурни ли сте, че искате да изтриете стая ${room.roomNumber}?`)) {
      return
    }

    try {
      await deleteRoom(room.id)
      await loadData()
    } catch (err) {
      setError(err.response?.data?.error || 'Неуспешно изтриване на стая')
    }
  }

  const handleDeleteRoomType = async (roomType) => {
    if (!window.confirm(`Сигурни ли сте, че искате да изтриете тип стая ${roomType.name}?`)) {
      return
    }

    try {
      await deleteRoomType(roomType.id)
      await loadData()
    } catch (err) {
      setTypeError(err.response?.data?.error || 'Неуспешно изтриване на тип стая')
    }
  }

  if (loading) {
    return <div className="page-loading">Зареждане...</div>
  }

  return (
    <div className="room-list-page">
      <div className="page-header">
        <h1>{roomTypeOnly ? 'Типове стаи' : 'Стаи'}</h1>
        {isAdmin && (
          <div className="header-buttons">
            {roomTypeOnly ? (
              <button 
                onClick={() => {
                  setShowTypeForm(!showTypeForm)
                  setShowForm(false)
                }} 
                className="btn-primary"
              >
                {showTypeForm ? 'Отказ' : '+ Добави тип стая'}
              </button>
            ) : (
              <>
                <button 
                  onClick={() => {
                    setShowForm(!showForm)
                  }} 
                  className="btn-primary"
                >
                  {showForm ? 'Отказ' : '+ Добави стая'}
                </button>
                <button 
                  onClick={() => setShowInactiveRooms(!showInactiveRooms)} 
                  className="btn-secondary"
                >
                  {showInactiveRooms ? 'Скрий неактивни' : 'Покажи неактивни'}
                </button>
              </>
            )}
          </div>
        )}
      </div>

      {isAdmin && roomTypeOnly && showTypeForm && (
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

      {isAdmin && roomTypeOnly && typeError && !showTypeForm && !editingType && (
        <div className="error-message">{typeError}</div>
      )}

      {isAdmin && roomTypeOnly && roomTypes.length > 0 && (
        <div className="room-type-list">
          {roomTypes.map(roomType => (
            <div key={roomType.id} className="room-type-chip">
              <div>
                <strong>{roomType.name}</strong>
                <span>{roomType.capacity} гости · €{roomType.basePrice}/нощ</span>
              </div>
              <div className="room-actions room-type-actions">
                <button type="button" className="btn-edit" onClick={() => handleEditType(roomType)}>Редактирай</button>
                <button type="button" className="btn-delete" onClick={() => handleDeleteRoomType(roomType)}>Изтрий</button>
              </div>
            </div>
          ))}
        </div>
      )}

      {isAdmin && editingType && (
        <div className="create-room-form">
          <h2>Редактирай тип стая</h2>
          <form onSubmit={handleEditTypeSubmit}>
            {typeError && <div className="error-message">{typeError}</div>}
            <div className="form-group">
              <label>Име</label>
              <input
                type="text"
                value={editTypeFormData.name}
                onChange={(e) => setEditTypeFormData({ ...editTypeFormData, name: e.target.value })}
                required
              />
            </div>
            <div className="form-row">
              <div className="form-group">
                <label>Капацитет</label>
                <input
                  type="number"
                  min="1"
                  value={editTypeFormData.capacity}
                  onChange={(e) => setEditTypeFormData({ ...editTypeFormData, capacity: e.target.value })}
                  required
                />
              </div>
              <div className="form-group">
                <label>Цена (€/нощ)</label>
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  value={editTypeFormData.basePrice}
                  onChange={(e) => setEditTypeFormData({ ...editTypeFormData, basePrice: e.target.value })}
                  required
                />
              </div>
            </div>
            <div className="action-row">
              <button type="submit" className="btn-primary">Запази</button>
              <button type="button" className="btn-secondary" onClick={() => {
                setEditingType(false)
                setEditingTypeId(null)
                setEditTypeFormData({ name: '', capacity: '', basePrice: '' })
                setTypeError('')
              }}>
                Отказ
              </button>
            </div>
          </form>
        </div>
      )}

      {isAdmin && editing && (
        <div className="create-room-form">
          <h2>Редактирай стая</h2>
          <form onSubmit={handleEditSubmit}>
            {editError && <div className="error-message">{editError}</div>}
            <div className="form-group">
              <label>Номер на стая</label>
              <input
                type="text"
                value={editFormData.roomNumber}
                onChange={(e) => setEditFormData({ ...editFormData, roomNumber: e.target.value })}
                required
              />
            </div>
            <div className="form-group">
              <label>Тип стая</label>
              <select
                value={editFormData.roomTypeId}
                onChange={(e) => setEditFormData({ ...editFormData, roomTypeId: e.target.value })}
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
            <div className="action-row">
              <button type="submit" className="btn-primary">Запази</button>
              <button type="button" className="btn-secondary" onClick={() => {
                setEditing(false)
                setEditingRoomId(null)
                setEditFormData({ roomNumber: '', roomTypeId: '' })
                setEditError('')
              }}>
                Отказ
              </button>
            </div>
          </form>
        </div>
      )}
      
      {!roomTypeOnly && (
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

              {(() => {
                const folio = folios.find(item => item.roomId === room.id)
                if (!folio) {
                  return (
                    <div className="room-account">
                      <h4>Сметка</h4>
                      <p>Няма настанена резервация</p>
                    </div>
                  )
                }
                const restaurant = Number(folio.restaurantCharge || 0)
                const total = Number(folio.nightsTotal || 0) + restaurant
                return (
                  <div className="room-account">
                    <h4>Сметка · {folio.guestName}</h4>
                    <div className="account-line">
                      <span>Нощувки</span>
                      <strong>€{Number(folio.nightsTotal || 0).toFixed(2)}</strong>
                    </div>
                    <div className="account-line">
                      <span>Ресторант</span>
                      <strong>€{restaurant.toFixed(2)}</strong>
                    </div>
                    <div className="account-line total">
                      <span>Общо</span>
                      <strong>€{total.toFixed(2)}</strong>
                    </div>
                  </div>
                )
              })()}
              {isAdmin && (
                <div className="room-actions">
                  <button type="button" className="btn-edit" onClick={() => handleEditRoom(room)}>
                    Редактирай
                  </button>
                  <button type="button" className="btn-toggle" onClick={() => handleToggleRoomActive(room)}>
                    {room.active ? 'Деактивирай' : 'Активирай'}
                  </button>
                  <button type="button" className="btn-delete" onClick={() => handleDeleteRoom(room)}>
                    Изтрий
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

export default RoomListPage

