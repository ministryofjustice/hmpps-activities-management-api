--
-- Strictly speaking we the ended_at should always be populated. However if the job were to fail e.g out of memory, it might never finish cleanly.
--
ALTER TABLE job ALTER COLUMN ended_at DROP NOT NULL;