import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/axios'
import { Loader2, PlayCircle, FileText } from 'lucide-react'

/**
 * INTERVIEW SETUP PAGE
 * User picks a resume and optionally sets a target role.
 * On submit: POST /api/interviews/start → creates session with questions
 * Then navigates to /interview/session/:id
 */
export default function InterviewSetup() {
  const [resumes, setResumes] = useState([])
  const [selectedResume, setSelectedResume] = useState('')
  const [targetRole, setTargetRole] = useState('')
  const [loading, setLoading] = useState(true)
  const [starting, setStarting] = useState(false)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    api.get('/resumes').then(r => {
      setResumes(r.data)
      setLoading(false)
    }).catch(() => setLoading(false))
  }, [])

  const handleStart = async () => {
    if (!selectedResume) { setError('Please select a resume'); return }
    setError('')
    setStarting(true)
    try {
      const res = await api.post('/interviews/start', {
        resumeId: parseInt(selectedResume),
        targetRole: targetRole || null,
      })
      // Begin the session (move from PENDING to IN_PROGRESS)
      await api.post(`/interviews/${res.data.id}/begin`)
      navigate(`/interview/session/${res.data.id}`)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to start interview')
      setStarting(false)
    }
  }

  if (loading) return (
    <div className="flex items-center justify-center py-20">
      <Loader2 className="w-8 h-8 animate-spin text-indigo-500" />
    </div>
  )

  if (resumes.length === 0) return (
    <div className="card text-center py-16 max-w-md mx-auto">
      <FileText className="w-12 h-12 text-gray-600 mx-auto mb-4" />
      <h2 className="text-xl font-semibold text-white mb-2">No Resumes Found</h2>
      <p className="text-gray-400 mb-6">Upload a resume first to generate personalized questions.</p>
      <a href="/resumes" className="btn-primary inline-block">Upload Resume</a>
    </div>
  )

  return (
    <div className="max-w-xl mx-auto space-y-6">
      <h1 className="text-2xl font-bold text-white">Start Mock Interview</h1>

      <div className="card space-y-5">
        <div>
          <label className="block text-sm font-medium text-gray-300 mb-2">
            Select Resume
          </label>
          <select
            value={selectedResume}
            onChange={e => setSelectedResume(e.target.value)}
            className="input"
          >
            <option value="">Choose a resume...</option>
            {resumes.map(r => (
              <option key={r.id} value={r.id}>{r.fileName}</option>
            ))}
          </select>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-300 mb-2">
            Target Role <span className="text-gray-500">(optional)</span>
          </label>
          <input
            type="text"
            value={targetRole}
            onChange={e => setTargetRole(e.target.value)}
            placeholder="e.g. Backend Developer, Full Stack Engineer"
            className="input"
          />
        </div>

        {error && (
          <p className="text-red-400 text-sm">{error}</p>
        )}

        <div className="bg-gray-800 rounded-lg p-4 text-sm text-gray-400 space-y-1">
          <p>📋 8 questions — mix of Technical, Behavioral & Situational</p>
          <p>🤖 AI evaluates each answer and gives a score + feedback</p>
          <p>⏱ Takes about 15-25 minutes</p>
        </div>

        <button
          onClick={handleStart}
          disabled={starting || !selectedResume}
          className="btn-primary w-full flex items-center justify-center gap-2 py-3"
        >
          {starting
            ? <><Loader2 className="w-4 h-4 animate-spin" /> Generating questions...</>
            : <><PlayCircle className="w-4 h-4" /> Start Interview</>
          }
        </button>
      </div>
    </div>
  )
}
