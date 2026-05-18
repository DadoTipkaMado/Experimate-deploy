ALTER TABLE tour_listing ADD COLUMN max_guests INTEGER NOT NULL DEFAULT 1;
ALTER TABLE tour_listing DROP COLUMN reserved;
