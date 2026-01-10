import { useEffect, useState } from 'react'
import { getRevenueReport } from '../../api/reportApi'
import { format, addDays } from 'date-fns'
import './ReportPage.css'

const RevenueReport = () => {
  const [report, setReport] = useState(null)
  const [startDate, setStartDate] = useState(format(new Date(), 'yyyy-MM-dd'))
  const [endDate, setEndDate] = useState(format(addDays(new Date(), 30), 'yyyy-MM-dd'))
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    loadReport()
  }, [])

  const loadReport = async () => {
    setLoading(true)
    try {
      const response = await getRevenueReport(startDate, endDate)
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
      <h1>Отчет за приходи</h1>

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
              <h3>Общ приход</h3>
              <p className="summary-value">€{report.totalRevenue || 0}</p>
            </div>
            <div className="summary-card">
              <h3>Приход в брой</h3>
              <p className="summary-value">€{report.cashRevenue || 0}</p>
            </div>
            <div className="summary-card">
              <h3>Приход с карта</h3>
              <p className="summary-value">€{report.cardRevenue || 0}</p>
            </div>
            <div className="summary-card">
              <h3>Общо плащания</h3>
              <p className="summary-value">{report.paymentCount || 0}</p>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default RevenueReport

