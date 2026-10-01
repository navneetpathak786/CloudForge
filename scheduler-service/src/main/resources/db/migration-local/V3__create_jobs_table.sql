CREATE TABLE jobs (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    command TEXT NOT NULL,
    cpu_requirement DOUBLE PRECISION NOT NULL,
    memory_requirement BIGINT NOT NULL,
    max_retries INT NOT NULL,
    priority INT,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    assigned_worker_id VARCHAR(255)
);
