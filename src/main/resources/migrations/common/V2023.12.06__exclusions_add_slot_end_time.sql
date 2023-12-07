-- ==================================================================
-- Add slot_end_time column
-- ==================================================================
ALTER TABLE exclusion ADD COLUMN slot_end_time time;

-- ==================================================================
-- Populate the column with appropriate values
-- ==================================================================
-- update "active" exclusions first since we know for sure the slot it associates with
UPDATE exclusion ex SET slot_end_time = (
    SELECT acs.end_time
    FROM activity_schedule_slot acs
    WHERE acs.week_number = ex.week_number
    AND acs.start_time = ex.slot_start_time
)
WHERE end_date is null;

-- for "ended"/"historical" exclusions, the slot may have changed, so get the end_time from a scheduled_instance at that time
UPDATE exclusion ex SET slot_end_time = (
    SELECT DISTINCT si.end_time
    FROM scheduled_instance si
    JOIN activity_schedule _as ON si.activity_schedule_id = _as.activity_schedule_id
    WHERE ex.week_number = FLOOR(MOD(EXTRACT(DAY FROM (si.session_date - date_trunc('week', _as.start_date))), _as.schedule_weeks * 7) / 7) + 1
        AND ex.slot_start_time = si.start_time
        AND ex.start_date <= si.session_date
        AND (ex.end_date >= si.session_date)
)
WHERE end_date is not null;

-- ==================================================================
-- Make the new column mandatory
-- ==================================================================
ALTER TABLE exclusion ALTER COLUMN slot_end_time SET NOT NULL;

-- ==================================================================
-- Alter the view to also take slot_end_time into account
-- ==================================================================
CREATE OR REPLACE VIEW v_prisoner_scheduled_activities
AS
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
       act.on_wing,
       act.off_wing
FROM scheduled_instance si
         JOIN activity_schedule schedule
              ON schedule.activity_schedule_id = si.activity_schedule_id AND
                 si.session_date >= schedule.start_date AND
                 (schedule.end_date IS NULL OR schedule.end_date >= si.session_date)
         JOIN allocation alloc ON alloc.activity_schedule_id = si.activity_schedule_id AND
                                  si.session_date >= alloc.start_date AND
                                  (alloc.end_date IS NULL OR alloc.end_date >= si.session_date)
         JOIN activity act ON act.activity_id = schedule.activity_id AND
                              (act.end_date IS NULL OR act.end_date >= si.session_date)
         JOIN activity_category category ON category.activity_category_id = act.activity_category_id
         LEFT JOIN exclusion ex ON alloc.allocation_id = ex.allocation_id AND
                                   ex.start_date <= si.session_date AND
                                   (ex.end_date >= si.session_date OR ex.end_date IS NULL) AND
                                   ex.slot_start_time = si.start_time AND
                                   ex.slot_end_time = si.end_time AND
                                   ex.week_number = FLOOR(MOD(EXTRACT(DAY FROM (si.session_date - date_trunc('week', schedule.start_date))), schedule.schedule_weeks * 7) / 7) + 1
WHERE TO_CHAR(si.session_date, 'DY') = 'MON' AND ex.monday_flag IS NOT true
   OR TO_CHAR(si.session_date, 'DY') = 'TUE' AND ex.tuesday_flag IS NOT true
   OR TO_CHAR(si.session_date, 'DY') = 'WED' AND ex.wednesday_flag IS NOT true
   OR TO_CHAR(si.session_date, 'DY') = 'THU' AND ex.thursday_flag IS NOT true
   OR TO_CHAR(si.session_date, 'DY') = 'FRI' AND ex.friday_flag IS NOT true
   OR TO_CHAR(si.session_date, 'DY') = 'SAT' AND ex.saturday_flag IS NOT true
   OR TO_CHAR(si.session_date, 'DY') = 'SUN' AND ex.sunday_flag IS NOT true;
