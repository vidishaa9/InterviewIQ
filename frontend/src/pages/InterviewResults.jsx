import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import api from '../api/axios'
import { Loader2, Trophy, ChevronDown, ChevronUp, PlayCircle } from 'lucide-react'

/**
 * INTERVIEW RESULTS PAGE
 * Shows the completed session with:
 * → Overall score prominently
 * → Score breakdown by question
 * → Expandable AI feedback per question
 */
export default function InterviewResults() {
  const { sessionId } = useParams()
  const [session, setSession] = useState(null)
  const [expandedQ, setExpandedQ] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get(`/interviews/${sessionId}`)
      .then(r => { setSession(r.data); setLoading(false) })
      .catch(() => setLoading(false))
  }, [sessionId])

  if (loading) return (
    <div className="flex items-center justify-center py-20">
      <Loader2 className="w-8 h-8 animate-spin text-indigo-500" />
    </div>
  )
  if (!session) return (
    <div className="text-center py-20 text-gray-400">Session not found.</div>
  )

  const score = session.overallScore ? parseFloat(session.overallScore) : null
  const scoreColor = score >= 7 ? 'text-green-400' : score >= 5 ? 'text-yellow-400' : 'text-red-400'

  return (
    <div className="max-w-3xl mx-auto space-y-8">
      {/* Score Hero */}
      <div className="card text-center py-10">
        <Trophy className="w-14 h-14 text-yellow-400 mx-auto mb-4" />
        <h1 className="text-2xl font-bold text-white mb-2">Interview Complete!</h1>
        {score !== null ? (
          <>
            <p className={`text-6xl font-black ${scoreColor} mt-4`}>
              {score.toFixed(1)}
              <span className="text-2xl text-gray-500 font-normal">/10</span>
            </p>
            <p className="text-gray-400 mt-2">
              {score >= 8 ? 'Excellent performance! 🎉' :
               score >= 6 ? 'Good job! Keep practicing.' :
               'Keep going — practice makes perfect!'}
            </p>
          </>
        ) : (
          <p className="text-gray-400">Score is being calculated...</p>
        )}
        <Link to="/interview/setup"
          className="btn-primary inline-flex items-center gap-2 mt-6">
          <PlayCircle className="w-4 h-4" /> Practice Again
        </Link>
      </div>

      {/* Per-question breakdown */}
      <div className="space-y-3">
        <h2 className="text-lg font-semibold text-white">Question Breakdown</h2>
        {session.questions.map((q, i) => (
          <div key={q.id} className="card">
            <button
              onClick={() => setExpandedQ(expandedQ === q.id ? null : q.id)}
              className="w-full flex items-start justify-between gap-4 text-left"
            >
              <div className="flex items-start gap-3">
                <span className="w-7 h-7 bg-gray-800 rounded-full flex items-center justify-center
                                 text-xs font-bold text-gray-400 flex-shrink-0 mt-0.5">
                  {i + 1}
                </span>
                <p className="text-sm text-gray-200 font-medium leading-relaxed">
                  {q.questionText}
                </p>
              </div>
              <div className="flex items-center gap-3 flex-shrink-0">
                {q.score && (
                  <span className={`text-lg font-bold ${
                    q.score >= 7 ? 'text-green-400' :
                    q.score >= 5 ? 'text-yellow-400' : 'text-red-400'
                  }`}>
                    {parseFloat(q.score).toFixed(1)}
                  </span>
                )}
                {expandedQ === q.id
                  ? <ChevronUp className="w-4 h-4 text-gray-400" />
                  : <ChevronDown className="w-4 h-4 text-gray-400" />
                }
              </div>
            </button>

            {expandedQ === q.id && q.answered && (
              <div className="mt-4 pt-4 border-t border-gray-800 space-y-3">
                <div>
                  <p className="text-xs text-gray-500 mb-1">Your answer</p>
                  <p className="text-sm text-gray-300">{q.userAnswer}</p>
                </div>
                {q.aiFeedback && (
                  <div>
                    <p className="text-xs text-indigo-400 mb-1">AI Feedback</p>
                    <p className="text-sm text-gray-300">{q.aiFeedback}</p>
                  </div>
                )}
                {q.strengths?.length > 0 && (
                  <div>
                    <p className="text-xs text-green-400 mb-1">Strengths</p>
                    {q.strengths.map((s, si) => (
                      <p key={si} className="text-sm text-gray-300">• {s}</p>
                    ))}
                  </div>
                )}
                {q.improvements?.length > 0 && (
                  <div>
                    <p className="text-xs text-yellow-400 mb-1">To improve</p>
                    {q.improvements.map((imp, ii) => (
                      <p key={ii} className="text-sm text-gray-300">• {imp}</p>
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>
        ))}
      </div>

      <div className="flex gap-3">
        <Link to="/dashboard" className="btn-secondary">Back to Dashboard</Link>
        <Link to="/history" className="btn-secondary">View History</Link>
      </div>
    </div>
  )
}
