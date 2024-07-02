-- This fixes the data where an attendance record was changed to not paid after the payment had been issued
update attendance set attendance_reason_id = 9 where attendance_id = 703695;
