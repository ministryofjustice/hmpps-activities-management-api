create or replace view v_activity_summary
AS
select a.activity_id                                           id,
       a.activity_category_id,
       a.prison_code,
       a.description                                           activity_name,
       s.capacity,
       count(alloc.allocation_id)                              allocated,
       (select count(*)
        from waiting_list w
        where w.activity_id = a.activity_id
          and (w.status = 'PENDING' OR w.status = 'APPROVED')) waitlisted,
       a.created_time,
       CASE
           WHEN a.end_date < current_timestamp THEN 'ARCHIVED'
           ELSE 'LIVE'
           END AS                                              activity_state
from activity a
         join activity_schedule s on a.activity_id = s.activity_id
         left join allocation alloc on s.activity_schedule_id = alloc.activity_schedule_id and alloc.prisoner_status != 'ENDED'
group by a.activity_id, a.description, a.prison_code, s.capacity
order by activity_name, a.activity_id;
