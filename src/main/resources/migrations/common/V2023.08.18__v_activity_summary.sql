create or replace function getStat(activityId bigint, stat varchar) returns int
    language plpgsql as
$$
declare
    result int;
begin
    IF stat = 'ALLOCATIONS' THEN
        select count(*)
        into result
        from allocation a
        where a.activity_schedule_id = activityId
          and a.prisoner_status != 'ENDED';
    ELSIF stat = 'WAITLIST' THEN
        select count(*)
        into result
        from waiting_list w
        where w.activity_schedule_id = activityId
          and (w.status = 'PENDING' OR w.status = 'APPROVED');
    END IF;
    return result;
end
$$;

CREATE OR REPLACE VIEW v_activity_summary
AS
select a.activity_id                         id,
       a.activity_category_id,
       a.prison_code,
       a.description                         activity_name,
       s.capacity,
       getStat(a.activity_id, 'ALLOCATIONS') allocated,
       getStat(a.activity_id, 'WAITLIST')    waitlisted,
       a.created_time,
       CASE
           WHEN a.end_date < current_timestamp THEN 'ARCHIVED'
           ELSE 'LIVE'
           END AS                            activity_state
from activity a
         join activity_schedule s on a.activity_id = s.activity_id
         join allocation alloc on s.activity_schedule_id = alloc.activity_schedule_id
group by a.activity_id, a.description, a.prison_code, s.capacity
order by activity_name
