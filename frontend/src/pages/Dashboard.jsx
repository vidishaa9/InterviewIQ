import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import api from '../api/axios'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts'
import { Trophy, Target, TrendingUp, PlayCircle, Loader2 } from 'lucide-react'

/**
 * DASHBOARD PAGE
 *
 * Shows the user's stats at a glance:
 * → Total sessions, avg score, best score (from /api/analytics/me)
 * → Recent interview history (from /api/interviews/history)
 * → Score trend bar chart (Recharts)
 * → Quick action to start a new interview
 *
 * Uses Promise.all() to fetch both data sources in PARALLEL,
 * not sequentially. This halves the loading time.
 */
export default function Dashboard() {
  const { user } = useAuth()
  const [analytics, setAnalytics] = useState(null)
  const [history, setHistory] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetchData = async () => {
      try {
        // Parallel fetch — both requests fire at the same time
        const [analyticsRes, historyRes] = await Promise.all([
          api.get('/analytics/me'),
          api.get('/interviews/history'),
        ])
        setAnalytics(analyticsRes.data)
        setHistory(historyRes.data.slice(0, 7)) // Last 7 for chart
      } catch (err) {
        console.error('Dashboard load error:', err)
      } finally {
        setLoading(false)
      }
    }
    fetchData()
  }, [])

  // Prepare chart data from history
  const chartData = history
    .filter(s => s.status === 'COMPLETED' && s.overallScore != null)
    .map((s, i) => ({
      name: `Session ${i + 1}`,
      score: parseFloat(s.overallScore),
    }))
    .reverse()

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="w-8 h-8 animate-spin text-indigo-500" />
      </div>
    )
  }

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">
            Welcome back, {user?.name?.split(' ')[0]} 👋
          </h1>
          <p className="text-gray-400 mt-1">Here's your interview performance overview</p>
        </div>
        <Link to="/interview/setup" className="btn-primary flex items-center gap-2">
          <PlayCircle className="w-4 h-4" />
          New Interview
        </Link>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <StatCard
          icon={<Target className="w-5 h-5 text-indigo-400" />}
          label="Total Sessions"
          value={analytics?.totalSessions ?? 0}
          sub="interviews started"
        />
        <StatCard
          icon={<TrendingUp className="w-5 h-5 text-green-400" />}
          label="Average Score"
          value={analytics?.averageScore != null
            ? `${parseFloat(analytics.averageScore).toFixed(1)}/10`
            : '—'}
          sub="across all interviews"
        />
        <StatCard
          icon={<Trophy className="w-5 h-5 text-yellow-400" />}
          label="Best Score"
          value={analytics?.bestScore != null
            ? `${parseFloat(analytics.bestScore).toFixed(1)}/10`
            : '—'}
          sub="personal best"
        />
      </div>

      {/* Score Trend Chart */}
      {chartData.length > 0 && (
        <div className="card">
          <h2 className="text-lg font-semibold text-white mb-6">Score Trend</h2>
          <ResponsiveContainer width="100%" height={220}>
            <BarChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#1f2937" />
              <XAxis dataKey="name" tick={{ fill: '#9ca3af', fontSize: 12 }} />
              <YAxis domain={[0, 10]} tick={{ fill: '#9ca3af', fontSize: 12 }} />
              <Tooltip
                contentStyle={{ backgroundColor: '#111827', border: '1px solid #374151', borderRadius: '8px' }}
                labelStyle={{ color: '#e5e7eb' }}
                itemStyle={{ color: '#818cf8' }}
              />
              <Bar dataKey="score" fill="#6366f1" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}

      {/* Recent History */}
      <div className="card">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold text-white">Recent Sessions</h2>
          <Link to="/history" className="text-sm text-indigo-400 hover:text-indigo-300">
            View all →
          </Link>
        </div>

        {history.length === 0 ? (
          <div className="text-center py-10">
            <p className="text-gray-400 mb-4">No interviews yet. Start your first one!</p>
            <Link to="/interview/setup" className="btn-primary inline-flex items-center gap-2">
              <PlayCircle className="w-4 h-4" /> Start Interview
            </Link>
          </div>
        ) : (
          <div className="space-y-3">
            {history.map(session => (
              <div key={session.id}
                className="flex items-center justify-between p-4 bg-gray-800 rounded-lg">
                <div>
                  <p className="text-sm font-medium text-white">
                    {session.resumeFileName}
                  </p>
                  <p className="text-xs text-gray-400 mt-0.5">
                    {new Date(session.createdAt).toLocaleDateString()} ·{' '}
                    {session.totalQuestions} questions
                  </p>
                </div>
                <div className="flex items-center gap-3">
                  <StatusBadge status={session.status} />
                  {session.overallScore && (
                    <span className="text-sm font-bold text-indigo-400">
                      {parseFloat(session.overallScore).toFixed(1)}/10
                    </span>
                  )}
                  <Link
                    to={`/interview/results/${session.id}`}
                    className="text-xs text-gray-400 hover:text-white"
                  >
                    View →
                  </Link>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

function StatCard({ icon, label, value, sub }) {
  return (
    <div className="card flex items-start gap-4">
      <div className="p-2 bg-gray-800 rounded-lg">{icon}</div>
      <div>
        <p className="text-xs text-gray-400 uppercase tracking-wide">{label}</p>
        <p className="text-2xl font-bold text-white mt-0.5">{value}</p>
        <p className="text-xs text-gray-500 mt-0.5">{sub}</p>
      </div>
    </div>
  )
}

function StatusBadge({ status }) {
  const styles = {
    COMPLETED: 'bg-green-900/40 text-green-400',
    IN_PROGRESS: 'bg-yellow-900/40 text-yellow-400',
    PENDING: 'bg-gray-800 text-gray-400',
  }
  return (
    <span className={`badge ${styles[status] || styles.PENDING}`}>
      {status.replace('_', ' ')}
    </span>
  )
}
