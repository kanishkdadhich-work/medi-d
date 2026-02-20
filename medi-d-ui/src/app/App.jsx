import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom"
import { useEffect } from "react"
import { useAuthStore } from "@/lib/auth-store"
import { getPrimaryRole } from "@/lib/roles"
import LoginPage from "./pages/LoginPage"
import DoctorDashboard from "./pages/DoctorDashboard"
import ReceptionistDashboard from "./pages/ReceptionistDashboard"
import PharmacistDashboard from "./pages/PharmacistDashboard"
import AppointmentPage from "./pages/AppointmentPage"
import PrescriptionPage from "./pages/PrescriptionPage"

function App() {
  const { user, loadAuth } = useAuthStore()

  useEffect(() => {
    loadAuth()
  }, [])

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={user ? <Navigate to="/" /> : <LoginPage />} />
        
        {user ? (
          <>
            <Route path="/" element={<DashboardRouter />} />
            <Route path="/appointments" element={<AppointmentPage />} />
            <Route path="/prescriptions" element={<PrescriptionPage />} />
          </>
        ) : (
          <Route path="*" element={<Navigate to="/login" />} />
        )}
      </Routes>
    </BrowserRouter>
  )
}

function DashboardRouter() {
  const { user } = useAuthStore()

  if (!user) {
    return <Navigate to="/login" />
  }

  const role = getPrimaryRole(user)

  if (role === 'doctor') {
    return <DoctorDashboard />
  }
  if (role === 'receptionist') {
    return <ReceptionistDashboard />
  }
  if (role === 'pharmacist') {
    return <PharmacistDashboard />
  }

  return <Navigate to="/login" />
}

export default App
