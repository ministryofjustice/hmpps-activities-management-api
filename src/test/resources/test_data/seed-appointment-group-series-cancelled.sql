INSERT INTO public.appointment_series_schedule
(appointment_series_schedule_id, frequency, number_of_appointments)
VALUES(6, 'DAILY', 10);

INSERT INTO public.appointment_series
(appointment_series_id, appointment_type, prison_code, category_code, custom_name, internal_location_id, custom_location, in_cell, on_wing, off_wing, start_date, start_time, end_time, appointment_series_schedule_id, unlock_notes, extra_information, created_time, created_by, updated_time, updated_by, is_migrated, appointment_tier_id, appointment_organiser_id, cancelled_by, cancelled_time, cancellation_start_date, cancellation_start_time)
VALUES(8, 'GROUP', 'MDI', 'AC1', NULL, 607011, NULL, false, false, true, '2022-10-13', '11:30:00', '12:00:00', 6, NULL, NULL, '2022-10-15 11:08:48.149', 'DHOUSTON_GEN', NULL, NULL, false, 1, NULL, 'DHOUSTON_GEN', '2022-10-16 10:59:08.841', '2022-10-16', '11:30:00');

INSERT INTO public.appointment
(appointment_id, appointment_series_id, sequence_number, prison_code, category_code, custom_name, internal_location_id, custom_location, in_cell, on_wing, off_wing, start_date, start_time, end_time, unlock_notes, extra_information, created_time, created_by, updated_time, updated_by, cancelled_time, cancellation_reason_id, cancelled_by, is_deleted, appointment_tier_id, appointment_organiser_id)
VALUES(89, 8, 1, 'MDI', 'AC1', NULL, 607011, NULL, false, false, true, '2022-10-12', '11:30:00', '12:00:00', NULL, NULL, '2022-10-15 11:08:48.149', 'DHOUSTON_GEN', NULL, NULL, NULL, NULL, NULL, false, 1, NULL);
INSERT INTO public.appointment
(appointment_id, appointment_series_id, sequence_number, prison_code, category_code, custom_name, internal_location_id, custom_location, in_cell, on_wing, off_wing, start_date, start_time, end_time, unlock_notes, extra_information, created_time, created_by, updated_time, updated_by, cancelled_time, cancellation_reason_id, cancelled_by, is_deleted, appointment_tier_id, appointment_organiser_id)
VALUES(87, 8, 1, 'MDI', 'AC1', NULL, 607011, NULL, false, false, true, '2022-10-13', '11:30:00', '12:00:00', NULL, NULL, '2022-10-15 11:08:48.149', 'DHOUSTON_GEN', NULL, NULL, NULL, NULL, NULL, false, 1, NULL);
INSERT INTO public.appointment
(appointment_id, appointment_series_id, sequence_number, prison_code, category_code, custom_name, internal_location_id, custom_location, in_cell, on_wing, off_wing, start_date, start_time, end_time, unlock_notes, extra_information, created_time, created_by, updated_time, updated_by, cancelled_time, cancellation_reason_id, cancelled_by, is_deleted, appointment_tier_id, appointment_organiser_id)
VALUES(88, 8, 1, 'MDI', 'AC1', NULL, 607011, NULL, false, false, true, '2022-10-14', '11:30:00', '12:00:00', NULL, NULL, '2022-10-15 11:08:48.149', 'DHOUSTON_GEN', NULL, NULL, NULL, NULL, NULL, false, 1, NULL);
INSERT INTO public.appointment
(appointment_id, appointment_series_id, sequence_number, prison_code, category_code, custom_name, internal_location_id, custom_location, in_cell, on_wing, off_wing, start_date, start_time, end_time, unlock_notes, extra_information, created_time, created_by, updated_time, updated_by, cancelled_time, cancellation_reason_id, cancelled_by, is_deleted, appointment_tier_id, appointment_organiser_id)
VALUES(77, 8, 1, 'MDI', 'AC1', NULL, 607011, NULL, false, false, true, '2022-10-15', '11:30:00', '12:00:00', NULL, NULL, '2022-10-15 11:08:48.149', 'DHOUSTON_GEN', NULL, NULL, NULL, NULL, NULL, false, 1, NULL);
INSERT INTO public.appointment
(appointment_id, appointment_series_id, sequence_number, prison_code, category_code, custom_name, internal_location_id, custom_location, in_cell, on_wing, off_wing, start_date, start_time, end_time, unlock_notes, extra_information, created_time, created_by, updated_time, updated_by, cancelled_time, cancellation_reason_id, cancelled_by, is_deleted, appointment_tier_id, appointment_organiser_id)
VALUES(78, 8, 2, 'MDI', 'AC1', NULL, 607011, NULL, false, false, true, '2022-10-16', '11:30:00', '12:00:00', NULL, NULL, '2022-10-15 11:08:48.149', 'DHOUSTON_GEN', NULL, NULL, '2022-10-16 10:59:08.841', 2, 'DHOUSTON_GEN', false, 1, NULL);
INSERT INTO public.appointment
(appointment_id, appointment_series_id, sequence_number, prison_code, category_code, custom_name, internal_location_id, custom_location, in_cell, on_wing, off_wing, start_date, start_time, end_time, unlock_notes, extra_information, created_time, created_by, updated_time, updated_by, cancelled_time, cancellation_reason_id, cancelled_by, is_deleted, appointment_tier_id, appointment_organiser_id)
VALUES(79, 8, 3, 'MDI', 'AC1', NULL, 607011, NULL, false, false, true, '2022-10-17', '11:30:00', '12:00:00', NULL, NULL, '2022-10-15 11:08:48.149', 'DHOUSTON_GEN', NULL, NULL, '2022-10-16 10:59:08.841', 2, 'DHOUSTON_GEN', false, 1, NULL);
INSERT INTO public.appointment
(appointment_id, appointment_series_id, sequence_number, prison_code, category_code, custom_name, internal_location_id, custom_location, in_cell, on_wing, off_wing, start_date, start_time, end_time, unlock_notes, extra_information, created_time, created_by, updated_time, updated_by, cancelled_time, cancellation_reason_id, cancelled_by, is_deleted, appointment_tier_id, appointment_organiser_id)
VALUES(80, 8, 4, 'MDI', 'AC1', NULL, 607011, NULL, false, false, true, '2022-10-18', '11:30:00', '12:00:00', NULL, NULL, '2022-10-15 11:08:48.149', 'DHOUSTON_GEN', NULL, NULL, '2022-10-16 10:58:26.464', 2, 'DHOUSTON_GEN', false, 1, NULL);
INSERT INTO public.appointment
(appointment_id, appointment_series_id, sequence_number, prison_code, category_code, custom_name, internal_location_id, custom_location, in_cell, on_wing, off_wing, start_date, start_time, end_time, unlock_notes, extra_information, created_time, created_by, updated_time, updated_by, cancelled_time, cancellation_reason_id, cancelled_by, is_deleted, appointment_tier_id, appointment_organiser_id)
VALUES(81, 8, 5, 'MDI', 'AC1', NULL, 607011, NULL, false, false, true, '2022-10-19', '11:30:00', '12:00:00', NULL, NULL, '2022-10-15 11:08:48.149', 'DHOUSTON_GEN', NULL, NULL, '2022-10-16 10:59:08.841', 2, 'DHOUSTON_GEN', false, 1, NULL);
INSERT INTO public.appointment
(appointment_id, appointment_series_id, sequence_number, prison_code, category_code, custom_name, internal_location_id, custom_location, in_cell, on_wing, off_wing, start_date, start_time, end_time, unlock_notes, extra_information, created_time, created_by, updated_time, updated_by, cancelled_time, cancellation_reason_id, cancelled_by, is_deleted, appointment_tier_id, appointment_organiser_id)
VALUES(82, 8, 6, 'MDI', 'AC1', NULL, 607011, NULL, false, false, true, '2022-10-20', '11:30:00', '12:00:00', NULL, NULL, '2022-10-15 11:08:48.149', 'DHOUSTON_GEN', NULL, NULL, '2022-10-16 10:59:08.841', 2, 'DHOUSTON_GEN', false, 1, NULL);
INSERT INTO public.appointment
(appointment_id, appointment_series_id, sequence_number, prison_code, category_code, custom_name, internal_location_id, custom_location, in_cell, on_wing, off_wing, start_date, start_time, end_time, unlock_notes, extra_information, created_time, created_by, updated_time, updated_by, cancelled_time, cancellation_reason_id, cancelled_by, is_deleted, appointment_tier_id, appointment_organiser_id)
VALUES(83, 8, 7, 'MDI', 'AC1', NULL, 607011, NULL, false, false, true, '2022-10-21', '11:30:00', '12:00:00', NULL, NULL, '2022-10-15 11:08:48.149', 'DHOUSTON_GEN', NULL, NULL, '2022-10-16 10:59:08.841', 2, 'DHOUSTON_GEN', false, 1, NULL);
INSERT INTO public.appointment
(appointment_id, appointment_series_id, sequence_number, prison_code, category_code, custom_name, internal_location_id, custom_location, in_cell, on_wing, off_wing, start_date, start_time, end_time, unlock_notes, extra_information, created_time, created_by, updated_time, updated_by, cancelled_time, cancellation_reason_id, cancelled_by, is_deleted, appointment_tier_id, appointment_organiser_id)
VALUES(84, 8, 8, 'MDI', 'AC1', NULL, 607011, NULL, false, false, true, '2022-10-22', '11:30:00', '12:00:00', NULL, NULL, '2022-10-15 11:08:48.149', 'DHOUSTON_GEN', NULL, NULL, '2022-10-16 10:59:08.841', 2, 'DHOUSTON_GEN', false, 1, NULL);
INSERT INTO public.appointment
(appointment_id, appointment_series_id, sequence_number, prison_code, category_code, custom_name, internal_location_id, custom_location, in_cell, on_wing, off_wing, start_date, start_time, end_time, unlock_notes, extra_information, created_time, created_by, updated_time, updated_by, cancelled_time, cancellation_reason_id, cancelled_by, is_deleted, appointment_tier_id, appointment_organiser_id)
VALUES(85, 8, 9, 'MDI', 'AC1', NULL, 607011, NULL, false, false, true, '2022-10-23', '11:30:00', '12:00:00', NULL, NULL, '2022-10-15 11:08:48.149', 'DHOUSTON_GEN', NULL, NULL, '2022-10-16 10:59:08.841', 2, 'DHOUSTON_GEN', false, 1, NULL);
INSERT INTO public.appointment
(appointment_id, appointment_series_id, sequence_number, prison_code, category_code, custom_name, internal_location_id, custom_location, in_cell, on_wing, off_wing, start_date, start_time, end_time, unlock_notes, extra_information, created_time, created_by, updated_time, updated_by, cancelled_time, cancellation_reason_id, cancelled_by, is_deleted, appointment_tier_id, appointment_organiser_id)
VALUES(86, 8, 10, 'MDI', 'AC1', NULL, 607011, NULL, false, false, true, '2022-10-24', '11:30:00', '12:00:00', NULL, NULL, '2022-10-15 11:08:48.149', 'DHOUSTON_GEN', NULL, NULL, '2022-10-16 10:59:08.841', 2, 'DHOUSTON_GEN', false, 1, NULL);

