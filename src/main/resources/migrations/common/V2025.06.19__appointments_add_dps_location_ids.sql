alter table appointment_series add column dps_location_id uuid;
create index idx_appointment_series_dps_location_id ON appointment_series (dps_location_id);

alter table appointment_set add column dps_location_id uuid;
create index idx_appointment_set_dps_location_id ON appointment_set (dps_location_id);

alter table appointment add column dps_location_id uuid;
create index idx_appointment_dps_location_id ON appointment (dps_location_id);

