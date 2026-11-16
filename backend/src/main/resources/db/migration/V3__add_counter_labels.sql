-- Where a staff member sits ("Counter 2"), and a copy on each token taken at call time, so history
-- still shows the right counter after a staff member moves desks.
ALTER TABLE user_accounts ADD COLUMN counter_label VARCHAR(40);
ALTER TABLE queue_tokens ADD COLUMN counter_label VARCHAR(40);
