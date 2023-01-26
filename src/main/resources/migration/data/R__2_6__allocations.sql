insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (1, 1, 'G4793VF', 1200993, 1, '2022-10-10', null, '2022-10-10 09:30:00', 'MR BLOGS', null, null, null),
       (2, 5, 'G4793VF', 1200993, 1, '2022-10-10', null, '2022-10-10 09:30:00', 'MR BLOGS', null, null, null),
       (3, 2, 'A9477DY', 1203218, 1, '2022-10-10', null, '2022-10-10 09:30:00', 'MR BLOGS', null, null, null),
       (4, 6, 'A9477DY', 1203218, 1, '2022-10-10', null, '2022-10-10 09:30:00', 'MR BLOGS', null, null, null),
       (5, 9, 'A5193DY', 1200982, 1, '2022-10-10', null, '2022-10-10 09:30:00', 'MR BLOGS', null, null, null),
       (6, 11, 'A5193DY', 1200982, 1, '2022-10-10', null, '2022-10-10 09:30:00', 'MR BLOGS', null, null, null);

alter sequence allocation_allocation_id_seq restart with 7;
