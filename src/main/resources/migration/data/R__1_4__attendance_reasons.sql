insert into attendance_reason(attendance_reason_id, code, description, attended, capture_pay, capture_more_detail, capture_case_note,
                              capture_incentive_level_warning, capture_other_text, display_in_absence, display_sequence, notes)
values (1, 'SICK', 'Sick', false, true, true, false, false, false, true, 1, 'Maps to ACCAB in NOMIS'),
       (2, 'REFUSED', 'Refused to attend', false, false, false, true, true, false, true, 2, 'Maps to UNACAB in NOMIS'),
       (3, 'NREQ', 'Not required or excused', false, false, false, false, false, false, true, 3, 'Maps to ACCAB in NOMIS'),
       (4, 'REST', 'Rest day', false, true, false, false, false, false, true, 4, 'Maps to ACCAB in NOMIS'),
       (5, 'CLASH', 'Prisoner''s schedule shows another activity', false, false, false, false, false, false, true, 5, 'Maps to ACCAB in NOMIS'),
       (6, 'OTHER', 'Other absence reason not listed', false, true, false, false, false, true, true, 6, 'Maps to UNACAB in NOMIS'),
       (7, 'SUSP', 'Suspended', false, false, false, false, false, false, false, null, 'Maps to ACCAB in NOMIS'),
       (8, 'CANC', 'Cancelled', false, false, false, false, false, false, false, null, 'Maps to ACCAB in NOMIS'),
       (9, 'ATT', 'Attended', true, false, false, false, false, false, false, null, 'Maps to ATT in NOMIS');

alter sequence attendance_reason_attendance_reason_id_seq restart with 10;