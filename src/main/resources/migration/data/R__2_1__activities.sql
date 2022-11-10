insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, summary, description, start_date, end_date, created_time, created_by)
values (1, 'MDI', 1, 1, 'Maths level 1', 'A basic maths course suitable for introduction to the subject', '2022-10-10', null, '2022-10-10 09:00:00', 'SEED USER'),
       (2, 'MDI', 1, 1, 'English level 1', 'A basic english course suitable for introduction to the subject', '2022-10-10', null, '2022-10-10 09:00:00', 'SEED USER'),
       (3, 'MDI', 2, 1, 'Wing cleaning', 'Cleaning and upkeep of the wing', '2022-10-10', null, '2022-10-10 09:00:00', 'SEED USER'),
       (4, 'MDI', 3, 1, 'Gym', 'Time for exercise', '2022-10-10', null, '2022-10-10 09:00:00', 'SEED USER');

alter sequence activity_activity_id_seq restart with 5
