insert into activity_category(activity_category_id, code, name, description)
values (1, 'SAA_EDUCATION', 'Education', 'Such as classes in English, maths, construction and computer skills'),
       (2, 'SAA_INDUSTRIES', 'Industries', 'Such as work in the prison and with employers and charities'),
       (3, 'SAA_SERVICES', 'Services', 'Such as work in the kitchens and laundry, cleaning, gardening, and mentoring'),
       (4, 'SAA_GYM_SPORTS_FITNESS', 'Gym, sport and fitness', 'Such as sport clubs, like football, fitness classes and gym sessions'),
       (5, 'SAA_INDUCTION', 'Induction', 'Such as gym induction, education assessments, health and safety workshops'),
       (6, 'SAA_INTERVENTIONS', 'Intervention programmes', 'Such as programmes for behaviour management, drug and alcohol misuse and community rehabilitation'),
       (7, 'SAA_LEISURE_SOCIAL', 'Leisure and social', 'Such as association, library time and social clubs, like music or art');

alter sequence activity_category_activity_category_id_seq restart with 8;
