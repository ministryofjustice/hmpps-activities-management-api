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
       ap.created_time as created_date
  FROM appointment ap
  JOIN appointment_series aps ON aps.appointment_series_id = ap.appointment_series_id
  JOIN appointment_attendee apa ON apa.appointment_id = ap.appointment_id;