import { useEffect, useState } from 'react'
import { getRevenueReport } from '../../api/reportApi'
import { format, addDays, parseISO } from 'date-fns'
import { bg } from 'date-fns/locale'
import { downloadCsv } from '../../utils/csv'
import './ReportPage.css'

const methodLabel = (method) => {
  if (method === 'CASH') return 'В брой'
  if (method === 'CARD') return 'С карта'
  return method
}

const RevenueReport = () => {
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
      const response = await getRevenueReport(startDate, endDate)
      setReport(response.data)
    } catch (err) {
      setError('Отчетът за плащания не се зареди')
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
    const rows = (report.payments || []).map((row) => [
      format(parseISO(row.paymentDate), 'dd.MM.yyyy HH:mm'),
      row.guestName,
      row.roomNumber,
      methodLabel(row.paymentMethod),
      row.amount
    ])
    downloadCsv(`plashtaniya-${startDate}-${endDate}.csv`, ['Дата', 'Гост', 'Стая', 'Метод', 'Сума'], rows)
  }

  return (
    <div className="report-page">
      <h1>Плащания</h1>
      <p className="report-note">Парите, влезли в касата за периода. Сторнираните плащания не се броят.</p>

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
              <h3>Общо</h3>
              <p className="summary-value">€{Number(report.totalRevenue || 0).toFixed(2)}</p>
            </div>
            <div className="summary-card">
              <h3>В брой</h3>
              <p className="summary-value">€{Number(report.cashRevenue || 0).toFixed(2)}</p>
            </div>
            <div className="summary-card">
              <h3>С карта</h3>
              <p className="summary-value">€{Number(report.cardRevenue || 0).toFixed(2)}</p>
            </div>
            <div className="summary-card">
              <h3>Брой плащания</h3>
              <p className="summary-value">{report.paymentCount || 0}</p>
            </div>
          </div>

          <div className="occupancy-grid">
            <h2>Редове</h2>
            {(report.payments || []).length === 0 ? (
              <p>Няма плащания за периода</p>
            ) : (
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Дата</th>
                    <th>Гост</th>
                    <th>Стая</th>
                    <th>Метод</th>
                    <th>Сума</th>
                  </tr>
                </thead>
                <tbody>
                  {report.payments.map((row, index) => (
                    <tr key={`${row.paymentDate}-${index}`}>
                      <td>{format(parseISO(row.paymentDate), 'dd MMM yyyy HH:mm', { locale: bg })}</td>
                      <td>{row.guestName}</td>
                      <td>{row.roomNumber}</td>
                      <td>{methodLabel(row.paymentMethod)}</td>
                      <td>€{Number(row.amount).toFixed(2)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

export default RevenueReport
