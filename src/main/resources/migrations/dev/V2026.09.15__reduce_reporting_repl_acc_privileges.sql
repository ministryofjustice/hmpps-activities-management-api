-- pglogical isn't bundled in the vanilla postgres image used for local/test envs
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_available_extensions WHERE name = 'pglogical') THEN
    EXECUTE 'CREATE EXTENSION IF NOT EXISTS pglogical';
  END IF;
END
$$;

-- role only exists in environments where DPR replication has been provisioned
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'digital_prison_reporting') THEN
    GRANT pg_read_all_data TO digital_prison_reporting;
    REVOKE rds_superuser FROM digital_prison_reporting;
    -- rds_superuser previously granted connect/create implicitly; restore explicitly so replication tooling still works
    EXECUTE format('GRANT CONNECT, CREATE ON DATABASE %I TO digital_prison_reporting', current_database());
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'rds_replication') THEN
      GRANT rds_replication TO digital_prison_reporting;
    END IF;
  END IF;
END
$$;