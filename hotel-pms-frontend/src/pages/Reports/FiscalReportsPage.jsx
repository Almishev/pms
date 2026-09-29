import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { printZReport, printXReport, getFiscalReportsHistory, generateReportPreview, getReportDetails } from '../../api/paymentApi'
import { useAuth } from '../../auth/AuthContext'
import { format } from 'date-fns'
import './FiscalReportsPage.css'

const FiscalReportsPage = () => {
  const { isAdmin } = useAuth()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [lastZReport, setLastZReport] = useState(null)
  const [lastXReport, setLastXReport] = useState(null)
  const [reportsHistory, setReportsHistory] = useState([])
  const [historyLoading, setHistoryLoading] = useState(false)
  const [filterType, setFilterType] = useState('ALL')
  const [showPreviewModal, setShowPreviewModal] = useState(false)
  const [previewData, setPreviewData] = useState(null)
  const [previewLoading, setPreviewLoading] = useState(false)
  const [previewType, setPreviewType] = useState(null) // 'Z_REPORT' or 'X_REPORT' or reportId

  useEffect(() => {
    if (isAdmin) {
      loadReportsHistory()
    }
  }, [isAdmin])

  const loadReportsHistory = async () => {
    setHistoryLoading(true)
    try {
      const response = await getFiscalReportsHistory()
      setReportsHistory(response.data)
    } catch (err) {
      console.error('Грешка при зареждане на история на отчети:', err)
    } finally {
      setHistoryLoading(false)
    }
  }

  // Check if user is admin
  if (!isAdmin) {
    return (
      <div className="fiscal-reports-page">
        <h1>Фискални отчети</h1>
        <div className="access-denied">
          <h2>Нямате достъп до тази страница</h2>
          <p>Само администраторите могат да принтират фискални отчети.</p>
          <button onClick={() => navigate('/dashboard')} className="btn-primary">
            Назад към таблото
          </button>
        </div>
      </div>
    )
  }

  const handleZReport = async () => {
    if (!window.confirm('Сигурни ли сте, че искате да принтирате Z-отчет?\n\nZ-отчетът затваря фискалния ден. Следващият отчет започва от този момент. Касата в системата продължава да приема плащания.')) {
      return
    }

    setLoading(true)
    setError('')
    setSuccess('')

    try {
      const response = await printZReport()
      setLastZReport({
        receiptNumber: response.data.receiptNumber,
        fiscalDate: response.data.fiscalDate
      })
      setSuccess('Z-отчетът е принтиран успешно!')
      await loadReportsHistory() // Refresh history
    } catch (err) {
      setError(err.response?.data?.error || 'Неуспешно принтиране на Z-отчет')
    } finally {
      setLoading(false)
    }
  }

  const handleXReport = async () => {
    setLoading(true)
    setError('')
    setSuccess('')

    try {
      const response = await printXReport()
      setLastXReport({
        receiptNumber: response.data.receiptNumber,
        fiscalDate: response.data.fiscalDate
      })
      setSuccess('X-отчетът е принтиран успешно!')
      await loadReportsHistory() // Refresh history
    } catch (err) {
      setError(err.response?.data?.error || 'Неуспешно принтиране на X-отчет')
    } finally {
      setLoading(false)
    }
  }

  const filteredReports = reportsHistory.filter(report => {
    if (filterType === 'ALL') return true
    return report.reportType === filterType
  })

  const handlePreview = async (type) => {
    setPreviewLoading(true)
    setPreviewType(type)
    setError('')
    
    try {
      const response = await generateReportPreview(type)
      setPreviewData(response.data)
      setShowPreviewModal(true)
    } catch (err) {
      setError(err.response?.data?.error || 'Неуспешно зареждане на преглед')
    } finally {
      setPreviewLoading(false)
    }
  }

  const handleViewReport = async (reportId) => {
    setPreviewLoading(true)
    setPreviewType(reportId)
    setError('')
    
    try {
      const response = await getReportDetails(reportId)
      setPreviewData(response.data)
      setShowPreviewModal(true)
    } catch (err) {
      setError(err.response?.data?.error || 'Неуспешно зареждане на детайли')
    } finally {
      setPreviewLoading(false)
    }
  }

  return (
    <div className="fiscal-reports-page">
      <h1>Фискални отчети</h1>

      <div className="fiscal-info">
        <div className="info-card warning">
          <h3>⚠️ Важна информация</h3>
          <ul>
            <li><strong>Z-отчет</strong> е ЗАДЪЛЖИТЕЛЕН всеки ден в края на работния ден</li>
            <li>Z-отчетът затваря фискалния ден. Следващият отчет започва от този момент</li>
            <li>Касата в системата продължава да приема плащания и след Z-отчета</li>
            <li><strong>X-отчет</strong> е опционален и може да се принтира по всяко време</li>
            <li>X-отчетът НЕ затваря дневния период</li>
          </ul>
        </div>
      </div>

      {error && <div className="error-message">{error}</div>}
      {success && <div className="success-message">{success}</div>}

      <div className="fiscal-reports-grid">
        <div className="report-card z-report">
          <div className="report-header">
            <h2>Z-отчет (Дневно затваряне)</h2>
            <span className="report-badge required">Задължителен</span>
          </div>
          <div className="report-content">
            <p className="report-description">
              Z-отчетът е задължителен всеки ден в края на работния ден. 
              Той затваря дневния период на фискалния принтер и е необходим за данъчни цели.
            </p>
            {lastZReport && (
              <div className="last-report-info">
                <p><strong>Последен Z-отчет:</strong></p>
                <p>Номер: {lastZReport.receiptNumber}</p>
                <p>Дата: {format(new Date(lastZReport.fiscalDate), 'dd MMM, yyyy HH:mm')}</p>
              </div>
            )}
            <div className="report-actions">
              <button
                onClick={() => handlePreview('Z_REPORT')}
                disabled={previewLoading}
                className="btn-report btn-preview"
              >
                {previewLoading && previewType === 'Z_REPORT' ? 'Зареждане...' : '👁️ Преглед'}
              </button>
              <button
                onClick={handleZReport}
                disabled={loading}
                className="btn-report btn-z-report"
              >
                {loading ? 'Принтиране...' : 'Принтирай Z-отчет'}
              </button>
            </div>
          </div>
        </div>

        <div className="report-card x-report">
          <div className="report-header">
            <h2>X-отчет (Междинен отчет)</h2>
            <span className="report-badge optional">Опционален</span>
          </div>
          <div className="report-content">
            <p className="report-description">
              X-отчетът е междинен отчет, който може да се принтира по всяко време 
              за проверка на текущите данни. НЕ затваря дневния период.
            </p>
            {lastXReport && (
              <div className="last-report-info">
                <p><strong>Последен X-отчет:</strong></p>
                <p>Номер: {lastXReport.receiptNumber}</p>
                <p>Дата: {format(new Date(lastXReport.fiscalDate), 'dd MMM, yyyy HH:mm')}</p>
              </div>
            )}
            <div className="report-actions">
              <button
                onClick={() => handlePreview('X_REPORT')}
                disabled={previewLoading}
                className="btn-report btn-preview"
              >
                {previewLoading && previewType === 'X_REPORT' ? 'Зареждане...' : '👁️ Преглед'}
              </button>
              <button
                onClick={handleXReport}
                disabled={loading}
                className="btn-report btn-x-report"
              >
                {loading ? 'Принтиране...' : 'Принтирай X-отчет'}
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Reports History Section */}
      <div className="reports-history-section">
        <div className="history-header">
          <h2>История на принтираните отчети</h2>
          <div className="history-filters">
            <select
              value={filterType}
              onChange={(e) => setFilterType(e.target.value)}
              className="filter-select"
            >
              <option value="ALL">Всички отчети</option>
              <option value="Z_REPORT">Само Z-отчети</option>
              <option value="X_REPORT">Само X-отчети</option>
            </select>
            <button onClick={loadReportsHistory} className="btn-refresh" title="Обнови">
              🔄
            </button>
          </div>
        </div>

        {historyLoading ? (
          <div className="loading-message">Зареждане на история...</div>
        ) : filteredReports.length === 0 ? (
          <div className="no-reports-message">
            <p>Няма принтирани отчети</p>
          </div>
        ) : (
          <div className="history-table-wrapper">
            <table className="history-table">
              <thead>
                <tr>
                  <th>Тип</th>
                  <th>Номер на отчет</th>
                  <th>Дата и час</th>
                  <th>Принтиран от</th>
                  <th>Статус</th>
                  <th>Действия</th>
                </tr>
              </thead>
              <tbody>
                {filteredReports.map(report => (
                  <tr key={report.id}>
                    <td>
                      <span className={`report-type-badge ${report.reportType === 'Z_REPORT' ? 'z-report' : 'x-report'}`}>
                        {report.reportType === 'Z_REPORT' ? 'Z-отчет' : 'X-отчет'}
                      </span>
                    </td>
                    <td>{report.reportNumber || '-'}</td>
                    <td>{format(new Date(report.fiscalDate), 'dd MMM, yyyy HH:mm')}</td>
                    <td>{report.user?.username || '-'}</td>
                    <td>
                      <div className="status-cell">
                        <span className={`status-badge ${report.success ? 'success' : 'error'}`}>
                          {report.success ? '✓ Успешен' : '✗ Грешка'}
                        </span>
                        {report.errorMessage && (
                          <div className="error-details" title={report.errorMessage}>
                            {report.errorMessage}
                          </div>
                        )}
                      </div>
                    </td>
                    <td>
                      <button
                        onClick={() => handleViewReport(report.id)}
                        className="btn-view-report"
                        title="Преглед на отчета"
                      >
                        👁️ Преглед
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Preview Modal */}
      {showPreviewModal && previewData && (
        <div className="modal-overlay" onClick={() => {
          setShowPreviewModal(false)
          setPreviewData(null)
        }}>
          <div className="modal-content report-preview-modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>
                {previewData.reportType === 'Z_REPORT' ? 'Z-отчет' : 'X-отчет'} - Преглед
                {previewData.reportNumber && <span className="report-number">#{previewData.reportNumber}</span>}
              </h2>
              <button
                onClick={() => {
                  setShowPreviewModal(false)
                  setPreviewData(null)
                }}
                className="btn-close"
              >
                ✕
              </button>
            </div>

            <div className="report-preview-content">
              <div className="report-summary-section">
                <h3>Обобщение</h3>
                <div className="summary-grid">
                  <div className="summary-item">
                    <label>Дата на отчета:</label>
                    <span>{format(new Date(previewData.reportDate), 'dd MMM, yyyy')}</span>
                  </div>
                  <div className="summary-item">
                    <label>Дата и час на принтиране:</label>
                    <span>{format(new Date(previewData.fiscalDate), 'dd MMM, yyyy HH:mm')}</span>
                  </div>
                  <div className="summary-item">
                    <label>Обща сума:</label>
                    <span className="amount">€{parseFloat(previewData.totalAmount || 0).toFixed(2)}</span>
                  </div>
                  <div className="summary-item">
                    <label>В брой:</label>
                    <span>€{parseFloat(previewData.cashAmount || 0).toFixed(2)} ({previewData.cashPayments || 0} плащания)</span>
                  </div>
                  <div className="summary-item">
                    <label>С карта:</label>
                    <span>€{parseFloat(previewData.cardAmount || 0).toFixed(2)} ({previewData.cardPayments || 0} плащания)</span>
                  </div>
                  <div className="summary-item">
                    <label>Общо плащания:</label>
                    <span>{previewData.totalPayments || 0}</span>
                  </div>
                </div>
              </div>

              {previewData.payments && previewData.payments.length > 0 && (
                <div className="report-payments-section">
                  <h3>Плащания в отчета ({previewData.payments.length})</h3>
                  <div className="payments-table-wrapper">
                    <table className="payments-table">
                      <thead>
                        <tr>
                          <th>ID</th>
                          <th>Гост</th>
                          <th>Стая</th>
                          <th>Сума</th>
                          <th>Метод</th>
                          <th>Дата</th>
                        </tr>
                      </thead>
                      <tbody>
                        {previewData.payments.map((payment, index) => (
                          <tr key={payment.paymentId || index}>
                            <td>#{payment.paymentId}</td>
                            <td>{payment.guestName}</td>
                            <td>{payment.roomNumber}</td>
                            <td>€{parseFloat(payment.amount || 0).toFixed(2)}</td>
                            <td>{payment.paymentMethod === 'CASH' ? 'В брой' : 'С карта'}</td>
                            <td>{format(new Date(payment.paymentDate), 'dd MMM, yyyy HH:mm')}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}

              {(!previewData.payments || previewData.payments.length === 0) && (
                <div className="no-payments-message">
                  <p>Няма плащания в този период</p>
                </div>
              )}
            </div>

            <div className="modal-actions">
              <button
                onClick={() => {
                  setShowPreviewModal(false)
                  setPreviewData(null)
                }}
                className="btn-secondary"
              >
                Затвори
              </button>
              {previewData.reportType && !previewData.reportNumber && (
                <button
                  onClick={() => {
                    setShowPreviewModal(false)
                    if (previewData.reportType === 'Z_REPORT') {
                      handleZReport()
                    } else {
                      handleXReport()
                    }
                  }}
                  className="btn-primary"
                >
                  Принтирай отчета
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default FiscalReportsPage

