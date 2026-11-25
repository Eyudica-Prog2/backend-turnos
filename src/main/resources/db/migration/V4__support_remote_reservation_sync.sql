ALTER TABLE reservation
    ALTER COLUMN reservation_process_id DROP NOT NULL;

ALTER TABLE reservation
    ADD COLUMN catedra_reservation_process_id VARCHAR(120) UNIQUE;
