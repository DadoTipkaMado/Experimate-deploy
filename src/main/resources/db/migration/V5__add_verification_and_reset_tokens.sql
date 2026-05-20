CREATE TABLE email_verification_token (
    id                   SERIAL PRIMARY KEY,
    token_hash           VARCHAR(64) NOT NULL UNIQUE,
    user_id              INTEGER     NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    expiration_date_time TIMESTAMP   NOT NULL
);

CREATE TABLE password_reset_token (
    id                   SERIAL PRIMARY KEY,
    token_hash           VARCHAR(64) NOT NULL UNIQUE,
    user_id              INTEGER     NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    expiration_date_time TIMESTAMP   NOT NULL
);
