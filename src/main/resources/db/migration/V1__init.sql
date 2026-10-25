CREATE TABLE users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(120) NOT NULL,
    created_at    TIMESTAMP    NOT NULL,
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE analyses (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    resume_file_name  VARCHAR(255) NOT NULL,
    job_title         VARCHAR(200) NOT NULL,
    job_description   TEXT         NOT NULL,
    fit_score         INT          NOT NULL,
    skill_coverage    DOUBLE       NOT NULL,
    text_similarity   DOUBLE       NOT NULL,
    verdict           VARCHAR(20)  NOT NULL,
    matched_skills    TEXT         NOT NULL,
    missing_skills    TEXT         NOT NULL,
    extra_skills      TEXT         NOT NULL,
    suggestions       TEXT         NOT NULL,
    created_at        TIMESTAMP    NOT NULL,
    CONSTRAINT fk_analyses_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_analyses_user_created ON analyses (user_id, created_at);
