-- Tuesday morning allocation(s)
insert into allocation(allocation_id, activity_schedule_id, prisoner_number, iep_level, pay_band, start_date, end_date, active, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (3, 5, 'G4793VF', 'BAS', 'A', '2022-10-10', null, true, '2022-10-10 09:30:00', 'MR BLOGS', null, null, null);

-- Tuesday afternoon allocation(s)
insert into allocation(allocation_id, activity_schedule_id, prisoner_number, iep_level, pay_band, start_date, end_date, active, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (4, 6, 'G4793VF', 'BAS', 'A', '2022-10-10', null, true, '2022-10-10 09:30:00', 'MR BLOGS', null, null, null);

-- Thursday morning allocation(s)
insert into allocation(allocation_id, activity_schedule_id, prisoner_number, iep_level, pay_band, start_date, end_date, active, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (5, 7, 'G4793VF', 'BAS', 'A', '2022-10-10', null, true, '2022-10-10 09:30:00', 'MR BLOGS', null, null, null);

-- Thursday afternoon allocation(s)
insert into allocation(allocation_id, activity_schedule_id, prisoner_number, iep_level, pay_band, start_date, end_date, active, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (6, 8, 'G4793VF', 'BAS', 'A', '2022-10-10', null, true, '2022-10-10 09:30:00', 'MR BLOGS', null, null, null);

alter sequence allocation_allocation_id_seq restart with 7;