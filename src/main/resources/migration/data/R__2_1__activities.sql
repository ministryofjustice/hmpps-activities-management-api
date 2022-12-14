insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, minimum_incentive_level, created_time, created_by)
values (1, 'MDI', 1, 1, true, false, false, false, 'H', 'Maths level 1', 'A basic maths course suitable for introduction to the subject', '2022-10-10', null, null, null, '2022-10-10 09:00:00', 'SEED USER'),
       (2, 'MDI', 1, 1, true, false, false, false, 'H', 'English level 1', 'A basic english course suitable for introduction to the subject', '2022-10-10', null, null, null, '2022-10-10 09:00:00', 'SEED USER'),
       (3, 'MDI', 3, 1, true, false, false, false, 'H', 'Wing cleaning', 'Cleaning and upkeep of the wing', '2022-10-10', null, null, null, '2022-10-10 09:00:00', 'SEED USER'),
       (4, 'MDI', 4, 1, false, false, false, false, 'H', 'Gym', 'Time for exercise', '2022-10-10', null, null, null, '2022-10-10 09:00:00', 'SEED USER');

alter sequence activity_activity_id_seq restart with 5
