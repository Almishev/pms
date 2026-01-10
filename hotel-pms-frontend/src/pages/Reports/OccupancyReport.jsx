import { useEffect, useState } from 'react'
import { getOccupancyReport } from '../../api/reportApi'
import { format, addDays, startOfDay } from 'date-fns'
import './ReportPage.css'

const OccupancyReport = () => {
  const [report, setReport] = useState(null)
  const [startDate, setStartDate] = useState(format(new Date(), 'yyyy-MM-dd'))
  const [endDate, setEndDate] = useState(format(addDays(new Date(), 14), 'yyyy-MM-dd'))
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    loadReport()
  }, [])

  const loadReport = async () => {
    setLoading(true)
    try {
      const response = await getOccupancyReport(startDate, endDate)
      setReport(response.data)
    } catch (error) {
      console.error('Error loading report:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    loadReport()
  }

  if (loading) {
    return <div className="page-loading">Зареждане...</div>
  }

  return (
    <div className="report-page">
      <h1>Отчет за заетост</h1>

      <form onSubmit={handleSubmit} className="report-filters">
        <div className="form-group">
          <label>Начална дата</label>
          <input
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
          />
        </div>
        <div className="form-group">
          <label>Крайна дата</label>
          <input
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
          />
        </div>
        <button type="submit" className="btn-primary">Генерирай отчет</button>
      </form>

      {report && (
        <div className="report-content">
          <div className="report-summary">
            <div className="summary-card">
              <h3>Общо нощувки</h3>
              <p className="summary-value">{report.totalNights}</p>
            </div>
            <div className="summary-card">
              <h3>Средна заетост</h3>
              <p className="summary-value">{report.averageOccupancy?.toFixed(1) || 0}</p>
            </div>
          </div>

          <div className="occupancy-grid">
            <h2>Заетост по дати</h2>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Дата</th>
                  <th>Заетост</th>
                </tr>
              </thead>
              <tbody>
                {Object.entries(report.occupancyByDate || {}).map(([date, count]) => (
                  <tr key={date}>
                    <td>{format(new Date(date), 'MMM dd, yyyy')}</td>
                    <td>{count}</td>
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

