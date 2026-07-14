

CREATE TABLE analytics (
                           id                  BIGSERIAL PRIMARY KEY,
                           user_id             BIGINT          NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
                           total_sessions      INT             NOT NULL DEFAULT 0,
                           completed_sessions  INT             NOT NULL DEFAULT 0,
                           average_score       DECIMAL(5,2)    DEFAULT 0,
                           best_score          DECIMAL(5,2)    DEFAULT 0,
                           top_skills          TEXT,
                           weak_areas          TEXT,
                           updated_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_analytics_user_id ON analytics(user_id);