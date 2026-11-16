-- A skipped (no-show) token can be put back in the line once; this points to the new token.
ALTER TABLE queue_tokens ADD COLUMN rejoined_as_id BIGINT REFERENCES queue_tokens(id);
