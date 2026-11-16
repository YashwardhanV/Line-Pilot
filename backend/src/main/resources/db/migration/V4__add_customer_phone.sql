-- Optional number for a "your turn" SMS/WhatsApp. Cleared by the daily job, so numbers are not
-- kept past the day they were given.
ALTER TABLE queue_tokens ADD COLUMN customer_phone VARCHAR(20);
