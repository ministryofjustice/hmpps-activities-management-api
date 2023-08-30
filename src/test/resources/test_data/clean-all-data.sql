SET REFERENTIAL_INTEGRITY FALSE;

--Activities
truncate table activity_category restart identity;
truncate table activity_tier restart identity;
truncate table eligibility_rule restart identity;
truncate table rollout_prison restart identity;
truncate table attendance restart identity;
truncate table attendance_reason restart identity;
truncate table scheduled_instance restart identity;
truncate table activity_schedule_suspension restart identity;
truncate table allocation restart identity;
truncate table planned_deallocation restart identity;
truncate table activity_pay restart identity;
truncate table activity_minimum_education_level restart identity;
truncate table activity_schedule restart identity;
truncate table activity_schedule_slot restart identity;
truncate table activity_eligibility restart identity;
truncate table eligibility_rule restart identity;
truncate table activity restart identity;
truncate table prison_pay_band restart identity;
truncate table prison_regime restart identity;
truncate table event_review restart identity;
truncate table attendance_history restart identity;
truncate table waiting_list restart identity;
truncate table local_audit restart identity;

--Appointments
truncate table appointment_cancellation_reason restart identity;
truncate table appointment_occurrence_allocation restart identity;
truncate table appointment_occurrence restart identity;
truncate table appointment_schedule restart identity;
truncate table appointment restart identity;
truncate table bulk_appointment_appointment restart identity;
truncate table bulk_appointment restart identity;

SET REFERENTIAL_INTEGRITY TRUE;
