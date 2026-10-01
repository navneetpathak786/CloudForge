CREATE TABLE workers (
    id VARCHAR(255) PRIMARY KEY,
    cpu_capacity DOUBLE PRECISION NOT NULL,
    memory_capacity BIGINT NOT NULL,
    cpu_available DOUBLE PRECISION NOT NULL,
    memory_available BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    registered_at TIMESTAMPTZ NOT NULL
);
