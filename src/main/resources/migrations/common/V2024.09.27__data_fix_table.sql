CREATE TABLE data_fix
(
    data_fix_id bigserial,
    activity_schedule_id bigint,
    prisoner_number varchar(7),
    start_date date,
    prisoner_status varchar(30)
);