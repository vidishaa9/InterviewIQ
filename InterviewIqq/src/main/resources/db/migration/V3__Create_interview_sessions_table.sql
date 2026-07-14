

CREATE TABLE interview_sessions (
                                    id              BIGSERIAL PRIMARY KEY,
                                    user_id         BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                    resume_id       BIGINT          NOT NULL REFERENCES resumes(id) ON DELETE CASCADE,
                                    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
                                    overall_score   DECIMAL(5,2),
                                    total_questions INT             NOT NULL DEFAULT 0,
                                    completed_at    TIMESTAMP,
                                    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sessions_user_id ON interview_sessions(user_id);
CREATE INDEX idx_sessions_resume_id ON interview_sessions(resume_id);