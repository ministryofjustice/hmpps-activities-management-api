create or replace view v_activity_summary
AS
select a.activity_id                                           id,
       a.activity_category_id,
       a.prison_code,
       a.description                                           activity_name,
       max(s.capacity)                                         capacity,
       count(distinct alloc.allocation_id)                     allocated,
       count(distinct w.waiting_list_id)                       waitlisted,
       a.created_time,
       CASE
           WHEN a.end_date < current_timestamp THEN
               'ARCHIVED'
           ELSE
               'LIVE'
           END AS                                              activity_state
from activity a
         join activity_schedule s on a.activity_id = s.activity_id
         left join allocation alloc on s.activity_schedule_id = alloc.activity_schedule_id and alloc.prisoner_status != 'ENDED'
         left join waiting_list w on w.activity_id = a.activity_id and (w.status::text = 'PENDING'::text or w.status::text = 'APPROVED'::text)
group by a.activity_id, a.description, a.prison_code
order by activity_name, a.activity_id;
