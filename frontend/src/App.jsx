import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import ProtectedRoute from './components/ProtectedRoute'
import Navbar from './components/Navbar'
import Login from './pages/Login'
import Register from './pages/Register'
import Dashboard from './pages/Dashboard'
import Resumes from './pages/Resumes'
import InterviewSetup from './pages/InterviewSetup'
import InterviewSession from './pages/InterviewSession'
import InterviewResults from './pages/InterviewResults'
import History from './pages/History'

/**
 * APP.JSX — Route definitions
 *
 * Route structure:
 * /login, /register  → Public (no auth needed)
 * Everything else    → Wrapped in ProtectedRoute (redirects to /login if not authenticated)
 *
 * AuthProvider wraps everything so all routes can access auth state.
 * BrowserRouter enables React Router v6 navigation.
 * Navbar only shows on protected routes (inside the ProtectedRoute wrapper).
 */
export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          {/* Public routes */}
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/" element={<Navigate to="/dashboard" replace />} />

          {/* Protected routes — all need a valid JWT */}
          <Route element={<ProtectedRoute />}>
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/resumes" element={<Resumes />} />
            <Route path="/interview/setup" element={<InterviewSetup />} />
            <Route path="/interview/session/:sessionId" element={<InterviewSession />} />
            <Route path="/interview/results/:sessionId" element={<InterviewResults />} />
            <Route path="/history" element={<History />} />
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
