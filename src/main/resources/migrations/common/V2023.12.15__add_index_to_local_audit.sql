CREATE INDEX IF NOT EXISTS idx_local_audit_prison_code_prisoner_number on local_audit(prison_code, prisoner_number);
CREATE INDEX IF NOT EXISTS idx_local_audit_prisoner_number on local_audit(prisoner_number);
