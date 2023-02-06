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
truncate table activity_pay restart identity;
truncate table prisoner_waiting restart identity;
truncate table activity_schedule restart identity;
truncate table activity_schedule_slot restart identity;
truncate table activity_eligibility restart identity;
truncate table eligibility_rule restart identity;
truncate table activity restart identity;
truncate table prison_pay_band restart identity;

--Appointments
truncate table appointment_instance;
truncate table appointment_occurrence_allocation;
truncate table appointment_occurrence;
truncate table appointment_schedule;
truncate table appointment_category;
truncate table appointment;

SET REFERENTIAL_INTEGRITY TRUE;
