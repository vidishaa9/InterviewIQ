

CREATE TABLE interview_questions (
                                     id              BIGSERIAL PRIMARY KEY,
                                     session_id      BIGINT          NOT NULL REFERENCES interview_sessions(id) ON DELETE CASCADE,
                                     question_text   TEXT            NOT NULL,
                                     category        VARCHAR(30)     NOT NULL DEFAULT 'TECHNICAL',
                                     difficulty      VARCHAR(20)     NOT NULL DEFAULT 'MEDIUM',
                                     user_answer     TEXT,
                                     ai_feedback     TEXT,
                                     strengths       TEXT,
                                     improvements    TEXT,
                                     score           DECIMAL(4,1),
                                     answered_at     TIMESTAMP,
                                     created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_questions_session_id ON interview_questions(session_id);