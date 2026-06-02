ALTER TABLE tour_listing ADD COLUMN host_checked_in BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tour_listing ADD COLUMN host_check_in_timestamp TIMESTAMP;
ALTER TABLE tour_listing ADD COLUMN tour_started_at TIMESTAMP;
ALTER TABLE tour_reservation DROP COLUMN host_checked_in;
ALTER TABLE tour_reservation DROP COLUMN host_check_in_timestamp;
