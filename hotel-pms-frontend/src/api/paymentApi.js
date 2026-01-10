import api from './axios'

export const processPayment = (data) => api.post('/payments', data)
export const getPaymentsByBooking = (bookingId) => 
  api.get(`/payments/booking/${bookingId}`)
export const stornoPayment = (id) => api.post(`/payments/${id}/storno`)

