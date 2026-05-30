-- Optional link from a promoted_ad to the partner_event it promotes.
-- NULL for regular free-form ads. The UNIQUE constraint enforces at most one promotion
-- per event at a time (Postgres treats NULLs as distinct, so many regular ads coexist).
-- ON DELETE CASCADE: deleting an event automatically removes its promotion from the feed.
ALTER TABLE promoted_ad
    ADD COLUMN partner_event_id integer;

ALTER TABLE ONLY promoted_ad
    ADD CONSTRAINT fk_promoted_ad_event FOREIGN KEY (partner_event_id) REFERENCES partner_event(id) ON DELETE CASCADE;

ALTER TABLE ONLY promoted_ad
    ADD CONSTRAINT uq_promoted_ad_event UNIQUE (partner_event_id);
