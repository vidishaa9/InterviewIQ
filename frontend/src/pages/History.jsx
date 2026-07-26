import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import api from '../api/axios'
import { Loader2, History as HistoryIcon, ExternalLink } from 'lucide-react'

/**
 * HISTORY PAGE
 * Shows all past interview sessions in a table.
 * Fetches from GET /api/interviews/history
 * Each row links to the results page for that session.
 */
export default function History() {
  const [sessions, setSessions] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get('/interviews/history')
      .then(r => { setSessions(r.data); setLoading(false) })
      .catch(() => setLoading(false))
  }, [])

  if (loading) return (
    <div className="flex items-center justify-center py-20">
      <Loader2 className="w-8 h-8 animate-spin text-indigo-500" />
    </div>
  )

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-white">Interview History</h1>
        <Link to="/interview/setup" className="btn-primary text-sm">
          + New Interview
        </Link>
      </div>

      {sessions.length === 0 ? (
        <div className="card text-center py-16">
          <HistoryIcon className="w-12 h-12 text-gray-600 mx-auto mb-4" />
          <p className="text-gray-400 mb-4">No interviews yet.</p>
          <Link to="/interview/setup" className="btn-primary inline-block">
            Start Your First Interview
          </Link>
        </div>
      ) : (
        <div className="card p-0 overflow-hidden">
          <table className="w-full">
            <thead>
              <tr className="border-b border-gray-800 text-left">
                <th className="px-6 py-4 text-xs font-medium text-gray-400 uppercase tracking-wide">Resume</th>
                <th className="px-6 py-4 text-xs font-medium text-gray-400 uppercase tracking-wide">Date</th>
                <th className="px-6 py-4 text-xs font-medium text-gray-400 uppercase tracking-wide">Questions</th>
                <th className="px-6 py-4 text-xs font-medium text-gray-400 uppercase tracking-wide">Status</th>
                <th className="px-6 py-4 text-xs font-medium text-gray-400 uppercase tracking-wide">Score</th>
                <th className="px-6 py-4 text-xs font-medium text-gray-400 uppercase tracking-wide"></th>
              </tr>
            </thead>
            <tbody>
              {sessions.map((session, i) => (
                <tr
                  key={session.id}
                  className={`border-b border-gray-800 last:border-0 hover:bg-gray-800/50 transition-colors`}
                >
                  <td className="px-6 py-4">
                    <p className="text-sm text-white font-medium truncate max-w-[180px]">
                      {session.resumeFileName}
                    </p>
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-400">
                    {new Date(session.createdAt).toLocaleDateString('en-US', {
                      month: 'short', day: 'numeric', year: 'numeric'
                    })}
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-400">
                    {session.totalQuestions} Qs
                  </td>
                  <td className="px-6 py-4">
                    <StatusBadge status={session.status} />
                  </td>
                  <td className="px-6 py-4">
                    {session.overallScore != null ? (
                      <span className={`text-sm font-bold ${
                        parseFloat(session.overallScore) >= 7 ? 'text-green-400' :
                        parseFloat(session.overallScore) >= 5 ? 'text-yellow-400' :
                        'text-red-400'
                      }`}>
                        {parseFloat(session.overallScore).toFixed(1)}/10
                      </span>
                    ) : (
                      <span className="text-gray-600 text-sm">—</span>
                    )}
                  </td>
                  <td className="px-6 py-4">
                    <Link
                      to={`/interview/results/${session.id}`}
                      className="flex items-center gap-1 text-xs text-indigo-400
                                 hover:text-indigo-300 transition-colors"
                    >
                      View <ExternalLink className="w-3 h-3" />
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
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
