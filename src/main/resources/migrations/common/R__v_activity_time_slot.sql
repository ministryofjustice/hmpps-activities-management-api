CREATE OR REPLACE VIEW v_activity_time_slot AS
SELECT scheduled_instance.scheduled_instance_id,
       'AM'::text AS time_slot,
       activity.prison_code,
       activity.activity_id,
       activity.summary,
       activity_category.name
FROM activity,
     activity_schedule,
     prison_regime,
     scheduled_instance,
     activity_category
WHERE activity.activity_id = activity_schedule.activity_id
  AND activity.prison_code::text = prison_regime.prison_code::text
  AND activity_schedule.activity_schedule_id = scheduled_instance.activity_schedule_id
  AND scheduled_instance.start_time < prison_regime.pm_start
  AND activity.activity_category_id = activity_category.activity_category_id
UNION
SELECT scheduled_instance.scheduled_instance_id,
       'PM'::text AS time_slot,
       activity.prison_code,
       activity.activity_id,
       activity.summary,
       activity_category.name
FROM activity,
     activity_schedule,
     prison_regime,
     scheduled_instance,
     activity_category
WHERE activity.activity_id = activity_schedule.activity_id
  AND activity.prison_code::text = prison_regime.prison_code::text
  AND activity_schedule.activity_schedule_id = scheduled_instance.activity_schedule_id
  AND scheduled_instance.start_time >= prison_regime.pm_start
  AND scheduled_instance.start_time < prison_regime.ed_start
  AND activity.activity_category_id = activity_category.activity_category_id
UNION
SELECT scheduled_instance.scheduled_instance_id,
       'ED'::text AS time_slot,
       activity.prison_code,
       activity.activity_id,
       activity.summary,
       activity_category.name
FROM activity,
     activity_schedule,
     prison_regime,
     scheduled_instance,
     activity_category
WHERE activity.activity_id = activity_schedule.activity_id
  AND activity.prison_code::text = prison_regime.prison_code::text
  AND activity_schedule.activity_schedule_id = scheduled_instance.activity_schedule_id
  AND scheduled_instance.start_time >= prison_regime.ed_start
  AND activity.activity_category_id = activity_category.activity_category_id;
