import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { addDays, eachDayOfInterval, format, parseISO } from 'date-fns'
import { bg } from 'date-fns/locale'
import { getRoomOccupancyCalendar } from '../../api/reportApi'
import { cancelBooking, checkIn, checkOut, moveBooking } from '../../api/bookingApi'
import './RoomOccupancyCalendar.css'

const VISIBLE_DAYS = 14
const DRAG_THRESHOLD = 6

const todayIso = () => format(new Date(), 'yyyy-MM-dd')

const statusLabel = (status) => {
  if (status === 'CHECKED_IN') return 'Настанен'
  if (status === 'BOOKED') return 'Резервация'
  if (status === 'CHECKED_OUT') return 'Напуснал'
  return status || ''
}

const barClass = (status) => {
  if (status === 'CHECKED_IN') return 'in-house'
  if (status === 'BOOKED') return 'reserved'
  return 'other'
}

const sortRooms = (rooms) =>
  [...rooms].sort((a, b) =>
    String(a.roomNumber).localeCompare(String(b.roomNumber), 'bg', { numeric: true })
  )

const staysForRoom = (room, dates, calendar) => {
  const stays = []
  let current = null

  dates.forEach((date, index) => {
    const key = format(date, 'yyyy-MM-dd')
    const cell = calendar?.[key]?.[room.roomNumber]
    const bookingId = cell?.occupied ? cell.bookingId : null

    if (current && current.bookingId === bookingId) {
      current.endIndex = index + 1
      return
    }

    if (current) stays.push(current)
    current = bookingId
      ? {
          bookingId,
          guestName: cell.guestName,
          status: cell.status,
          checkInDate: cell.checkInDate,
          checkOutDate: cell.checkOutDate,
          roomNumber: room.roomNumber,
          roomType: room.roomType,
          startIndex: index,
          endIndex: index + 1
        }
      : null
  })

  if (current) stays.push(current)
  return stays
}

