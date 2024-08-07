CREATE OR REPLACE VIEW v_prisoner_scheduled_activities
AS
SELECT DISTINCT si.scheduled_instance_id,
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
       schedule.description                                                                  AS schedule_description,
       act.activity_id,
       category.code                                                                         AS activity_category,
       act.summary                                                                           AS activity_summary,
       si.cancelled,
       EXISTS (SELECT 1
               FROM planned_suspension ps
               WHERE alloc.allocation_id = ps.allocation_id
                 AND ps.planned_start_date <= si.session_date
                 AND (ps.planned_end_date > si.session_date OR ps.planned_end_date IS NULL)) AS suspended,
       act.in_cell,
       act.on_wing,
       act.off_wing,
       EXISTS (SELECT 1 WHERE alloc.prisoner_status = 'AUTO_SUSPENDED')                      AS auto_suspended
FROM scheduled_instance si
         JOIN activity_schedule schedule
              ON schedule.activity_schedule_id = si.activity_schedule_id AND
                 si.session_date >= schedule.start_date AND
                 (schedule.end_date IS NULL OR schedule.end_date >= si.session_date)
         JOIN allocation alloc ON alloc.activity_schedule_id = si.activity_schedule_id AND
                                  si.session_date >= alloc.start_date AND
                                  (alloc.end_date IS NULL OR alloc.end_date >= si.session_date) AND
                                  (alloc.deallocated_time IS NULL OR alloc.deallocated_time >= (si.session_date + si.start_time))
         JOIN activity act ON act.activity_id = schedule.activity_id AND
                              (act.end_date IS NULL OR act.end_date >= si.session_date)
         JOIN activity_category category ON category.activity_category_id = act.activity_category_id
         LEFT JOIN exclusion ex ON alloc.allocation_id = ex.allocation_id AND
                                   ex.start_date <= si.session_date AND
                                   (ex.end_date >= si.session_date OR ex.end_date IS NULL) AND
                                   ex.time_slot = si.time_slot and
                                   ex.week_number = FLOOR(MOD(EXTRACT(DAY FROM (si.session_date - date_trunc('week', schedule.start_date))), schedule.schedule_weeks * 7) / 7) + 1
                                   and TO_CHAR(si.session_date, 'DAY') in (
                                   select edw.day_of_week
                                   from exclusion_days_of_week edw
                                   where edw.exclusion_id = ex.exclusion_id);