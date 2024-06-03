--Common
truncate table job restart identity cascade;
truncate table event_tier restart identity cascade;
truncate table event_organiser restart identity cascade;
--
-- --Activities
truncate table activity_category restart identity cascade;
truncate table eligibility_rule restart identity cascade;
truncate table rollout_prison restart identity cascade;
truncate table attendance restart identity cascade;
truncate table attendance_reason restart identity cascade;
truncate table scheduled_instance restart identity cascade;
truncate table activity_schedule_suspension restart identity cascade;
truncate table allocation restart identity cascade;
truncate table planned_deallocation restart identity cascade;
truncate table activity_pay restart identity cascade;
truncate table activity_minimum_education_level restart identity cascade;
truncate table activity_schedule restart identity cascade;
truncate table activity_schedule_slot restart identity cascade;
truncate table activity_eligibility restart identity cascade;
truncate table eligibility_rule restart identity cascade;
truncate table activity restart identity cascade;
truncate table exclusion restart identity cascade;
truncate table prison_pay_band restart identity cascade;
truncate table prison_regime restart identity cascade;
truncate table event_review restart identity cascade;
truncate table attendance_history restart identity cascade;
truncate table waiting_list restart identity cascade;
truncate table local_audit restart identity cascade;
truncate table planned_suspension restart identity cascade;

--Appointments
truncate table appointment_attendee restart identity cascade;
truncate table appointment restart identity cascade;
truncate table appointment_series_schedule restart identity cascade;
truncate table appointment_series restart identity cascade;
truncate table appointment_set_appointment_series restart identity cascade;
truncate table appointment_set restart identity cascade;
