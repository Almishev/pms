import api from './axios'

export const getBookings = () => api.get('/bookings')
export const getBookingById = (id) => api.get(`/bookings/${id}`)
export const createBooking = (data) => api.post('/bookings', data)
export const checkIn = (id) => api.post(`/bookings/${id}/check-in`)
export const checkOut = (id) => api.post(`/bookings/${id}/check-out`)
export const cancelBooking = (id) => api.post(`/bookings/${id}/cancel`)
export const getOpenFolios = () => api.get('/bookings/open-folios')
export const getRestaurantCharges = (id) => api.get(`/bookings/${id}/restaurant-charges`)
export const addRestaurantCharge = (id, data) => api.post(`/bookings/${id}/restaurant-charges`, data)
export const updateRestaurantCharge = (id, restaurantCharge) =>
  api.put(`/bookings/${id}/restaurant-charge`, null, { params: { restaurantCharge } })
export const moveBooking = (id, data) => api.put(`/bookings/${id}/stay`, data)
export const getBookingsByDateRange = (startDate, endDate) => 
  api.get('/bookings/date-range', { params: { startDate, endDate } })

