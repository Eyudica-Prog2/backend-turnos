ALTER TABLE reservation
    ADD COLUMN phone_submission_status VARCHAR(20) NOT NULL DEFAULT 'NOT_SENT',
    ADD COLUMN phone_submission_event_id VARCHAR(120);

ALTER TABLE reservation
    ADD CONSTRAINT ck_reservation_phone_submission_status
        CHECK (phone_submission_status IN ('NOT_SENT', 'PENDING', 'SENT'));
