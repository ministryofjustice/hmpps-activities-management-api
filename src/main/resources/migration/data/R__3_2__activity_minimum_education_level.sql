insert into activity_minimum_education_level(activity_minimum_education_level_id, activity_id, education_level_code, education_level_description)
values (1, 1, '1', 'Reading Measure 1.0'),
       (2, 1, '1.1', 'Reading Measure 1.1');

alter sequence activity_minimum_education_le_activity_minimum_education_le_seq restart with 3;
