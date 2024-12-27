CREATE TABLE IF NOT EXISTS client
(
    id                BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY UNIQUE,
    first_name        VARCHAR(64)  NOT NULL,
    last_name         VARCHAR(64)  NOT NULL,
    middle_name       VARCHAR(64),
    passport          VARCHAR(10)  NOT NULL UNIQUE,
    marital_status    VARCHAR(32)  NOT NULL,
    address           VARCHAR(128) NOT NULL,
    phone             VARCHAR(16)  NOT NULL,
    organization_name VARCHAR(96)  NOT NULL,
    position          VARCHAR(64)  NOT NULL,
    employment_period VARCHAR(16), -- ISO-8601

    CONSTRAINT chk_phone_format CHECK (phone ~ '^[0-9+\-() ]+$')
);

CREATE TABLE IF NOT EXISTS credit_application
(
    id               BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY UNIQUE,
    client_id        BIGINT         NOT NULL,
    requested_amount NUMERIC(15, 2) NOT NULL,
    status           VARCHAR(8)     NOT NULL DEFAULT 'PENDING',
    approved_amount  NUMERIC(15, 2),
    approved_term    INT CHECK (approved_term BETWEEN 1 AND 365), -- in days
    created_at       timestamptz    NOT NULL DEFAULT CURRENT_DATE,
    FOREIGN KEY (client_id) REFERENCES client (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS credit_agreement
(
    id                    BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY UNIQUE,
    credit_application_id BIGINT      NOT NULL UNIQUE,
    signed_at             timestamptz,
    signing_status        VARCHAR(20) NOT NULL DEFAULT 'NOT_SIGNED',
    FOREIGN KEY (credit_application_id) REFERENCES credit_application (id) ON DELETE CASCADE
);
