import api from './axios'

export const getRevenueReport = (startDate, endDate) => 
  api.get('/reports/revenue', { params: { startDate, endDate } })
export const getOccupancyReport = (startDate, endDate) => 
  api.get('/reports/occupancy', { params: { startDate, endDate } })
export const getNightsReport = (startDate, endDate) => 
  api.get('/reports/nights', { params: { startDate, endDate } })
export const getRoomOccupancyCalendar = (startDate, endDate) => 
  api.get('/reports/room-occupancy-calendar', { params: { startDate, endDate } })
export const getNsiReport = (month) =>
  api.get('/reports/nsi', { params: { month } })

