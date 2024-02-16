--
-- This index was added to prevent duplicate allocations to the same activity, however this has had a knock on effect with deallocation.
-- We have found that an offender can be allocated to the same activity on the same date provided earlier one is already ended, however if you then try
-- to end the later allocation a duplicate key violation is incurred.  This is causing issues the deallocate expiring allocations job.
--
DROP INDEX IF EXISTS idx_schedule_id_prisoner_number_start_date_prisoner_status;