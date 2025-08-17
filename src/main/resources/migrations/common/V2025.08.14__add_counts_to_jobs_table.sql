alter table job add column if not exists total_sub_tasks smallint;
alter table job add column if not exists completed_sub_tasks smallint;