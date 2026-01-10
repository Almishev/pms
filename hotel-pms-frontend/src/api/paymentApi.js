import api from './axios'

export const processPayment = (data) => api.post('/payments', data)
export const getPaymentsByBooking = (bookingId) => 
  api.get(`/payments/booking/${bookingId}`)
export const stornoPayment = (id) => api.post(`/payments/${id}/storno`)
export const printZReport = () => api.post('/payments/fiscal/z-report')
export const printXReport = () => api.post('/payments/fiscal/x-report')
export const getFiscalReportsHistory = () => api.get('/payments/fiscal/reports')
export const getFiscalReportsByType = (type) => api.get(`/payments/fiscal/reports/${type}`)
export const generateReportPreview = (type) => api.get(`/payments/fiscal/reports/preview/${type}`)
export const getReportDetails = (id) => api.get(`/payments/fiscal/reports/${id}/details`)

