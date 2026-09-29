import api from './axios'

export const getRooms = (checkInDate, checkOutDate, includeInactive = false) => {
  const params = { includeInactive }
  if (checkInDate) params.checkInDate = checkInDate
  if (checkOutDate) params.checkOutDate = checkOutDate
  return api.get('/rooms', { params })
}
export const getRoomById = (id) => api.get(`/rooms/${id}`)
export const createRoom = (roomNumber, roomTypeId) => 
  api.post('/rooms', null, { params: { roomNumber, roomTypeId } })
export const updateRoom = (id, data) => 
  api.put(`/rooms/${id}`, null, { params: data })
export const deleteRoom = (id) => api.delete(`/rooms/${id}`)
export const updateRoomType = (id, data) => 
  api.put(`/rooms/types/${id}`, null, { params: data })
export const deleteRoomType = (id) => api.delete(`/rooms/types/${id}`)
export const getRoomTypes = () => api.get('/rooms/types')
export const createRoomType = (name, capacity, basePrice) => 
  api.post('/rooms/types', null, { params: { name, capacity, basePrice } })

