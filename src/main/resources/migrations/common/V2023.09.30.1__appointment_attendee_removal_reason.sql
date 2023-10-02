-- ========================================
-- ADD APPOINTMENT ATTENDEE REMOVAL REASONS
-- ========================================

CREATE TABLE appointment_attendee_removal_reason
(
    appointment_attendee_removal_reason_id  bigserial   NOT NULL CONSTRAINT appointment_attendee_removal_reason_pk PRIMARY KEY,
    description                             varchar(50) NOT NULL,
    is_delete                               boolean     NOT NULL
);

INSERT INTO appointment_attendee_removal_reason (appointment_attendee_removal_reason_id, description, is_delete)
VALUES   (1, 'Permanent removal by user', true),
         (2, 'Temporary removal by user', false),
         (3, 'Cancel on transfer - NOMIS OCUCANTR form', true),
         (4, 'Prisoner status: Released', true),
         (5, 'Prisoner status: Permanent transfer', true),
         (6, 'Prisoner status: Temporary transfer', false);

ALTER TABLE appointment_attendee DROP COLUMN removed_by;
ALTER TABLE appointment_attendee ADD COLUMN removal_reason_id bigint DEFAULT NULL REFERENCES appointment_attendee_removal_reason (appointment_attendee_removal_reason_id);
ALTER TABLE appointment_attendee ADD COLUMN removed_by varchar(100) DEFAULT NULL;
ALTER TABLE appointment_attendee ADD COLUMN is_deleted boolean NOT NULL DEFAULT false;

DROP INDEX idx_appointment_attendee_removed_time;
CREATE INDEX idx_appointment_attendee_removal_reason_id ON appointment_attendee (removal_reason_id);
CREATE INDEX idx_appointment_attendee_is_deleted ON appointment_attendee (is_deleted);

-- ===================================================
-- UPDATE VIEWS USING APPOINTMENT ATTENDEE SOFT DELETE
-- ===================================================

CREATE OR REPLACE VIEW v_appointment_instance AS
SELECT aa.appointment_attendee_id                                                    AS appointment_instance_id,
       a.appointment_series_id,
       aa.appointment_id,
       aa.appointment_attendee_id,
       asrs.appointment_type,
       a.prison_code,
       aa.prisoner_number,
       aa.booking_id,
       a.category_code,
       a.custom_name,
       a.appointment_tier_id,
       a.appointment_host_id,
       CASE
           WHEN a.in_cell THEN null
           ELSE a.internal_location_id END                                          AS internal_location_id,
       a.custom_location,
       a.in_cell,
       a.on_wing,
       a.off_wing,
       a.start_date                                                                 AS appointment_date,
       a.start_time,
       a.end_time,
       a.unlock_notes,
       a.extra_information,
       a.created_time,
       a.created_by,
       a.updated_time,
       a.updated_by,
       CASE
           WHEN a.cancellation_reason_id IS NULL THEN false
           ELSE NOT acr.is_delete END                                               AS is_cancelled,
       a.cancelled_time,
       a.cancelled_by,
       CASE
           WHEN aa.removal_reason_id IS NULL THEN false
           ELSE NOT aarr.is_delete END                                              AS is_removed,
       aa.removed_time,
       aa.removed_by
FROM appointment_attendee aa
         JOIN appointment a on aa.appointment_id = a.appointment_id
         JOIN appointment_series asrs on asrs.appointment_series_id = a.appointment_series_id
         LEFT JOIN appointment_cancellation_reason acr on a.cancellation_reason_id = acr.appointment_cancellation_reason_id
         LEFT JOIN appointment_attendee_removal_reason aarr on aa.removal_reason_id = aarr.appointment_attendee_removal_reason_id
WHERE NOT aa.is_deleted AND NOT a.is_deleted;
