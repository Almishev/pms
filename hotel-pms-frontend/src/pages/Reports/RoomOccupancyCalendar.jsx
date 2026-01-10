import { useEffect, useState } from 'react'
import { getRoomOccupancyCalendar } from '../../api/reportApi'
import { format, addDays, parseISO, eachDayOfInterval, startOfWeek, endOfWeek } from 'date-fns'
import './RoomOccupancyCalendar.css'

const RoomOccupancyCalendar = () => {
  const [calendarData, setCalendarData] = useState(null)
  const [startDate, setStartDate] = useState(format(new Date(), 'yyyy-MM-dd'))
  const [endDate, setEndDate] = useState(format(addDays(new Date(), 30), 'yyyy-MM-dd'))
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    loadCalendar()
  }, [])

  const loadCalendar = async () => {
    if (!startDate || !endDate) return
    
    setLoading(true)
    try {
      const response = await getRoomOccupancyCalendar(startDate, endDate)
      setCalendarData(response.data)
    } catch (error) {
      console.error('Error loading calendar:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    loadCalendar()
  }

  const handlePrint = () => {
    window.print()
  }

  if (loading) {
    return <div className="page-loading">Зареждане...</div>
  }

  if (!calendarData) {
    return (
      <div className="room-occupancy-calendar-page">
        <h1>Календар на заетост на стаите</h1>
        <form onSubmit={handleSubmit} className="calendar-filters">
          <div className="form-group">
            <label>Начална дата</label>
            <input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              required
            />
          </div>
          <div className="form-group">
            <label>Крайна дата</label>
            <input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              required
            />
          </div>
          <button type="submit" className="btn-primary">Зареди календар</button>
        </form>
      </div>
    )
  }

  const dates = eachDayOfInterval({
    start: parseISO(calendarData.startDate),
    end: parseISO(calendarData.endDate)
  })

  const getStatusClass = (occupied, status) => {
    if (!occupied) return 'available'
    if (status === 'CHECKED_IN') return 'checked-in'
    if (status === 'BOOKED') return 'booked'
    if (status === 'CHECKED_OUT') return 'checked-out'
    return 'occupied'
  }

  const getStatusLabel = (occupied, status) => {
    if (!occupied) return 'Свободна'
    if (status === 'CHECKED_IN') return 'Настанена'
    if (status === 'BOOKED') return 'Резервирана'
    if (status === 'CHECKED_OUT') return 'Напуснала'
    return 'Заета'
  }

  return (
    <div className="room-occupancy-calendar-page">
      <div className="calendar-header">
        <h1>Календар на заетост на стаите</h1>
        <div className="calendar-actions">
          <form onSubmit={handleSubmit} className="calendar-filters">
            <div className="form-group">
              <label>Начална дата</label>
              <input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                required
              />
            </div>
            <div className="form-group">
              <label>Крайна дата</label>
              <input
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                required
              />
            </div>
            <button type="submit" className="btn-primary">Зареди</button>
          </form>
          <button onClick={handlePrint} className="btn-print">
            🖨️ Принтирай
          </button>
        </div>
      </div>

      <div className="calendar-container print-container">
        <div className="calendar-info">
          <p><strong>Период:</strong> {format(parseISO(calendarData.startDate), 'MMM dd, yyyy')} - {format(parseISO(calendarData.endDate), 'MMM dd, yyyy')}</p>
          <p><strong>Общо стаи:</strong> {calendarData.rooms?.length || 0}</p>
        </div>

        <div className="calendar-table-wrapper">
          <table className="occupancy-calendar-table">
            <thead>
              <tr>
                <th className="room-column">Стая</th>
                {dates.map(date => (
                  <th key={date.toISOString()} className="date-column">
                    <div className="date-header">
                      <div className="date-day">{format(date, 'dd')}</div>
                      <div className="date-weekday">{format(date, 'EEE')}</div>
                    </div>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {calendarData.rooms?.map(room => (
                <tr key={room.id}>
                  <td className="room-cell">
                    <div className="room-info">
                      <strong>{room.roomNumber}</strong>
                      <span className="room-type">{room.roomType}</span>
                    </div>
                  </td>
                  {dates.map(date => {
                    const dateKey = format(date, 'yyyy-MM-dd')
                    const roomStatus = calendarData.calendar[dateKey]?.[room.roomNumber]
                    const occupied = roomStatus?.occupied || false
                    const status = roomStatus?.status || ''
                    const guestName = roomStatus?.guestName || ''
                    
                    return (
                      <td 
                        key={`${room.id}-${dateKey}`}
                        className={`status-cell ${getStatusClass(occupied, status)}`}
                        title={occupied ? `${guestName} - ${getStatusLabel(occupied, status)}` : 'Свободна'}
                      >
                        {occupied && (
                          <div className="status-content">
                            <div className="status-indicator"></div>
                            {guestName && <div className="guest-name">{guestName.split(' ')[0]}</div>}
                          </div>
                        )}
                      </td>
                    )
                  })}
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="legend print-legend">
          <h3>Легенда</h3>
          <div className="legend-items">
            <div className="legend-item">
              <span className="legend-color available"></span>
              <span>Свободна</span>
            </div>
            <div className="legend-item">
              <span className="legend-color booked"></span>
              <span>Резервирана</span>
            </div>
            <div className="legend-item">
              <span className="legend-color checked-in"></span>
              <span>Настанена</span>
            </div>
            <div className="legend-item">
              <span className="legend-color checked-out"></span>
              <span>Напуснала</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

export default RoomOccupancyCalendar

