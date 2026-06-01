-- Backfill quiz_result rows for accounts created before the onboarding quiz
-- feature, which never received one. Registration creates the row going forward;
-- this is a one-time data fix for legacy users. Idempotent: skips users that
-- already have a row, so re-running is a no-op.
INSERT INTO quiz_result (user_id, status, created_at)
SELECT id, 'AWAITING', NOW()
FROM app_user
WHERE id NOT IN (SELECT user_id FROM quiz_result WHERE user_id IS NOT NULL);
