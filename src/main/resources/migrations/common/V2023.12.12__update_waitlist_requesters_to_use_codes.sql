UPDATE waiting_list wl
SET requested_by = (
    CASE
        WHEN wl.requested_by = 'Self-requested' THEN 'PRISONER'
        WHEN wl.requested_by = 'IAG or CXK careers information, advice and guidance staff' THEN 'GUIDANCE_STAFF'
        WHEN wl.requested_by = 'Education staff' THEN 'EDUCATION_STAFF'
        WHEN wl.requested_by = 'Workshop staff' THEN 'WORKSHOP_STAFF'
        WHEN wl.requested_by = 'Activity leader' THEN 'ACTIVITY_LEADER'
        WHEN wl.requested_by = 'Mental health staff' THEN 'MENTAL_HEALTH_STAFF'
        WHEN wl.requested_by = 'Offender Management Unit' THEN 'OMU_STAFF'
        WHEN wl.requested_by = 'Wing staff' THEN 'WING_STAFF'
        WHEN wl.requested_by = 'Keyworker or POM' THEN 'POM_STAFF'
        WHEN wl.requested_by = 'Reception staff' THEN 'RECP_STAFF'
        WHEN wl.requested_by = 'Orderly or Red Band' THEN 'RED_BAND'
        WHEN wl.requested_by = 'Other' THEN 'OTHER'
        ELSE 'PRISONER'
    END
);

ALTER TABLE waiting_list ALTER COLUMN requested_by TYPE varchar(20);
