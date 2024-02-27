-- Compatibility with integration tests using H2 database
ALTER TABLE activity DROP CONSTRAINT IF EXISTS CONSTRAINT_CBF1;

