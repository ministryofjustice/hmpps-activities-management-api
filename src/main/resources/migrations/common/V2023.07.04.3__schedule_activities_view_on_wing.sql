CREATE OR REPLACE VIEW v_prisoner_scheduled_activities as
SELECT si.scheduled_instance_id,
       alloc.allocation_id,
       act.prison_code,
       si.session_date,
       si.start_time,
       si.end_time,
       alloc.prisoner_number,
       alloc.booking_id,
       schedule.internal_location_id,
       schedule.internal_location_code,
       schedule.internal_location_description,
       schedule.description AS schedule_description,
       act.activity_id,
       category.code        AS activity_category,
       act.summary          AS activity_summary,
       si.cancelled,
       CASE
           WHEN alloc.prisoner_status = 'SUSPENDED' OR alloc.prisoner_status = 'AUTO_SUSPENDED'
               THEN true
           ELSE false
           END              AS suspended,
      act.in_cell,
      act.on_wing
FROM scheduled_instance si
         JOIN activity_schedule schedule
              ON schedule.activity_schedule_id = si.activity_schedule_id AND
                 si.session_date >= schedule.start_date AND
                 (schedule.end_date IS NULL OR schedule.end_date <= si.session_date)
         JOIN allocation alloc ON alloc.activity_schedule_id = si.activity_schedule_id AND
                                  si.session_date >= alloc.start_date AND
                                  (alloc.end_date IS NULL OR alloc.end_date <= si.session_date)
         JOIN activity act ON act.activity_id = schedule.activity_id AND
                              (act.end_date IS NULL OR act.end_date <= si.session_date)
         JOIN activity_category category
              ON category.activity_category_id = act.activity_category_id;