INSERT INTO public.appointment_attendee
(appointment_attendee_id, appointment_id, prisoner_number, booking_id, added_time, added_by, attended, attendance_recorded_time, attendance_recorded_by, removed_time, removal_reason_id, removed_by, is_deleted)
VALUES(221, 77, 'G4793VF', 1085025, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false);
INSERT INTO public.appointment_attendee
(appointment_attendee_id, appointment_id, prisoner_number, booking_id, added_time, added_by, attended, attendance_recorded_time, attendance_recorded_by, removed_time, removal_reason_id, removed_by, is_deleted)
VALUES(222, 77, 'A5193DY', 1103531, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false);
INSERT INTO public.appointment_attendee
(appointment_attendee_id, appointment_id, prisoner_number, booking_id, added_time, added_by, attended, attendance_recorded_time, attendance_recorded_by, removed_time, removal_reason_id, removed_by, is_deleted)
VALUES(223, 78, 'G4793VF', 1085025, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false);
INSERT INTO public.appointment_attendee
(appointment_attendee_id, appointment_id, prisoner_number, booking_id, added_time, added_by, attended, attendance_recorded_time, attendance_recorded_by, removed_time, removal_reason_id, removed_by, is_deleted)
VALUES(224, 78, 'A5193DY', 1103531, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false);
INSERT INTO public.appointment_attendee
(appointment_attendee_id, appointment_id, prisoner_number, booking_id, added_time, added_by, attended, attendance_recorded_time, attendance_recorded_by, removed_time, removal_reason_id, removed_by, is_deleted)
VALUES(225, 79, 'G4793VF', 1085025, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false);
INSERT INTO public.appointment_attendee
(appointment_attendee_id, appointment_id, prisoner_number, booking_id, added_time, added_by, attended, attendance_recorded_time, attendance_recorded_by, removed_time, removal_reason_id, removed_by, is_deleted)
VALUES(226, 79, 'A5193DY', 1103531, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false);
INSERT INTO public.appointment_attendee
(appointment_attendee_id, appointment_id, prisoner_number, booking_id, added_time, added_by, attended, attendance_recorded_time, attendance_recorded_by, removed_time, removal_reason_id, removed_by, is_deleted)
VALUES(227, 80, 'G4793VF', 1085025, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false);
INSERT INTO public.appointment_attendee
(appointment_attendee_id, appointment_id, prisoner_number, booking_id, added_time, added_by, attended, attendance_recorded_time, attendance_recorded_by, removed_time, removal_reason_id, removed_by, is_deleted)
VALUES(228, 80, 'A5193DY', 1103531, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false);
INSERT INTO public.appointment_attendee
(appointment_attendee_id, appointment_id, prisoner_number, booking_id, added_time, added_by, attended, attendance_recorded_time, attendance_recorded_by, removed_time, removal_reason_id, removed_by, is_deleted)
VALUES(229, 81, 'G4793VF', 1085025, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false);
INSERT INTO public.appointment_attendee
(appointment_attendee_id, appointment_id, prisoner_number, booking_id, added_time, added_by, attended, attendance_recorded_time, attendance_recorded_by, removed_time, removal_reason_id, removed_by, is_deleted)
VALUES(230, 81, 'A5193DY', 1103531, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false);
INSERT INTO public.appointment_attendee
(appointment_attendee_id, appointment_id, prisoner_number, booking_id, added_time, added_by, attended, attendance_recorded_time, attendance_recorded_by, removed_time, removal_reason_id, removed_by, is_deleted)
VALUES(231, 82, 'G4793VF', 1085025, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false);
INSERT INTO public.appointment_attendee
(appointment_attendee_id, appointment_id, prisoner_number, booking_id, added_time, added_by, attended, attendance_recorded_time, attendance_recorded_by, removed_time, removal_reason_id, removed_by, is_deleted)
VALUES(232, 82, 'A5193DY', 1103531, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false);
INSERT INTO public.appointment_attendee
(appointment_attendee_id, appointment_id, prisoner_number, booking_id, added_time, added_by, attended, attendance_recorded_time, attendance_recorded_by, removed_time, removal_reason_id, removed_by, is_deleted)
VALUES(233, 83, 'G4793VF', 1085025, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false);
INSERT INTO public.appointment_attendee
(appointment_attendee_id, appointment_id, prisoner_number, booking_id, added_time, added_by, attended, attendance_recorded_time, attendance_recorded_by, removed_time, removal_reason_id, removed_by, is_deleted)
VALUES(234, 83, 'A5193DY', 1103531, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false);
INSERT INTO public.appointment_attendee
(appointment_attendee_id, appointment_id, prisoner_number, booking_id, added_time, added_by, attended, attendance_recorded_time, attendance_recorded_by, removed_time, removal_reason_id, removed_by, is_deleted)
VALUES(235, 84, 'G4793VF', 1085025, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false);
INSERT INTO public.appointment_attendee
(appointment_attendee_id, appointment_id, prisoner_number, booking_id, added_time, added_by, attended, attendance_recorded_time, attendance_recorded_by, removed_time, removal_reason_id, removed_by, is_deleted)
VALUES(236, 84, 'A5193DY', 1103531, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false);
INSERT INTO public.appointment_attendee
(appointment_attendee_id, appointment_id, prisoner_number, booking_id, added_time, added_by, attended, attendance_recorded_time, attendance_recorded_by, removed_time, removal_reason_id, removed_by, is_deleted)
VALUES(237, 85, 'G4793VF', 1085025, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false);
INSERT INTO public.appointment_attendee
(appointment_attendee_id, appointment_id, prisoner_number, booking_id, added_time, added_by, attended, attendance_recorded_time, attendance_recorded_by, removed_time, removal_reason_id, removed_by, is_deleted)
VALUES(238, 85, 'A5193DY', 1103531, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false);
INSERT INTO public.appointment_attendee
(appointment_attendee_id, appointment_id, prisoner_number, booking_id, added_time, added_by, attended, attendance_recorded_time, attendance_recorded_by, removed_time, removal_reason_id, removed_by, is_deleted)
VALUES(239, 86, 'G4793VF', 1085025, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false);
INSERT INTO public.appointment_attendee
(appointment_attendee_id, appointment_id, prisoner_number, booking_id, added_time, added_by, attended, attendance_recorded_time, attendance_recorded_by, removed_time, removal_reason_id, removed_by, is_deleted)
VALUES(240, 86, 'A5193DY', 1103531, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false);
INSERT INTO public.appointment_attendee
(appointment_attendee_id, appointment_id, prisoner_number, booking_id, added_time, added_by, attended, attendance_recorded_time, attendance_recorded_by, removed_time, removal_reason_id, removed_by, is_deleted)
VALUES(241, 87, 'G4793VF', 1085025, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false);
INSERT INTO public.appointment_attendee
(appointment_attendee_id, appointment_id, prisoner_number, booking_id, added_time, added_by, attended, attendance_recorded_time, attendance_recorded_by, removed_time, removal_reason_id, removed_by, is_deleted)
VALUES(242, 87, 'A5193DY', 1103531, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false);
INSERT INTO public.appointment_attendee
(appointment_attendee_id, appointment_id, prisoner_number, booking_id, added_time, added_by, attended, attendance_recorded_time, attendance_recorded_by, removed_time, removal_reason_id, removed_by, is_deleted)
VALUES(243, 88, 'G4793VF', 1085025, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false);
INSERT INTO public.appointment_attendee
(appointment_attendee_id, appointment_id, prisoner_number, booking_id, added_time, added_by, attended, attendance_recorded_time, attendance_recorded_by, removed_time, removal_reason_id, removed_by, is_deleted)
VALUES(244, 88, 'A5193DY', 1103531, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false);
INSERT INTO public.appointment_attendee
(appointment_attendee_id, appointment_id, prisoner_number, booking_id, added_time, added_by, attended, attendance_recorded_time, attendance_recorded_by, removed_time, removal_reason_id, removed_by, is_deleted)
VALUES(245, 89, 'G4793VF', 1085025, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false);
INSERT INTO public.appointment_attendee
(appointment_attendee_id, appointment_id, prisoner_number, booking_id, added_time, added_by, attended, attendance_recorded_time, attendance_recorded_by, removed_time, removal_reason_id, removed_by, is_deleted)
VALUES(246, 89, 'A5193DY', 1103531, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false);
