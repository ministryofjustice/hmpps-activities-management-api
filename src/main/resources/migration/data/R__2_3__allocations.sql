insert into allocation(allocation_id, activity_schedule_id, prisoner_number, incentive_level, pay_band, start_date, end_date, active, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (1, 1, 'G4793VF', 'BAS', 'A', '2022-10-10', null, true, '2022-10-10 09:30:00', 'MR BLOGS', null, null, null),
       (2, 3, 'G4793VF', 'BAS', 'A', '2022-10-10', null, true, '2022-10-10 09:30:00', 'MR BLOGS', null, null, null),
       (3, 5, 'G4793VF', 'BAS', 'A', '2022-10-10', null, true, '2022-10-10 09:30:00', 'MR BLOGS', null, null, null),
       (4, 6, 'G4793VF', 'BAS', 'A', '2022-10-10', null, true, '2022-10-10 09:30:00', 'MR BLOGS', null, null, null),
       (5, 7, 'G4793VF', 'BAS', 'A', '2022-10-10', null, true, '2022-10-10 09:30:00', 'MR BLOGS', null, null, null),
       (6, 8, 'G4793VF', 'BAS', 'A', '2022-10-10', null, true, '2022-10-10 09:30:00', 'MR BLOGS', null, null, null),
       (7, 9, 'G4793VF', 'BAS', 'A', '2022-10-10', null, true, '2022-10-10 09:30:00', 'MR BLOGS', null, null, null),
       (8, 11, 'G4793VF', 'BAS', 'A', '2022-10-10', null, true, '2022-10-10 09:30:00', 'MR BLOGS', null, null, null),
       (9, 12, 'G4793VF', 'BAS', 'A', '2022-10-10', null, true, '2022-10-10 09:30:00', 'MR BLOGS', null, null, null);

alter sequence allocation_allocation_id_seq restart with 10;
