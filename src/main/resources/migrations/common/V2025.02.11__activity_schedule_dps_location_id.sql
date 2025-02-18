alter table activity_schedule add column dps_location_id uuid;

create index idx_activity_schedule_dps_location_id ON activity_schedule (dps_location_id);