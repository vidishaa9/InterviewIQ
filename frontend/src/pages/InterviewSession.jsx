import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import api from '../api/axios'
import { Loader2, Send, CheckCircle, ChevronRight } from 'lucide-react'

/**
 * INTERVIEW SESSION PAGE
 *
 * The core interactive page of the app. Shows one question at a time.
 *
 * FLOW:
 * 1. Load session (GET /api/interviews/:id)
 * 2. Show first unanswered question
 * 3. User types answer → Submit button
 *    → POST /api/interviews/:sessionId/questions/:qId/answer
 *    → Backend calls Gemini to score it
 *    → Response has score + feedback
 * 4. Show brief feedback, move to next question
 * 5. After all questions → Complete session button
 *    → POST /api/interviews/:id/complete
 *    → Navigate to /interview/results/:id
 */
export default function InterviewSession() {
  const { sessionId } = useParams()
  const navigate = useNavigate()
  const [session, setSession] = useState(null)
  const [currentIndex, setCurrentIndex] = useState(0)
  const [answer, setAnswer] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [lastFeedback, setLastFeedback] = useState(null)
  const [completing, setCompleting] = useState(false)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get(`/interviews/${sessionId}`)
      .then(r => {
        setSession(r.data)
        // Find first unanswered question
        const firstUnanswered = r.data.questions.findIndex(q => !q.answered)
        setCurrentIndex(firstUnanswered === -1 ? r.data.questions.length - 1 : firstUnanswered)
        setLoading(false)
      })
      .catch(() => navigate('/history'))
  }, [sessionId])

  const currentQuestion = session?.questions[currentIndex]
  const answeredCount = session?.questions.filter(q => q.answered).length ?? 0
  const allAnswered = answeredCount === session?.questions.length

  const handleSubmit = async () => {
    if (!answer.trim()) return
    setSubmitting(true)
    try {
      const res = await api.post(
        `/interviews/${sessionId}/questions/${currentQuestion.id}/answer`,
        { answer: answer.trim() }
      )
      // Update the question in local state
      setSession(prev => ({
        ...prev,
        questions: prev.questions.map(q =>
          q.id === currentQuestion.id ? { ...q, ...res.data, answered: true } : q
        )
      }))
      setLastFeedback(res.data)
      setAnswer('')
    } catch (err) {
      console.error(err)
    } finally {
      setSubmitting(false)
    }
  }

  const handleComplete = async () => {
    setCompleting(true)
    try {
      await api.post(`/interviews/${sessionId}/complete`)
      navigate(`/interview/results/${sessionId}`)
    } catch (err) {
      console.error(err)
      setCompleting(false)
    }
  }

  const moveToNext = () => {
    setLastFeedback(null)
    if (currentIndex < session.questions.length - 1) {
      setCurrentIndex(currentIndex + 1)
    }
  }

  if (loading) return (
    <div className="flex items-center justify-center py-20">
      <Loader2 className="w-8 h-8 animate-spin text-indigo-500" />
    </div>
  )

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      {/* Progress Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-white">Mock Interview</h1>
          <p className="text-gray-400 text-sm">
            Question {currentIndex + 1} of {session.questions.length}
          </p>
        </div>
        <span className="text-sm text-gray-400">
          {answeredCount}/{session.questions.length} answered
        </span>
      </div>

      {/* Progress bar */}
      <div className="w-full bg-gray-800 rounded-full h-2">
        <div
          className="bg-indigo-600 h-2 rounded-full transition-all duration-500"
          style={{ width: `${(answeredCount / session.questions.length) * 100}%` }}
        />
      </div>

      {/* Question Card */}
      {currentQuestion && (
        <div className="card space-y-4">
          <div className="flex items-center gap-2">
            <span className={`badge ${
              currentQuestion.category === 'TECHNICAL' ? 'bg-blue-900/40 text-blue-400' :
              currentQuestion.category === 'BEHAVIORAL' ? 'bg-purple-900/40 text-purple-400' :
              'bg-orange-900/40 text-orange-400'
            }`}>
              {currentQuestion.category}
            </span>
            <span className={`badge ${
              currentQuestion.difficulty === 'EASY' ? 'bg-green-900/40 text-green-400' :
              currentQuestion.difficulty === 'HARD' ? 'bg-red-900/40 text-red-400' :
              'bg-yellow-900/40 text-yellow-400'
            }`}>
              {currentQuestion.difficulty}
            </span>
          </div>

          <p className="text-lg font-medium text-white leading-relaxed">
            {currentQuestion.questionText}
          </p>

          {/* If question already answered, show it */}
          {currentQuestion.answered ? (
            <div className="space-y-3">
              <div className="bg-gray-800 rounded-lg p-4">
                <p className="text-xs text-gray-500 mb-1">Your answer</p>
                <p className="text-gray-300 text-sm">{currentQuestion.userAnswer}</p>
              </div>
              <div className="flex items-center gap-2 text-green-400">
                <CheckCircle className="w-4 h-4" />
                <span className="text-sm font-medium">
                  Score: {currentQuestion.score}/10
                </span>
              </div>
            </div>
          ) : (
            /* Answer textarea */
            <div className="space-y-3">
              <textarea
                value={answer}
                onChange={e => setAnswer(e.target.value)}
                rows={6}
                placeholder="Type your answer here..."
                disabled={submitting}
                className="input resize-none"
              />
              <button
                onClick={handleSubmit}
                disabled={submitting || !answer.trim()}
                className="btn-primary flex items-center gap-2"
              >
                {submitting
                  ? <><Loader2 className="w-4 h-4 animate-spin" /> Evaluating...</>
                  : <><Send className="w-4 h-4" /> Submit Answer</>
                }
              </button>
            </div>
          )}
        </div>
      )}

      {/* AI Feedback shown right after submission */}
      {lastFeedback && (
        <div className="card space-y-4 border-green-800">
          <div className="flex items-center justify-between">
            <h3 className="font-semibold text-white">AI Feedback</h3>
            <span className="text-2xl font-bold text-indigo-400">
              {lastFeedback.score}/10
            </span>
          </div>
          <p className="text-gray-300 text-sm leading-relaxed">{lastFeedback.aiFeedback}</p>
          {lastFeedback.strengths?.length > 0 && (
            <div>
              <p className="text-xs text-green-400 font-medium mb-1.5">✓ Strengths</p>
              <ul className="space-y-1">
                {lastFeedback.strengths.map((s, i) => (
                  <li key={i} className="text-sm text-gray-300">• {s}</li>
                ))}
              </ul>
            </div>
          )}
          {lastFeedback.improvements?.length > 0 && (
            <div>
              <p className="text-xs text-yellow-400 font-medium mb-1.5">↑ Improvements</p>
              <ul className="space-y-1">
                {lastFeedback.improvements.map((imp, i) => (
                  <li key={i} className="text-sm text-gray-300">• {imp}</li>
                ))}
              </ul>
            </div>
          )}
          {!allAnswered && (
            <button onClick={moveToNext} className="btn-secondary flex items-center gap-2">
              Next Question <ChevronRight className="w-4 h-4" />
            </button>
          )}
        </div>
      )}

      {/* Complete button */}
      {allAnswered && (
        <div className="card text-center py-6">
          <CheckCircle className="w-12 h-12 text-green-400 mx-auto mb-3" />
          <h3 className="text-lg font-semibold text-white mb-2">All questions answered!</h3>
          <p className="text-gray-400 text-sm mb-4">Complete the session to see your final score.</p>
          <button
            onClick={handleComplete}
            disabled={completing}
            className="btn-primary flex items-center gap-2 mx-auto"
          >
            {completing
              ? <><Loader2 className="w-4 h-4 animate-spin" /> Calculating score...</>
              : 'Complete & See Results'
            }
          </button>
        </div>
      )}

      {/* Question navigation dots */}
      <div className="flex gap-2 justify-center flex-wrap">
        {session.questions.map((q, i) => (
          <button
            key={q.id}
            onClick={() => { setLastFeedback(null); setCurrentIndex(i) }}
            className={`w-8 h-8 rounded-full text-xs font-medium transition-colors
              ${i === currentIndex ? 'bg-indigo-600 text-white' :
                q.answered ? 'bg-green-800 text-green-300' :
                'bg-gray-700 text-gray-400 hover:bg-gray-600'}`}
          >
            {i + 1}
          </button>
        ))}
      </div>
    </div>
  )
}
