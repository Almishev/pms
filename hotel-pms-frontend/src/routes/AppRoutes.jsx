import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import PrivateRoute from '../auth/PrivateRoute'
import RoleRoute from '../auth/RoleRoute'

// Pages
import LoginPage from '../pages/Login/LoginPage'
import DashboardPage from '../pages/Dashboard/DashboardPage'
import RoomListPage from '../pages/Rooms/RoomListPage'
import BookingListPage from '../pages/Bookings/BookingListPage'
import NewBookingPage from '../pages/Bookings/NewBookingPage'
import PaymentPage from '../pages/Payments/PaymentPage'
import OccupancyReport from '../pages/Reports/OccupancyReport'
import NightsReport from '../pages/Reports/NightsReport'
import RevenueReport from '../pages/Reports/RevenueReport'
import RoomOccupancyCalendar from '../pages/Reports/RoomOccupancyCalendar'
import FiscalReportsPage from '../pages/Reports/FiscalReportsPage'

// Layout
import Layout from '../components/layout/Layout'

const AppRoutes = () => {
  const { isAuthenticated } = useAuth()

  return (
    <Routes>
      <Route path="/login" element={
        isAuthenticated ? <Navigate to="/dashboard" /> : <LoginPage />
      } />
      
      <Route path="/" element={
        <PrivateRoute>
          <Layout />
        </PrivateRoute>
      }>
        <Route index element={<Navigate to="/dashboard" />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="rooms" element={<RoomListPage />} />
        <Route path="bookings" element={<BookingListPage />} />
        <Route path="bookings/new" element={<NewBookingPage />} />
        <Route path="payments" element={<PaymentPage />} />
        <Route path="reports/fiscal" element={<FiscalReportsPage />} />
        <Route path="reports/occupancy" element={<OccupancyReport />} />
        <Route path="reports/nights" element={<NightsReport />} />
        <Route path="reports/revenue" element={<RevenueReport />} />
        <Route path="reports/room-calendar" element={<RoomOccupancyCalendar />} />
      </Route>
    </Routes>
  )
}

export default AppRoutes

