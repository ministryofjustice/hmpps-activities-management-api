insert into appointment_category(appointment_category_id, code, description, appointment_parent_category_id, status)
values (83, 'MAAS', 'Medical - Aortic aneurysm screening', 6, 'ACTIVE'),
       (84, 'MBS', 'Medical - Breast screening', 6, 'ACTIVE'),
       (85, 'MCS', 'Medical - Cervical screening', 6, 'ACTIVE'),
       (86, 'MDES', 'Medical - Diabetic eye screening', 6, 'ACTIVE'),
       (87, 'MFV', 'Medical - Flu vaccine', 6, 'ACTIVE'),
       (88, 'MSUMA', 'Medical - Substance use mutual aid', 6, 'ACTIVE'),
       (89, 'MSUS', 'Medical - Substance use support', 6, 'ACTIVE');

alter sequence if exists appointment_category_appointment_category_id_seq restart with 90;