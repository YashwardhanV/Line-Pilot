CREATE TABLE user_accounts (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(60) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_accounts_username UNIQUE (username)
);

CREATE TABLE service_queues (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    location VARCHAR(160) NOT NULL,
    token_prefix VARCHAR(5) NOT NULL,
    open BOOLEAN NOT NULL DEFAULT TRUE,
    default_service_minutes INTEGER NOT NULL,
    sequence_date DATE,
    last_sequence INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_service_queues_code UNIQUE (code),
    CONSTRAINT ck_service_queues_default_minutes CHECK (default_service_minutes BETWEEN 1 AND 240),
    CONSTRAINT ck_service_queues_last_sequence CHECK (last_sequence >= 0)
);

CREATE TABLE queue_tokens (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL,
    service_queue_id BIGINT NOT NULL REFERENCES service_queues(id),
    sequence_number INTEGER NOT NULL,
    service_date DATE NOT NULL,
    display_number VARCHAR(20) NOT NULL,
    customer_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    claimed_by_id BIGINT REFERENCES user_accounts(id),
    joined_at TIMESTAMPTZ NOT NULL,
    called_at TIMESTAMPTZ,
    serving_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_queue_tokens_public_id UNIQUE (public_id),
    CONSTRAINT uk_queue_tokens_daily_sequence UNIQUE (service_queue_id, service_date, sequence_number),
    CONSTRAINT ck_queue_tokens_status CHECK (status IN ('WAITING', 'CALLED', 'SERVING', 'COMPLETED', 'SKIPPED', 'CANCELLED'))
);

CREATE INDEX idx_queue_tokens_call_next
    ON queue_tokens (service_queue_id, joined_at, id)
    WHERE status = 'WAITING';

CREATE INDEX idx_queue_tokens_history
    ON queue_tokens (service_queue_id, joined_at DESC);

CREATE INDEX idx_queue_tokens_wait_estimate
    ON queue_tokens (service_queue_id, completed_at DESC)
    WHERE status = 'COMPLETED';

CREATE UNIQUE INDEX uk_queue_tokens_active_staff_claim
    ON queue_tokens (claimed_by_id)
    WHERE claimed_by_id IS NOT NULL AND status IN ('CALLED', 'SERVING');
