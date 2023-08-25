CREATE OR REPLACE VIEW v_activity_summary
AS
select a.activity_id                                           id,
       a.activity_category_id,
       a.prison_code,
       a.description                                           activity_name,
       s.capacity,
       (select count(*)
        from allocation alloc
        where alloc.activity_schedule_id = a.activity_id
          and alloc.prisoner_status != 'ENDED')                allocated,
       (select count(*)
        from waiting_list w
        where w.activity_schedule_id = a.activity_id
          and (w.status = 'PENDING' OR w.status = 'APPROVED')) waitlisted,
       a.created_time,
       CASE
           WHEN a.end_date < current_timestamp THEN 'ARCHIVED'
           ELSE 'LIVE'
           END AS                                              activity_state
from activity a
         join activity_schedule s on a.activity_id = s.activity_id
         left join allocation alloc on s.activity_schedule_id = alloc.activity_schedule_id
group by a.activity_id, a.description, a.prison_code, s.capacity
order by activity_name
