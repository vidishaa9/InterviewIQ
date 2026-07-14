

CREATE TABLE resumes (
                         id                  BIGSERIAL PRIMARY KEY,
                         user_id             BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                         file_name           VARCHAR(255)    NOT NULL,
                         file_path           VARCHAR(500)    NOT NULL,
                         extracted_skills    TEXT,
                         experience_level    VARCHAR(20),
                         job_roles           TEXT,
                         raw_text            TEXT,
                         uploaded_at         TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_resumes_user_id ON resumes(user_id);