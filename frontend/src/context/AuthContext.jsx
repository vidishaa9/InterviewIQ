import { createContext, useContext, useState, useEffect } from 'react'

/**
 * AUTH CONTEXT
 *
 * Provides authentication state to the ENTIRE app.
 * Any component can call useAuth() to get:
 *   - user: the current user object (null if not logged in)
 *   - token: the JWT token
 *   - login(data): store user + token, redirect
 *   - logout(): clear everything, redirect to login
 *   - isAuthenticated: boolean shortcut
 *
 * HOW JWT STORAGE WORKS:
 * We store the JWT in localStorage.
 * On page refresh, we read it back from localStorage.
 * This keeps users logged in across page refreshes.
 *
 * SECURITY NOTE: localStorage is vulnerable to XSS attacks.
 * For production, consider httpOnly cookies instead.
 * For a portfolio project, localStorage is acceptable.
 *
 * On app load, useEffect checks localStorage for an existing token.
 * If found → user is restored as logged in without needing to re-login.
 */
const AuthContext = createContext(null)

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null)
  const [token, setToken] = useState(null)
  const [loading, setLoading] = useState(true) // true while checking localStorage

  // On mount: restore auth state from localStorage
  useEffect(() => {
    const storedToken = localStorage.getItem('iq_token')
    const storedUser = localStorage.getItem('iq_user')
    if (storedToken && storedUser) {
      setToken(storedToken)
      setUser(JSON.parse(storedUser))
    }
    setLoading(false) // Done checking, app can render
  }, [])

  const login = (authData) => {
    // authData = { token, name, email, userId } from backend
    const userObj = {
      id: authData.userId,
      name: authData.name,
      email: authData.email,
    }
    setToken(authData.token)
    setUser(userObj)
    localStorage.setItem('iq_token', authData.token)
    localStorage.setItem('iq_user', JSON.stringify(userObj))
  }

  const logout = () => {
    setToken(null)
    setUser(null)
    localStorage.removeItem('iq_token')
    localStorage.removeItem('iq_user')
  }

  // Don't render children until we know auth state
  if (loading) {
    return (
      <div className="min-h-screen bg-gray-950 flex items-center justify-center">
        <div className="animate-spin w-8 h-8 border-2 border-indigo-500 border-t-transparent rounded-full" />
      </div>
    )
  }

  return (
    <AuthContext.Provider value={{
      user,
      token,
      login,
      logout,
      isAuthenticated: !!token
    }}>
      {children}
    </AuthContext.Provider>
  )
}

// Custom hook — any component can do: const { user, logout } = useAuth()
export const useAuth = () => {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
