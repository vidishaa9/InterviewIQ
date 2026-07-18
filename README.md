# InterviewIQ
AI-powered mock interview platform — upload your resume, practice with personalized questions, get scored feedback.

---

## Tech Stack
**Backend** — Spring Boot 3.3, Spring Security 6, JWT, PostgreSQL, Flyway, PDFBox, Gemini 2.0 Flash

**Frontend** — React 18, Vite, Tailwind CSS, React Router v6, Recharts, Axios

---

## What It Does
1. User uploads a resume PDF
2. Gemini AI extracts skills and experience level
3. AI generates 8 personalized interview questions
4. User answers each question in a live session
5. AI scores each answer (0–10) with feedback and improvement tips
6. Dashboard tracks score trends over time

---

## Running Locally

**Prerequisites** — Java 17+, Node.js 18+, Docker, Gemini API key 

```bash
# 1. Start PostgreSQL
docker run --name resumeai-db \
  -e POSTGRES_USER=admin \
  -e POSTGRES_PASSWORD=password \
  -e POSTGRES_DB=resumeai \
  -p 5432:5432 -d postgres:16

# 2. Run backend (Flyway creates all tables automatically)
./mvnw spring-boot:run

# 3. Run frontend
cd frontend && npm install && npm run dev
```

Backend → http://localhost:8089  
Frontend → http://localhost:5173

---

## Key Technical Decisions

- **Flyway migrations** — schema versioned in `db/migration/`, no ddl-auto in production
- **Session state machine** — PENDING → IN_PROGRESS → COMPLETED enforced server-side
- **Pre-aggregated analytics** — one row per user updated post-session, dashboard loads with a single SELECT
- **GlobalExceptionHandler** — every error returns `{ status, message, timestamp }` consistently
- **Axios JWT interceptor** — token attached to every request automatically, 401s redirect to login

---

