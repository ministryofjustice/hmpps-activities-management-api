UPDATE attendance_reason SET description = 'Prisoner’s schedule shows another activity' where code = 'CLASH';
UPDATE attendance_reason SET description = 'Other: absence reason not listed' where code = 'OTHER';
