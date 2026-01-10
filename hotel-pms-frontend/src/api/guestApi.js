import api from './axios'

export const getGuests = () => api.get('/guests')
export const getGuestById = (id) => api.get(`/guests/${id}`)
export const createGuest = (data) => api.post('/guests', data)
export const updateGuest = (id, data) => api.put(`/guests/${id}`, data)
export const searchGuests = (query) => 
  api.get('/guests/search', { params: { query } })

