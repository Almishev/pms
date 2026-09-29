import { useEffect, useState } from 'react'
import { getOccupancyReport } from '../../api/reportApi'
import { format, addDays, parseISO } from 'date-fns'
import { bg } from 'date-fns/locale'
import { downloadCsv } from '../../utils/csv'
import './ReportPage.css'

const OccupancyReport = () => {
  const [report, setReport] = useState(null)
  const [startDate, setStartDate] = useState(format(new Date(), 'yyyy-MM-dd'))
  const [endDate, setEndDate] = useState(format(addDays(new Date(), 14), 'yyyy-MM-dd'))
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    loadReport()
  }, [])

  const loadReport = async () => {
    setLoading(true)
    setError('')
    try {
      const response = await getOccupancyReport(startDate, endDate)
      setReport(response.data)
    } catch (err) {
      setError('Отчетът за заетост не се зареди')
    } finally {
      setLoading(false)
    }
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    loadReport()
  }

  const exportReport = () => {
    if (!report) return
    const rows = (report.byDate || []).map((row) => [
      format(parseISO(row.date), 'dd.MM.yyyy'),
      row.occupied,
      row.capacity,
      row.percent
    ])
    rows.push([])
    rows.push(['Тип стая', 'Стаи', 'Нощувки', 'Заетост %'])
    ;(report.byRoomType || []).forEach((row) => {
      rows.push([row.roomType, row.rooms, row.nights, row.percent])
    })
    downloadCsv(`zaetost-${startDate}-${endDate}.csv`, ['Дата', 'Заети', 'Капацитет', 'Заетост %'], rows)
  }

  return (
    <div className="report-page">
      <h1>Отчет за заетост</h1>

      <form onSubmit={handleSubmit} className="report-filters">
        <div className="form-group">
          <label>Начална дата</label>
          <input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
        </div>
        <div className="form-group">
          <label>Крайна дата</label>
          <input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
        </div>
        <button type="submit" className="btn-primary">Генерирай отчет</button>
        {report && <button type="button" className="btn-export" onClick={exportReport}>Експорт към Excel</button>}
      </form>

      {error && <div className="tape-error">{error}</div>}
      {loading && <div className="page-loading">Зареждане...</div>}

      {report && !loading && (
        <div className="report-content">
          <div className="report-summary">
            <div className="summary-card">
              <h3>Общо нощувки</h3>
              <p className="summary-value">{report.totalNights}</p>
            </div>
            <div className="summary-card">
              <h3>Средна заетост</h3>
              <p className="summary-value">{Number(report.occupancyPercent || 0).toFixed(1)}%</p>
              <small>от {report.capacity} стаи</small>
            </div>
          </div>

          <div className="occupancy-grid">
            <h2>По тип стая</h2>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Тип</th>
                  <th>Стаи</th>
                  <th>Нощувки</th>
                  <th>Заетост</th>
                </tr>
              </thead>
              <tbody>
                {(report.byRoomType || []).map((row) => (
                  <tr key={row.roomType}>
                    <td>{row.roomType}</td>
                    <td>{row.rooms}</td>
                    <td>{row.nights}</td>
                    <td>{Number(row.percent).toFixed(1)}%</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="occupancy-grid">
            <h2>По дати</h2>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Дата</th>
                  <th>Заети</th>
                  <th>Капацитет</th>
                  <th>Заетост</th>
                </tr>
              </thead>
              <tbody>
                {(report.byDate || []).map((row) => (
                  <tr key={row.date}>
                    <td>{format(parseISO(row.date), 'dd MMM yyyy', { locale: bg })}</td>
                    <td>{row.occupied}</td>
                    <td>{row.capacity}</td>
                    <td>{Number(row.percent).toFixed(1)}%</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}

export default OccupancyReport
