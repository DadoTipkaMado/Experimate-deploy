-- Premium expiry timestamp backing the PREMIUM_USER role and one-time premium purchase.
-- NULL means the user is not premium; existing rows default to NULL so current
-- (non-premium) accounts are left untouched by this migration.
ALTER TABLE app_user
    ADD COLUMN premium_until timestamp(6) without time zone DEFAULT NULL;
