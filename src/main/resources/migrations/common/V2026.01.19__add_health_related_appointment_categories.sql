insert into appointment_category(appointment_category_id, code, description, appointment_parent_category_id, status)
values (83, 'MGPNT', 'Medical - GP or nurse triage', 6, 'ACTIVE'),
       (84, 'MMC', 'Medical - Medication collection', 6, 'ACTIVE'),
       (85, 'MSUS', 'Medical - Substance use support', 6, 'ACTIVE'),
       (86, 'MSUMA', 'Medical - Substance use mutual aid', 6, 'ACTIVE'),
       (87, 'MDES', 'Medical - Diabetic eye screening', 6, 'ACTIVE'),
       (88, 'MAAS', 'Medical - Aortic aneurysm screening', 6, 'ACTIVE'),
       (89, 'MBS', 'Medical - Breast screening', 6, 'ACTIVE'),
       (90, 'MCS', 'Medical - Cervical screening', 6, 'ACTIVE'),
       (91, 'MFV', 'Medical - Flu vaccine', 6, 'ACTIVE'),
       (92, 'MMHS', 'Medical - Mental health service', 6, 'ACTIVE');

alter sequence if exists appointment_category_appointment_category_id_seq restart with 93;