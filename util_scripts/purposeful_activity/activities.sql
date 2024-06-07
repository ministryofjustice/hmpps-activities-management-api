-- This query will identify the activities data relevant for purposeful activity reporting over a two week period
-- The weeks are considered to begin start of day sunday, and end 1 second to midnight on the saturday.
-- A two week period is included so the purposeful activity reporting team can pick up any changes/amendments to activities made after the last report was taken

-- the integer param is a week offset. Default is 1 which means the report will run for the period up to the most recent Saturday. 
-- Increase the offset to go back in time. a value of 2 will generate the report for the two weeks up to the saturday before last.

PREPARE get_purposeful_activity_activities_for_prior_two_weeks (integer) AS
WITH date_range AS (
    SELECT
        (CURRENT_DATE - (EXTRACT(DOW FROM CURRENT_DATE)::integer + $1 * 7 + 7))::timestamp AS start_date,
        ((CURRENT_DATE - (EXTRACT(DOW FROM CURRENT_DATE)::integer + $1 * 7) + INTERVAL '23:59:59')::timestamp + INTERVAL '6 day') AS end_date
)
select 
act.activity_id as "activity.activity_id",
act.prison_code as "activity.prison_code",
act.activity_category_id as "activity.activity_category_id",
actcat.code as "activity_category.code",
actcat.name as "activity_category.name",
actcat.description as "activity_category.description",
act.activity_tier_id as "activity.activity_tier_id",
tier.code as "activity_tier.code",
tier.description as "activity_tier.description",
act.attendance_required as "activity.attendance_required",
act.in_cell as "activity.in_cell",
act.piece_work as "activity.piece_work",
act.outside_work as "activity.outside_work",
act.summary as "activity.summary",
act.description as "activity.description",
act.start_date as "activity.start_date",
act.end_date as "activity.end_date",
act.created_time as "activity.created_time",
act.updated_time as "activity.updated_time",
act.on_wing as "activity.on_wing",
act.off_wing as "activity.off_wing",
asch.activity_schedule_id as "activity_schedule.activity_schedule_id",
asch.description as "activity_schedule.description",
asch.start_date as "activity_schedule.start_date",
asch.end_date as "activity_schedule.end_date",
asch.updated_time as "activity_schedule.updated_time",
allo.allocation_id as "allocation.allocation_id",
allo.allocated_time as "allocation.allocated_time",
allo.deallocated_time as "allocation.deallocated_time",
allo.deallocated_reason as "allocation.deallocated_reason",
allo.suspended_time as "allocation.suspended_time",
allo.suspended_reason as "allocation.suspended_reason",
allo.planned_deallocation_id as "allocation.planned_deallocation_id",
pade.planned_date as "planned_deallocation.planned_date",
pade.planned_reason as "planned_deallocation.planned_reason",
att.attendance_id as "attendance.attendance_id",
att.prisoner_number as "attendance.prisoner_number",
att.attendance_reason_id as "attendance.attendance_reason_id",
atre.code as "attendance_reason.code",
atre.description as "attendance_reason.description",
atre.attended as "attendance_reason.attended",
att.recorded_time as "attendance.recorded_time",
att.status as "attendance.status",
att.pay_amount as "attendance.pay_amount",
att.bonus_amount as "attendance.bonus_amount",
att.pieces as "attendance.pieces",
att.issue_payment as "attendance.issue_payment"
from attendance att
inner join scheduled_instance si on att.scheduled_instance_id = si.scheduled_instance_id
	and (si.session_date || ' ' || si.start_time)::timestamp BETWEEN 
   	(SELECT start_date FROM date_range) AND 
   	(SELECT end_date FROM date_range)
inner join activity_schedule asch on asch.activity_schedule_id = si.activity_schedule_id
inner join activity act on act.activity_id = asch.activity_id
inner join activity_category actcat on actcat.activity_category_id = act.activity_category_id 
left outer join event_tier tier on tier.event_tier_id = act.activity_tier_id
left outer join allocation allo on allo.activity_schedule_id = asch.activity_schedule_id and allo.prisoner_number = att.prisoner_number
left outer join attendance_reason atre on atre.attendance_reason_id = att.attendance_reason_id
left outer join planned_deallocation pade on pade.allocation_id = allo.allocation_id
