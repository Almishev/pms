import { useEffect, useState } from 'react'
import { format } from 'date-fns'
import { getNsiReport } from '../../api/reportApi'
import { downloadCsv } from '../../utils/csv'
import './ReportPage.css'

const NsiReport = () => {
  const [month, setMonth] = useState(format(new Date(), 'yyyy-MM'))
  const [report, setReport] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    loadReport(month)
  }, [])

  const loadReport = async (value) => {
    setLoading(true)
    setError('')
    try {
      const response = await getNsiReport(value)
      setReport(response.data)
    } catch (err) {
      setError('Справката за НСИ не се зареди')
    } finally {
      setLoading(false)
    }
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    loadReport(month)
  }

  const exportReport = () => {
    if (!report) return
    const rows = [
      ['Пристигания', report.arrivals, report.foreignArrivals],
      ['Нощувки', report.nights, report.foreignNights],
      ['Приход от нощувки', report.roomRevenue, report.foreignRoomRevenue],
      [],
      ['Държава', 'Пристигания', 'Нощувки', 'Приход от нощувки']
    ]
    ;(report.byCountry || []).forEach((row) => {
      rows.push([row.country, row.arrivals, row.nights, row.roomRevenue])
    })
    downloadCsv(`nsi-${month}.csv`, ['Показател', 'Общо', 'в т.ч. чужденци'], rows)
  }

  return (
    <div className="report-page">
      <h1>Месечна справка за НСИ</h1>
      <p className="report-note">Гости без записана държава се броят като българи. Приходът е от нощувките, без ресторант.</p>

      <form onSubmit={handleSubmit} className="report-filters">
        <div className="form-group">
          <label>Месец</label>
          <input type="month" value={month} onChange={(e) => setMonth(e.target.value)} />
        </div>
        <button type="submit" className="btn-primary">Генерирай справка</button>
        {report && <button type="button" className="btn-export" onClick={exportReport}>Експорт към Excel</button>}
      </form>

      {error && <div className="tape-error">{error}</div>}
      {loading && <div className="page-loading">Зареждане...</div>}

      {report && !loading && (
        <div className="report-content">
          <table className="data-table">
            <thead>
              <tr>
                <th>Показател</th>
                <th>Общо</th>
                <th>в т.ч. чужденци</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>Пристигания</td>
                <td>{report.arrivals}</td>
                <td>{report.foreignArrivals}</td>
              </tr>
              <tr>
                <td>Нощувки</td>
                <td>{report.nights}</td>
                <td>{report.foreignNights}</td>
              </tr>
              <tr>
                <td>Приход от нощувки</td>
                <td>€{Number(report.roomRevenue || 0).toFixed(2)}</td>
                <td>€{Number(report.foreignRoomRevenue || 0).toFixed(2)}</td>
              </tr>
            </tbody>
          </table>

          <div className="occupancy-grid">
            <h2>Чужденци по държава</h2>
            {(report.byCountry || []).length === 0 ? (
              <p>Няма чуждестранни гости за месеца</p>
            ) : (
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Държава</th>
                    <th>Пристигания</th>
                    <th>Нощувки</th>
                    <th>Приход от нощувки</th>
                  </tr>
                </thead>
                <tbody>
                  {report.byCountry.map((row) => (
                    <tr key={row.country}>
                      <td>{row.country}</td>
                      <td>{row.arrivals}</td>
                      <td>{row.nights}</td>
                      <td>€{Number(row.roomRevenue).toFixed(2)}</td>
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

export default NsiReport
