insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, summary, description, in_cell, start_date, risk_level, created_time, created_by, paid)
values (1, 'RSI', 1, 2, 'Maths', 'Maths', false,'2022-10-10', 'high', '2022-9-21 00:00:00', 'SEED USER', true),
       (2, 'RSI', 1, 2, 'English', 'English', true,'2022-10-10', 'high', '2022-9-21 00:00:00', 'SEED USER', true),
       (3, 'RSI', 1, 2, 'Woodwork', 'Woodwork', false,'2022-10-10', 'high', '2022-9-21 00:00:00', 'SEED USER', true),
       (4, 'RSI', 1, 2, 'Yoga', 'Yoga', false,'2022-10-10', 'high', '2022-9-21 00:00:00', 'SEED USER', true),
       (5, 'RSI', 1, 2, 'Needlework', 'Needlework', false,'2022-10-10', 'high', '2022-9-21 00:00:00', 'SEED USER', true),
       (6, 'RSI', 1, 2, 'Pottery', 'Pottery', false,'2022-10-10', 'high', '2022-9-21 00:00:00', 'SEED USER', true),
       (7, 'RSI', 1, 2, 'Origami', 'Origami', false,'2022-10-10', 'high', '2022-9-21 00:00:00', 'SEED USER', true),
       (8, 'RSI', 1, 2, 'History', 'History', false,'2022-10-10', 'high', '2022-9-21 00:00:00', 'SEED USER', true);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, dps_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (1, 1, 'Maths Level 1', 1, null,'L1', 'Location 1', 10, '2022-10-10'),
       (2, 2, 'English', null, null, null,  null,10, '2022-10-10'),
       (3, 3, 'Woodwork', 2, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Old Location Code 2', 'Old Location 1', 10, '2022-10-10'),
       (4, 4, 'Yoga', 2, null, 'L2222', 'Location 2222', 10, '2022-10-10'),
       (5, 5, 'Needlework', 3, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Old Location Code 3', 'Old Location 3', 10, '2022-10-10'),
       (6, 6, 'Pottery', 4, null, 'Old Location Code 4', 'Old Location 4', 10, '2022-10-10'),
       (7, 7, 'Origami', 5, null, 'Old Location Code 5', 'Old Location 5', 10, '2022-10-10'),
       (8, 8, 'History', 6, null, 'Old Location Code 6', 'Old Location 6', 10, '2022-10-10');





