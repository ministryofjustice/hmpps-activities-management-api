UPDATE appointment_category SET status = 'INACTIVE'
WHERE code IN ('PA', 'PROG_SESS');
