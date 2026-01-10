import api from './axios'

export const getRooms = (checkInDate, checkOutDate) => {
  const params = {}
  if (checkInDate) params.checkInDate = checkInDate
  if (checkOutDate) params.checkOutDate = checkOutDate
  return api.get('/rooms', { params })
}
export const getRoomById = (id) => api.get(`/rooms/${id}`)
export const createRoom = (roomNumber, roomTypeId) => 
  api.post('/rooms', null, { params: { roomNumber, roomTypeId } })
export const updateRoom = (id, data) => 
  api.put(`/rooms/${id}`, null, { params: data })
export const getRoomTypes = () => api.get('/rooms/types')
export const createRoomType = (name, capacity, basePrice) => 
  api.post('/rooms/types', null, { params: { name, capacity, basePrice } })

