--Individual appointments--
--Prisoner A1234BC, Category AC1, Location 123, Today 08:30-10:00, Created by TEST.USER
INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES (100, 'INDIVIDUAL', 'MDI', 'AC1', 1, 123, false, now()::date, '08:30', '10:00', now()::timestamp, 'TEST.USER');
INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES (200, 100, 1, 'MDI', 'AC1', 1, 123, false, now()::date, '08:30', '10:00', now()::timestamp, 'TEST.USER');
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (300, 200, 'A1234BC', 456);

--Prisoner B2345CD, Category AC2, Description Art Class, Location 456, Today 13:00-15:00, Created by DIFFERENT.USER
INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, custom_name, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES (101, 'INDIVIDUAL', 'MDI', 'AC2', 'Art Class', 1, 456, false, now()::date, '13:00', '15:00', now()::timestamp, 'DIFFERENT.USER');
INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES (201, 101, 1, 'MDI', 'AC2', 1, 456, false, now()::date, '13:00', '15:00', now()::timestamp, 'DIFFERENT.USER');
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (301, 201, 'B2345CD', 457);

--Prisoner A1234BC, Category AC3, In cell, One week from now 12:30-14:00, Created by TEST.USER
INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES (102, 'INDIVIDUAL', 'MDI', 'AC3', 1, null, true, now()::date, '12:30', '14:00', now()::timestamp, 'TEST.USER');
INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES (202, 102, 1, 'MDI', 'AC3', 1, null, true, now()::date, '12:30', '14:00', now()::timestamp, 'TEST.USER');
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (302, 202, 'A1234BC', 456);

--Prison OTH, Prisoner A1234BC, Category AC1, Location 789, Today 09:00-10:30, Created by OTHER.USER
INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES (103, 'INDIVIDUAL', 'OTH', 'AC1', 1, 789, false, now()::date, '09:00', '10:30', now()::timestamp, 'OTHER.USER');
INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES (203, 103, 1, 'OTH', 'AC1', 1, 789, false, now()::date, '09:00', '10:30', now()::timestamp, 'OTHER.USER');
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (303, 203, 'D4567EF', 459);

--Individual appointments--
--Prisoner A1234BC, Category AC1, Location 123, Today 08:30-10:00, Created by TEST.USER
INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES (104, 'INDIVIDUAL', 'MDI', 'AC1', 1, 123, false, now()::date, '21:00', '22:30', now()::timestamp, 'TEST.USER');
INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES (204, 104, 1, 'MDI', 'AC1', 1, 123, false, now()::date, '21:00', '22:30', now()::timestamp, 'TEST.USER');
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (304, 204, 'A1234BC', 456);

--Group appointments--
--Prisoners A1234BC and B2345CD, Category AC1, Location 123, Started one week ago 09:00-10:30, Repeating weekly 4 times, One edited, One cancelled, One deleted, Created by TEST.USER
INSERT INTO appointment_series_schedule (appointment_series_schedule_id, frequency, number_of_appointments)
VALUES (10, 'WEEKLY', 4);
INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, appointment_series_schedule_id, created_time, created_by)
VALUES (110, 'GROUP', 'MDI', 'AC1', 1, 123, false, now()::date, '09:00', '10:30', 10, now()::timestamp, 'TEST.USER');
INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES (210, 110, 1, 'MDI', 'AC1', 1, 123, false, now()::date , '09:00', '10:30', now()::timestamp, 'TEST.USER');
INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by, updated_time, updated_by)
VALUES (211, 110, 2, 'MDI', 'AC1', 1, 456, false, now()::date, '13:30', '15:00', now()::timestamp, 'TEST.USER', now()::timestamp, 'DIFFERENT.USER');
INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by, cancelled_time, cancellation_reason_id, cancelled_by)
VALUES (212, 110, 3, 'MDI', 'AC1', 1, 123, false, now()::date, '09:00', '10:30', now()::timestamp, 'TEST.USER', now()::timestamp, 2, 'DIFFERENT.USER');
INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by, cancelled_time, cancellation_reason_id, cancelled_by, is_deleted)
VALUES (213, 110, 4, 'MDI', 'AC1', 1, 123, false, now()::date, '09:00', '10:30', now()::timestamp, 'TEST.USER', now()::timestamp, 1, 'DIFFERENT.USER', true);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (320, 210, 'A1234BC', 456);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (321, 210, 'B2345CD', 457);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (322, 211, 'A1234BC', 456);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (323, 211, 'B2345CD', 457);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (324, 212, 'A1234BC', 456);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (325, 212, 'B2345CD', 457);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (326, 213, 'A1234BC', 456);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (327, 213, 'B2345CD', 457);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (328, 211, 'C3456DE', 458);

-- Appointments for tomorrow

