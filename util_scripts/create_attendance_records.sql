--
-- Script for creating attendance records.
--
-- It requires an activity, activity_schedule, allocation(s) and scheduled_instance to be present for the attendance
-- records to be created.
--
-- Running this script will look for (missing) attendances for the current date and insert them.
--
insert into attendance (scheduled_instance_id, prisoner_number, posted, status)
select sch_ins.scheduled_instance_id, allo.prisoner_number, false, 'SCH'
from scheduled_instance sch_ins
       join activity_schedule act_sch on act_sch.activity_schedule_id = sch_ins.activity_schedule_id
       join allocation allo           on allo.activity_schedule_id = act_sch.activity_schedule_id
       join activity act              on act.activity_id = act_sch.activity_id
where sch_ins.session_date = current_date
  and not exists (select 1
                    from attendance a
                   where a.scheduled_instance_id = sch_ins.scheduled_instance_id
                     and a.prisoner_number = allo.prisoner_number);
