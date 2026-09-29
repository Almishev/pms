import { useEffect, useState } from 'react'
import { getNightsReport } from '../../api/reportApi'
import { format, addDays } from 'date-fns'
import { downloadCsv } from '../../utils/csv'
import './ReportPage.css'

const NightsReport = () => {
  const [report, setReport] = useState(null)
  const [startDate, setStartDate] = useState(format(new Date(), 'yyyy-MM-dd'))
  const [endDate, setEndDate] = useState(format(addDays(new Date(), 30), 'yyyy-MM-dd'))
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    loadReport()
  }, [])

  const loadReport = async () => {
    setLoading(true)
    setError('')
    try {
      const response = await getNightsReport(startDate, endDate)
      setReport(response.data)
    } catch (err) {
      setError('Отчетът за начисления не се зареди')
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
    const rows = (report.byRoomType || []).map((row) => [
      row.roomType,
      row.nights,
      row.revenue,
      row.averagePrice
    ])
    downloadCsv(`nachisleniya-${startDate}-${endDate}.csv`, ['Тип стая', 'Нощувки', 'Начисление', 'Средна цена'], rows)
  }

  return (
    <div className="report-page">
      <h1>Начисления</h1>
      <p className="report-note">Сумата е цената на нощувките към датата на нощувката. Ресторантът и касата са в Плащания.</p>

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
              <h3>Начисления от нощувки</h3>
              <p className="summary-value">€{Number(report.totalRevenue || 0).toFixed(2)}</p>
            </div>
            <div className="summary-card">
              <h3>Средна цена/нощ</h3>
              <p className="summary-value">€{Number(report.averagePricePerNight || 0).toFixed(2)}</p>
            </div>
          </div>

          <div className="occupancy-grid">
            <h2>По тип стая</h2>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Тип</th>
                  <th>Нощувки</th>
                  <th>Начисление</th>
                  <th>Средна цена</th>
                </tr>
              </thead>
              <tbody>
                {(report.byRoomType || []).map((row) => (
                  <tr key={row.roomType}>
                    <td>{row.roomType}</td>
                    <td>{row.nights}</td>
                    <td>€{Number(row.revenue).toFixed(2)}</td>
                    <td>€{Number(row.averagePrice).toFixed(2)}</td>
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

export default NightsReport
