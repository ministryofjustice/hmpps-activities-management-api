-- This fixes the data where an attendance record was changed to not paid after the payment had been issued
update attendance set issue_payment = true where attendance_id = 703695;

