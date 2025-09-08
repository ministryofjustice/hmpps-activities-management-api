-- =============================================
-- APPOINTMENT PARENT CATEGORIES
-- =============================================

CREATE TABLE appointment_parent_category
(
    appointment_parent_category_id  bigserial    NOT NULL CONSTRAINT appointment_parent_category_pk PRIMARY KEY,
    name                            varchar(100) NOT NULL UNIQUE,
    description                     varchar(300)
);

insert into appointment_parent_category(appointment_parent_category_id, name, description)
values (1, 'Adjudications', 'Adjudications'),
       (2, 'Court', 'Court'),
       (3, 'Education', 'Education'),
       (4, 'Faith and spirituality', 'Faith and spirituality'),
       (5, 'Gym, sport, fitness', 'Gym, sport, fitness'),
       (6, 'Health', 'Health'),
       (7, 'Induction', 'Induction'),
       (8, 'Interventions', 'Interventions'),
       (9, 'Other', 'Other'),
       (10, 'Prisoner Forum', 'Prisoner Forum'),
       (11, 'Resettlement', 'Resettlement'),
       (12, 'Sentence management', 'Sentence management'),
       (13, 'Visits Official Video', 'Visits Official Video'),
       (14, 'Visits social', 'Visits social'),
       (15, 'Visits Social Video', 'Visits Social Video');

alter sequence if exists appointment_parent_category_appointment_parent_category_id_seq restart with 16;

-- =============================================
-- APPOINTMENT CATEGORIES
-- =============================================

CREATE TABLE appointment_category
(
    appointment_category_id         bigserial       NOT NULL CONSTRAINT appointment_category_pk PRIMARY KEY,
    code                            varchar(30)     NOT NULL UNIQUE,
    description                     varchar(300),
    appointment_parent_category_id  bigint          NOT NULL REFERENCES appointment_parent_category (appointment_parent_category_id),
    status                          varchar(30)
);

