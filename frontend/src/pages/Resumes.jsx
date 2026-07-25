import { useState, useEffect, useRef } from 'react'
import api from '../api/axios'
import { Upload, FileText, Trash2, Loader2, CheckCircle, X } from 'lucide-react'

/**
 * RESUMES PAGE
 *
 * Two sections:
 * 1. Upload zone — drag & drop or click to upload a PDF
 *    → Sends multipart/form-data POST to /api/resumes/upload
 *    → Backend extracts text, calls Gemini, returns skills
 *    → Shows extracted skills as badges
 *
 * 2. Resume list — all uploaded resumes for this user
 *    → Each card shows filename, skills, experience level
 *    → Delete button removes from list + server
 *
 * MULTIPART UPLOAD:
 * FormData is the browser API for sending files.
 * axios automatically sets Content-Type: multipart/form-data
 * when you pass a FormData object as the request body.
 */
export default function Resumes() {
  const [resumes, setResumes] = useState([])
  const [uploading, setUploading] = useState(false)
  const [uploadError, setUploadError] = useState('')
  const [uploadSuccess, setUploadSuccess] = useState(null)
  const [dragging, setDragging] = useState(false)
  const fileInputRef = useRef(null)

  useEffect(() => { fetchResumes() }, [])

  const fetchResumes = async () => {
    try {
      const res = await api.get('/resumes')
      setResumes(res.data)
    } catch (err) {
      console.error(err)
    }
  }

  const handleFile = async (file) => {
    if (!file) return
    if (file.type !== 'application/pdf') {
      setUploadError('Please upload a PDF file only.')
      return
    }
    if (file.size > 10 * 1024 * 1024) {
      setUploadError('File must be under 10MB.')
      return
    }

    setUploadError('')
    setUploadSuccess(null)
    setUploading(true)

    const formData = new FormData()
    formData.append('file', file)

    try {
      const res = await api.post('/resumes/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      setUploadSuccess(res.data)
      setResumes(prev => [res.data, ...prev])
    } catch (err) {
      setUploadError(err.response?.data?.message || 'Upload failed. Please try again.')
    } finally {
      setUploading(false)
    }
  }

  const handleDelete = async (id) => {
    if (!confirm('Delete this resume?')) return
    try {
      await api.delete(`/resumes/${id}`)
      setResumes(prev => prev.filter(r => r.id !== id))
    } catch (err) {
      console.error(err)
    }
  }

  const onDrop = (e) => {
    e.preventDefault()
    setDragging(false)
    const file = e.dataTransfer.files[0]
    handleFile(file)
  }

  return (
    <div className="space-y-8">
      <h1 className="text-2xl font-bold text-white">My Resumes</h1>

      {/* Upload Zone */}
      <div
        onDragOver={(e) => { e.preventDefault(); setDragging(true) }}
        onDragLeave={() => setDragging(false)}
        onDrop={onDrop}
        onClick={() => !uploading && fileInputRef.current?.click()}
        className={`border-2 border-dashed rounded-xl p-12 text-center cursor-pointer transition-all
          ${dragging
            ? 'border-indigo-500 bg-indigo-900/20'
            : 'border-gray-700 bg-gray-900 hover:border-gray-600'
          }
          ${uploading ? 'opacity-50 cursor-not-allowed' : ''}`}
      >
        <input
          ref={fileInputRef}
          type="file"
          accept=".pdf"
          className="hidden"
          onChange={e => handleFile(e.target.files[0])}
        />

        {uploading ? (
          <div className="flex flex-col items-center gap-3">
            <Loader2 className="w-10 h-10 text-indigo-500 animate-spin" />
            <p className="text-gray-300 font-medium">Analyzing your resume with AI...</p>
            <p className="text-gray-500 text-sm">This takes about 10-15 seconds</p>
          </div>
        ) : (
          <div className="flex flex-col items-center gap-3">
            <Upload className="w-10 h-10 text-gray-500" />
            <p className="text-gray-300 font-medium">Drop your PDF here or click to browse</p>
            <p className="text-gray-500 text-sm">PDF only · Max 10MB</p>
          </div>
        )}
      </div>

      {/* Upload feedback */}
      {uploadError && (
        <div className="flex items-center gap-2 bg-red-900/30 border border-red-700
                        text-red-400 px-4 py-3 rounded-lg text-sm">
          <X className="w-4 h-4 flex-shrink-0" />
          {uploadError}
        </div>
      )}
      {uploadSuccess && (
        <div className="bg-green-900/30 border border-green-700 text-green-400 px-4 py-3
                        rounded-lg text-sm">
          <div className="flex items-center gap-2 mb-2">
            <CheckCircle className="w-4 h-4" />
            <span className="font-medium">Resume processed successfully!</span>
          </div>
          <p className="text-xs text-green-500">
            Found {uploadSuccess.extractedSkills?.length || 0} skills ·{' '}
            {uploadSuccess.experienceLevel} level
          </p>
        </div>
      )}

      {/* Resume List */}
      {resumes.length === 0 && !uploading ? (
        <div className="card text-center py-10">
          <FileText className="w-12 h-12 text-gray-600 mx-auto mb-3" />
          <p className="text-gray-400">No resumes uploaded yet</p>
        </div>
      ) : (
        <div className="grid gap-4">
          {resumes.map(resume => (
            <div key={resume.id} className="card">
              <div className="flex items-start justify-between gap-4">
                <div className="flex items-start gap-3">
                  <div className="p-2 bg-indigo-900/40 rounded-lg flex-shrink-0">
                    <FileText className="w-5 h-5 text-indigo-400" />
                  </div>
                  <div>
                    <p className="font-medium text-white">{resume.fileName}</p>
                    <p className="text-xs text-gray-500 mt-0.5">
                      {new Date(resume.uploadedAt).toLocaleDateString()} ·{' '}
                      <span className="capitalize">{resume.experienceLevel} level</span>
                    </p>

                    {/* Skills */}
                    <div className="flex flex-wrap gap-1.5 mt-3">
                      {resume.extractedSkills?.slice(0, 12).map(skill => (
                        <span key={skill}
                          className="badge bg-indigo-900/40 text-indigo-300 border border-indigo-800">
                          {skill}
                        </span>
                      ))}
                      {resume.extractedSkills?.length > 12 && (
                        <span className="badge bg-gray-800 text-gray-400">
                          +{resume.extractedSkills.length - 12} more
                        </span>
                      )}
                    </div>
                  </div>
                </div>

                <button
                  onClick={() => handleDelete(resume.id)}
                  className="p-2 text-gray-500 hover:text-red-400 hover:bg-red-900/20
                             rounded-lg transition-colors flex-shrink-0"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
