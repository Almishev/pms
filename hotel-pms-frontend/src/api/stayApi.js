import api from './axios'

export const getStayNightsByBooking = (bookingId) => 
  api.get(`/stays/booking/${bookingId}`)
export const getStayNightsByDateRange = (startDate, endDate) => 
  api.get('/stays/date-range', { params: { startDate, endDate } })
export const getOccupancy = (date) => api.get(`/stays/occupancy/${date}`)
export const updateStayNightsPrice = (bookingId, pricePerNight) =>
  api.put(`/stays/booking/${bookingId}/price`, null, { params: { pricePerNight } })

