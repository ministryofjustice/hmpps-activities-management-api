insert into activity_category(activity_category_id, code, name, description)
values (1, 'SAA_EDUCATION', 'Education', 'Such as classes in English, maths, construction or barbering'),
       (2, 'SAA_INDUSTRIES', 'Industries', 'Such as work like recycling, packing or assembly operated by the prison, external firms or charities'),
       (3, 'SAA_PRISON_JOBS', 'Prison jobs', 'Such as kitchen, cleaning, gardens or other maintenance and services to keep the prison running'),
       (4, 'SAA_GYM_SPORTS_FITNESS', 'Gym, sport, fitness', 'Such as sports clubs, like football, or recreational gym sessions'),
       (5, 'SAA_INDUCTION', 'Induction', 'Such as gym induction, education assessments or health and safety workshops'),
       (6, 'SAA_INTERVENTIONS', 'Intervention programmes', 'Such as programmes for behaviour management, drug and alcohol misuse and community rehabilitation'),
       (7, 'SAA_FAITH_SPIRITUALITY', 'Faith and spirituality', 'Such as chapel, prayer meetings or meditation'),
       (8, 'SAA_NOT_IN_WORK', 'Not in work', 'Such as unemployed, retired, long-term sick, or on remand'),
       (9, 'SAA_OTHER', 'Other', 'Select if the activity you’re creating doesn’t fit any other category');

alter sequence activity_category_activity_category_id_seq restart with 10;