--Individual appointments--
--Prisoner A1234BC, Category AC1, Location 123, Today 08:30-10:00, Created by TEST.USER
INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES (111, 'INDIVIDUAL', 'MDI', 'AC1', 1, 123, false, now()::date + 1, '08:30', '10:00', now()::timestamp, 'TEST.USER');
INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES (214, 111, 1, 'MDI', 'AC1', 1, 123, false, now()::date + 1, '08:30', '10:00', now()::timestamp, 'TEST.USER');
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (329, 214, 'A1234BC', 456);

--Prisoner B2345CD, Category AC2, Description Art Class, Location 456, Today 13:00-15:00, Created by DIFFERENT.USER
INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, custom_name, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES (112, 'INDIVIDUAL', 'MDI', 'AC2', 'Art Class', 1, 456, false, now()::date + 1, '13:00', '15:00', now()::timestamp, 'DIFFERENT.USER');
INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES (215, 112, 1, 'MDI', 'AC2', 1, 456, false, now()::date + 1, '13:00', '15:00', now()::timestamp, 'DIFFERENT.USER');
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (330, 215, 'B2345CD', 457);

--Prisoner A1234BC, Category AC3, In cell, One week from now 12:30-14:00, Created by TEST.USER
INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES (113, 'INDIVIDUAL', 'MDI', 'AC3', 1, null, true, now()::date + 1, '12:30', '14:00', now()::timestamp, 'TEST.USER');
INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES (216, 113, 1, 'MDI', 'AC3', 1, null, true, now()::date + 1, '12:30', '14:00', now()::timestamp, 'TEST.USER');
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (331, 216, 'A1234BC', 456);

--Prison OTH, Prisoner A1234BC, Category AC1, Location 789, Today 09:00-10:30, Created by OTHER.USER
INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES (114, 'INDIVIDUAL', 'OTH', 'AC1', 1, 789, false, now()::date + 1, '09:00', '10:30', now()::timestamp, 'OTHER.USER');
INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES (217, 114, 1, 'OTH', 'AC1', 1, 789, false, now()::date + 1, '09:00', '10:30', now()::timestamp, 'OTHER.USER');
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (332, 217, 'D4567EF', 459);

--Individual appointments--
--Prisoner A1234BC, Category AC1, Location 123, Today 08:30-10:00, Created by TEST.USER
INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES (115, 'INDIVIDUAL', 'MDI', 'AC1', 1, 123, false, now()::date + 1, '21:00', '22:30', now()::timestamp, 'TEST.USER');
INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES (218, 115, 1, 'MDI', 'AC1', 1, 123, false, now()::date + 1, '21:00', '22:30', now()::timestamp, 'TEST.USER');
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (333, 218, 'A1234BC', 456);

--Group appointments--
--Prisoners A1234BC and B2345CD, Category AC1, Location 123, Started one week ago 09:00-10:30, Repeating weekly 4 times, One edited, One cancelled, One deleted, Created by TEST.USER
INSERT INTO appointment_series_schedule (appointment_series_schedule_id, frequency, number_of_appointments)
VALUES (11, 'WEEKLY', 4);
INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, appointment_series_schedule_id, created_time, created_by)
VALUES (116, 'GROUP', 'MDI', 'AC1', 1, 123, false, now()::date + 1, '09:00', '10:30', 10, now()::timestamp, 'TEST.USER');
INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES (219, 116, 1, 'MDI', 'AC1', 1, 123, false, now()::date + 1, '09:00', '10:30', now()::timestamp, 'TEST.USER');
INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by, updated_time, updated_by)
VALUES (220, 116, 2, 'MDI', 'AC1', 1, 456, false, now()::date + 1, '13:30', '15:00', now()::timestamp, 'TEST.USER', now()::timestamp, 'DIFFERENT.USER');
INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by, cancelled_time, cancellation_reason_id, cancelled_by)
VALUES (221, 116, 3, 'MDI', 'AC1', 1, 123, false, now()::date + 1, '09:00', '10:30', now()::timestamp, 'TEST.USER', now()::timestamp, 2, 'DIFFERENT.USER');
INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by, cancelled_time, cancellation_reason_id, cancelled_by, is_deleted)
VALUES (222, 116, 4, 'MDI', 'AC1', 1, 123, false, now()::date + 1, '09:00', '10:30', now()::timestamp, 'TEST.USER', now()::timestamp, 1, 'DIFFERENT.USER', true);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (334, 219, 'A1234BC', 456);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (335, 219, 'B2345CD', 457);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (336, 220, 'A1234BC', 456);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (337, 220, 'B2345CD', 457);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (338, 220, 'C3456DE', 458);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (339, 221, 'A1234BC', 456);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (340, 221, 'B2345CD', 457);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (341, 222, 'A1234BC', 456);
INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES (342, 222, 'B2345CD', 457);

