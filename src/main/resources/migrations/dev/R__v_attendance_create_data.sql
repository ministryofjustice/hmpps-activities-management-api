CREATE OR REPLACE VIEW v_attendance_create
AS SELECT gen_random_uuid() as id, data.*
   FROM (SELECT DISTINCT si.scheduled_instance_id,
                         si.session_date,
                         si.time_slot,
                         alloc.allocation_id,
                         alloc.prisoner_number,
                         alloc.prisoner_status,
                         alloc.prison_pay_band_id,
                         alloc.start_date as alloc_start,
                         alloc.end_date   as alloc_end,
                         as1.start_date   as schedule_start,
                         as1.end_date     as schedule_end,
                         as1.schedule_weeks,
                         si.cancelled_reason,
                         si.cancelled_by,
                         case
                             when e.exclusion_id is not null then true
                             else false
                             end          as possible_exclusion,
                         a.prison_code,
                         a.paid,
                         a.activity_id
         FROM scheduled_instance si
                  join
              activity_schedule as1
              on as1.activity_schedule_id = si.activity_schedule_id
                  join
              activity a
              on a.activity_id = as1.activity_id
                  join
              allocation alloc
              on as1.activity_schedule_id = alloc.activity_schedule_id
                  left join
              exclusion e
              on alloc.allocation_id = e.allocation_id
         WHERE alloc.prisoner_status <> 'ENDED') as data