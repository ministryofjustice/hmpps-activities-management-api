CREATE OR REPLACE VIEW v_sar_appointment AS
SELECT ap.prison_code AS prison_code,
       ap.appointment_id AS appointment_id,
       apa.prisoner_number AS prisoner_number,
       ap.category_code AS category_code,
       ap.start_date AS start_date,
       ap.start_time AS start_time,
       ap.end_time AS end_time,
       aps.extra_information AS extra_information,
       case
           when apa.attended = true then 'Yes'
           when apa.attended = false then 'No'
           else 'Unmarked'
       end AS attended,
       ap.created_time as created_date,
       aps.prisoner_extra_information AS prisoner_extra_information,
       ap.custom_name AS custom_name,
       eo.description AS organiser,
       ap.dps_location_id AS dps_location_id,
       ap.in_cell AS in_cell,
       ap.off_wing AS off_wing,
       ap.on_wing AS on_wing,
       cr.description AS cancellation_reason,
       ap.cancelled_by AS cancelled_by
  FROM appointment ap
  JOIN appointment_series aps ON aps.appointment_series_id = ap.appointment_series_id
  JOIN appointment_attendee apa ON apa.appointment_id = ap.appointment_id
  LEFT JOIN event_organiser eo on eo.event_organiser_id =  ap.appointment_organiser_id
  LEFT JOIN appointment_cancellation_reason cr on cr.appointment_cancellation_reason_id = ap.cancellation_reason_id
  WHERE apa.is_deleted = false;
