ALTER TABLE waiting_list ALTER COLUMN requested_by TYPE varchar(50);

UPDATE waiting_list
SET requested_by = 'MULTI_DISCIPLINARY_BOARD'
WHERE requested_by = 'Staff in a sequencing or allocation meeting';
