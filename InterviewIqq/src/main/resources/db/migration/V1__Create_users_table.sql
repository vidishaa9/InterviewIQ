CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       full_name VARCHAR(100) NOT NULL,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);