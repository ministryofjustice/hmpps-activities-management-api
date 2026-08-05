DO $$
DECLARE
	updated_rows INTEGER;
BEGIN
	UPDATE planned_suspension ps
	SET planned_end_date = DATE '2026-08-05'
	FROM allocation allo
	WHERE allo.allocation_id = ps.allocation_id
		AND allo.prisoner_status <> 'ENDED'
		AND ps.planned_by = 'MIGRATION'
		AND ps.planned_end_date IS NULL
		AND ps.planned_start_date >= DATE '2026-07-01';

	GET DIAGNOSTICS updated_rows = ROW_COUNT;

	IF updated_rows < 1 OR updated_rows > 150 THEN
		RAISE EXCEPTION USING
			MESSAGE = format(
				'Cleanup planned suspensions updated %s rows; expected between 1 and 150 (target around 118).',
				updated_rows
			);
	END IF;
END;