insert into appointment_category(appointment_category_id, code, description, appointment_parent_category_id, status)
values (1, 'OIC', 'Adjudication Hearing', 1, 'ACTIVE'),
       (2, 'VLB', 'Video Link - Court Hearing', 2, 'ACTIVE'),
       (3, 'IMM', 'Immigration Appointment', 2, 'ACTIVE'),
       (4, 'CALA', 'Case - Legal Aid', 2, 'ACTIVE'),
       (5, 'CABA', 'Case - Bail Apps', 2, 'ACTIVE'),
       (6, 'EDUC', 'Education', 3, 'ACTIVE'),
       (7, 'LIBR', 'Library', 3, 'ACTIVE'),
       (8, 'SHT', 'Shannon Trust Appointment', 3, 'ACTIVE'),
       (9, 'EXAM', 'Exams', 3, 'ACTIVE'),
       (10, 'TOE', 'Toe by Toe Appointment', 3, 'ACTIVE'),
       (11, 'PRACT', 'Practicals', 3, 'ACTIVE'),
       (12, 'CHAP', 'Chaplaincy', 4, 'ACTIVE'),
       (13, 'GYMW', 'Gym - Weights', 5, 'ACTIVE'),
       (14, 'GYMFS', 'Gym - Fitness Suite', 5, 'ACTIVE'),
       (15, 'GYMSH', 'Gym - Sports Halls Activity', 5, 'ACTIVE'),
       (16, 'GYMRP', 'Gym - Remedial Physiotherapy', 5, 'ACTIVE'),
       (17, 'GYMF', 'Gym - Football', 5, 'ACTIVE'),
       (18, 'GYMWH', 'Gym - Walking to Health', 5, 'ACTIVE'),
       (19, 'MEOT', 'Medical - Other', 6, 'ACTIVE'),
       (20, 'MEDO', 'Medical - Doctor', 6, 'ACTIVE'),
       (21, 'MEDE', 'Medical - Dentist', 6, 'ACTIVE'),
       (22, 'MEPS', 'Medical - Psychology Services', 6, 'ACTIVE'),
       (23, 'FOPS', 'Psychology Services - Forensic', 6, 'ACTIVE'),
       (24, 'MEOP', 'Medical - Optician', 6, 'ACTIVE'),
       (25, 'PA', 'Prison Activities', 6, 'ACTIVE'),
       (26, 'PROG_SESS', 'Programme Session', 6, 'ACTIVE'),
       (27, 'VISIT', 'Visits', 6, 'INACTIVE'),
       (28, 'MPHY', 'Medical - Physiotherapy', 6, 'ACTIVE'),
       (29, 'CSS', 'Counselling Session', 6, 'ACTIVE'),
       (30, 'MLTC', 'Medical - Long Term Conditions', 6, 'ACTIVE'),
       (31, 'MPSY', 'Medical - Psychiatry', 6, 'ACTIVE'),
       (32, 'MPOD', 'Medical - Podiatry', 6, 'ACTIVE'),
       (33, 'MEXR', 'Medical - X-ray', 6, 'ACTIVE'),
       (34, 'MGUM', 'Medical - GUM', 6, 'ACTIVE'),
       (35, 'PHARMACY', 'Medical - Pharmacy Attendance', 6, 'ACTIVE'),
       (36, 'IND', 'Induction Meeting', 7, 'ACTIVE'),
       (37, 'INTERV', 'Interventions', 8, 'ACTIVE'),
       (38, 'PROGRAM', 'Programmes Appointment', 8, 'ACTIVE'),
       (39, 'BBR', 'Building Better Relationships', 8, 'ACTIVE'),
       (40, 'SOTP', 'SOTP Appointment', 8, 'ACTIVE'),
       (41, 'TSP', 'TSP Appointment', 8, 'ACTIVE'),
       (42, 'REOT', 'Rec - Other', 9, 'ACTIVE'),
       (43, 'REPR', 'Rec - Prop', 9, 'ACTIVE'),
       (44, 'REPH', 'Rec - Photo', 9, 'ACTIVE'),
       (45, 'ACTI', 'Activities', 9, 'ACTIVE'),
       (46, 'OTHE', 'Other', 9, 'ACTIVE'),
       (47, 'BARHAI', 'Barbers / Haircut', 9, 'ACTIVE'),
       (48, 'CANT', 'Canteen', 9, 'ACTIVE'),
       (49, 'KWA', 'Key Worker Activity', 9, 'ACTIVE'),
       (50, 'RAPT', 'Case - RAPT', 9, 'ACTIVE'),
       (51, 'FOCUS', 'Focus Groups', 10, 'ACTIVE'),
       (52, 'EQUAL', 'Equality Meetings', 10, 'ACTIVE'),
       (53, 'LACO', 'Labour Control', 10, 'ACTIVE'),
       (54, 'AGE', 'Age UK Meetings', 10, 'ACTIVE'),
       (55, 'GOVE', 'Governor', 10, 'ACTIVE'),
       (56, 'STUCOU', 'Student Council', 10, 'ACTIVE'),
       (57, 'USV', 'User Voice Session', 10, 'ACTIVE'),
       (58, 'IMB', 'IMB', 10, 'ACTIVE'),
       (59, 'EMPHUB', 'Employment Hub', 11, 'ACTIVE'),
       (60, 'RST', 'Reset Appointment', 11, 'ACTIVE'),
       (61, 'CAREER', 'Careers Adviser Appointment', 11, 'ACTIVE'),
       (62, 'JCP', 'Job Centre Plus', 11, 'ACTIVE'),
       (63, 'PRM', 'Pre-Release Meeting', 11, 'ACTIVE'),
       (64, 'FID', 'Finance and ID', 11, 'ACTIVE'),
       (65, 'SBD', 'Storybook Dad''s', 11, 'ACTIVE'),
       (66, 'CAHO', 'Case - Housing', 11, 'ACTIVE'),
       (67, 'CAPR', 'Case - Probation', 11, 'ACTIVE'),
       (68, 'CABE', 'Case - Benefits', 11, 'ACTIVE'),
       (69, 'OMU', 'Offender Management Unit', 12, 'ACTIVE'),
       (70, 'SMS', 'Case - SMS', 12, 'ACTIVE'),
       (71, 'CAOT', 'Case - Other', 12, 'ACTIVE'),
       (72, 'VLPA', 'Video Link - Parole Hearing', 12, 'ACTIVE'),
       (73, 'PAROLE', 'Parole Hearing', 12, 'ACTIVE'),
       (74, 'ROTL', 'ROTL Board', 12, 'ACTIVE'),
       (75, 'VLLA', 'Video Link - Legal Appointment', 13, 'ACTIVE'),
       (76, 'VLPM', 'Video Link - Probation Meeting', 13, 'ACTIVE'),
       (77, 'VLOO', 'Video Link - Official Other', 13, 'ACTIVE'),
       (78, 'SOLS', 'Solicitor Meeting (Not Legal Visit)', 13, 'ACTIVE'),
       (79, 'CHILD', 'Children and Families', 14, 'ACTIVE'),
       (80, 'SVC', 'Social video call', 15, 'ACTIVE'),
       (81, 'VLAP', 'Video Link to Another Prison', 15, 'ACTIVE'),
       (82, 'GYMPE', 'Gym - PE Course', 5, 'INACTIVE');

alter sequence if exists appointment_category_appointment_category_id_seq restart with 83;
