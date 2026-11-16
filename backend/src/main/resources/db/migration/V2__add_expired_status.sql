-- Tokens left over from an earlier day are closed as EXPIRED instead of carrying into today's line.
ALTER TABLE queue_tokens DROP CONSTRAINT ck_queue_tokens_status;
ALTER TABLE queue_tokens ADD CONSTRAINT ck_queue_tokens_status
    CHECK (status IN ('WAITING', 'CALLED', 'SERVING', 'COMPLETED', 'SKIPPED', 'CANCELLED', 'EXPIRED'));