const RoomOccupancyCalendar = () => {
  const navigate = useNavigate()
  const dragRef = useRef(null)
  const [anchor, setAnchor] = useState(todayIso())
  const [calendarData, setCalendarData] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [moving, setMoving] = useState(false)
  const [selected, setSelected] = useState(null)
  const [preview, setPreview] = useState(null)

  const start = parseISO(anchor)
  const end = addDays(start, VISIBLE_DAYS - 1)
  const dates = eachDayOfInterval({ start, end })
  const rooms = sortRooms(calendarData?.rooms || [])
  const today = todayIso()

  useEffect(() => {
    loadCalendar(anchor)
  }, [anchor])

  const loadCalendar = async (startDate) => {
    setLoading(true)
    setError('')
    try {
      const endDate = format(addDays(parseISO(startDate), VISIBLE_DAYS - 1), 'yyyy-MM-dd')
      const response = await getRoomOccupancyCalendar(startDate, endDate)
      setCalendarData(response.data)
    } catch (err) {
      setError('Календарът не се зареди')
    } finally {
      setLoading(false)
    }
  }

  const shift = (days) => {
    setSelected(null)
    setAnchor(format(addDays(parseISO(anchor), days), 'yyyy-MM-dd'))
  }

  const occupiedOn = (date) => {
    const key = format(date, 'yyyy-MM-dd')
    return rooms.filter((room) => calendarData?.calendar?.[key]?.[room.roomNumber]?.occupied).length
  }

  const startBooking = (room, iso) => {
    if (iso < today) return
    const checkOut = format(addDays(parseISO(iso), 1), 'yyyy-MM-dd')
    navigate(`/bookings/new?roomId=${room.id}&checkIn=${iso}&checkOut=${checkOut}`)
  }

  const applyMove = async (stay, roomIndex, dayDelta, roomDelta) => {
    const nextIndex = roomIndex + roomDelta
    if (nextIndex < 0 || nextIndex >= rooms.length) {
      setError('Стаята е извън списъка')
      return
    }
    if (!stay.checkInDate || !stay.checkOutDate) {
      setError('Датите на резервацията липсват. Презареди страницата.')
      return
    }
    const room = rooms[nextIndex]
    const checkInDate = format(addDays(parseISO(stay.checkInDate), dayDelta), 'yyyy-MM-dd')
    const checkOutDate = format(addDays(parseISO(stay.checkOutDate), dayDelta), 'yyyy-MM-dd')
    setMoving(true)
    setError('')
    try {
      await moveBooking(stay.bookingId, { roomId: room.id, checkInDate, checkOutDate })
      setSelected(null)
      await loadCalendar(anchor)
    } catch (err) {
      setError(err.response?.data?.error || 'Резервацията не беше преместена')
    } finally {
      setMoving(false)
    }
  }

  const onBarPointerDown = (event, stay, roomIndex) => {
    if (event.button !== 0 || moving) return
    event.preventDefault()
    event.stopPropagation()
    const row = event.currentTarget.closest('.tape-row')
    const track = event.currentTarget.parentElement
    dragRef.current = {
      stay,
      roomIndex,
      originX: event.clientX,
      originY: event.clientY,
      colWidth: track.getBoundingClientRect().width / dates.length,
      rowHeight: row.getBoundingClientRect().height,
      moved: false
    }
    event.currentTarget.setPointerCapture(event.pointerId)
  }

  const onBarPointerMove = (event, bookingId) => {
    const drag = dragRef.current
    if (!drag || drag.stay.bookingId !== bookingId) return
    const dx = event.clientX - drag.originX
    const dy = event.clientY - drag.originY
    if (Math.abs(dx) > DRAG_THRESHOLD || Math.abs(dy) > DRAG_THRESHOLD) {
      drag.moved = true
    }
    if (!drag.moved) return
    setPreview({
      bookingId,
      dayDelta: Math.round(dx / drag.colWidth),
      roomDelta: Math.round(dy / drag.rowHeight),
      colWidth: drag.colWidth,
      rowHeight: drag.rowHeight
    })
  }

  const onBarPointerUp = (event, stay) => {
    const drag = dragRef.current
    dragRef.current = null
    setPreview(null)
    if (!drag || drag.stay.bookingId !== stay.bookingId) return
    if (!drag.moved) {
      setSelected(stay)
      return
    }
    const dayDelta = Math.round((event.clientX - drag.originX) / drag.colWidth)
    const roomDelta = Math.round((event.clientY - drag.originY) / drag.rowHeight)
    if (dayDelta === 0 && roomDelta === 0) {
      setSelected(stay)
      return
    }
    applyMove(stay, drag.roomIndex, dayDelta, roomDelta)
  }

  const runAction = async (action) => {
    if (!selected) return
    if (action === 'cancel' && !window.confirm('Да се отмени ли резервацията?')) return
    setMoving(true)
    setError('')
    try {
      if (action === 'check-in') await checkIn(selected.bookingId)
      if (action === 'check-out') await checkOut(selected.bookingId)
      if (action === 'cancel') await cancelBooking(selected.bookingId)
      setSelected(null)
      await loadCalendar(anchor)
    } catch (err) {
      setError(err.response?.data?.error || 'Действието не беше изпълнено')
    } finally {
      setMoving(false)
    }
  }

  return (
    <div className="room-occupancy-calendar-page">
      <div className="calendar-header">
        <div>
          <h1>Календар на стаите</h1>
          <p className="tape-range">
            {format(start, 'dd.MM.yyyy')} – {format(end, 'dd.MM.yyyy')}
          </p>
        </div>
        <div className="tape-toolbar">
          <button type="button" className="tape-nav" onClick={() => shift(-7)}>‹ Седмица</button>
          <button type="button" className="tape-nav" onClick={() => setAnchor(todayIso())}>Днес</button>
          <button type="button" className="tape-nav" onClick={() => shift(7)}>Седмица ›</button>
          <button type="button" className="btn-print" onClick={() => window.print()}>Печат</button>
        </div>
      </div>

      <p className="tape-hint">
        Лентата започва в деня на пристигане и свършва сутринта на напускане.
        Клик върху лента отваря резервацията, клик върху празен ден започва нова.
        Плъзни лентата, за да смениш стая или дати.
      </p>

      {error && <div className="tape-error">{error}</div>}
      {loading && !calendarData && <div className="page-loading">Зареждане...</div>}

      {calendarData && (
        <div className="tape-scroll">
          <div className="tape-head">
            <div className="tape-room-head">Стая</div>
            <div className="tape-dates" style={{ gridTemplateColumns: `repeat(${dates.length}, minmax(72px, 1fr))` }}>
              {dates.map((date) => {
                const key = format(date, 'yyyy-MM-dd')
                const weekend = date.getDay() === 0 || date.getDay() === 6
                return (
                  <div key={key} className={`tape-date ${key === today ? 'today' : ''} ${weekend ? 'weekend' : ''}`}>
                    <span>{format(date, 'EEE', { locale: bg })}</span>
                    <strong>{format(date, 'd MMM', { locale: bg })}</strong>
                    <em>{occupiedOn(date)}/{rooms.length}</em>
                  </div>
                )
              })}
            </div>
          </div>

          {rooms.map((room, roomIndex) => (
            <div className="tape-row" key={room.id}>
              <div className="tape-room">
                <strong>{room.roomNumber}</strong>
                <span>{room.roomType}</span>
              </div>
              <div className="tape-track" style={{ gridTemplateColumns: `repeat(${dates.length}, minmax(72px, 1fr))` }}>
                {dates.map((date, index) => {
                  const key = format(date, 'yyyy-MM-dd')
                  const weekend = date.getDay() === 0 || date.getDay() === 6
                  return (
                    <button
                      key={key}
                      type="button"
                      className={`tape-cell ${key === today ? 'today' : ''} ${weekend ? 'weekend' : ''} ${key < today ? 'past' : ''}`}
                      style={{ gridColumn: index + 1, gridRow: 1 }}
                      onClick={() => startBooking(room, key)}
                      title={key < today ? 'Минала дата' : 'Нова резервация'}
                    />
                  )
                })}
                {staysForRoom(room, dates, calendarData.calendar).map((stay) => {
                  const dragging = preview?.bookingId === stay.bookingId
                  return (
                    <button
                      key={stay.bookingId}
                      type="button"
                      className={`tape-bar ${barClass(stay.status)} ${dragging ? 'dragging' : ''}`}
                      style={{
                        gridColumn: `${stay.startIndex + 1} / ${stay.endIndex + 1}`,
                        gridRow: 1,
                        transform: dragging
                          ? `translate(${preview.dayDelta * preview.colWidth}px, ${preview.roomDelta * preview.rowHeight}px)`
                          : undefined
                      }}
                      title={`${stay.guestName} · ${statusLabel(stay.status)}`}
                      onPointerDown={(event) => onBarPointerDown(event, stay, roomIndex)}
                      onPointerMove={(event) => onBarPointerMove(event, stay.bookingId)}
                      onPointerUp={(event) => onBarPointerUp(event, stay)}
                    >
                      <span>{stay.guestName}</span>
                    </button>
                  )
                })}
              </div>
            </div>
          ))}
        </div>
      )}

      <div className="legend">
        <div className="legend-items">
          <div className="legend-item"><span className="legend-color reserved" /> Резервация</div>
          <div className="legend-item"><span className="legend-color in-house" /> Настанен</div>
          <div className="legend-item"><span className="legend-color free" /> Свободно</div>
          <div className="legend-item"><span className="legend-color today-mark" /> Днес</div>
        </div>
      </div>

      {selected && (
        <div className="tape-panel">
          <div className="tape-panel-card">
            <button type="button" className="tape-panel-close" onClick={() => setSelected(null)}>Затвори</button>
            <h2>{selected.guestName}</h2>
            <p>Стая {selected.roomNumber} · {selected.roomType}</p>
            <p>
              {selected.checkInDate ? format(parseISO(selected.checkInDate), 'dd.MM.yyyy') : '—'}
              {' – '}
              {selected.checkOutDate ? format(parseISO(selected.checkOutDate), 'dd.MM.yyyy') : '—'}
            </p>
            <p className={`tape-status ${barClass(selected.status)}`}>{statusLabel(selected.status)}</p>
            <div className="tape-panel-actions">
              {selected.status === 'BOOKED' && (
                <button type="button" onClick={() => runAction('check-in')} disabled={moving}>Настани</button>
              )}
              {selected.status === 'CHECKED_IN' && (
                <button type="button" onClick={() => runAction('check-out')} disabled={moving}>Напускане</button>
              )}
              {selected.status === 'BOOKED' && (
                <button type="button" className="danger" onClick={() => runAction('cancel')} disabled={moving}>Отмени</button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default RoomOccupancyCalendar
