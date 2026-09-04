-- Records, per affected allocation, the sessions added/removed by an activity schedule slot amendment.
-- Only allocations actually affected by the amendment (i.e. not already excluded from a removed session,
-- or newly attending an added session) are added to this table.
create table activity_schedule_change_impact
(
    activity_schedule_change_impact_id bigserial NOT NULL CONSTRAINT activity_schedule_change_impact_pk primary key,
    activity_id                        bigint    NOT NULL REFERENCES activity (activity_id),
    activity_schedule_id               bigint    NOT NULL REFERENCES activity_schedule (activity_schedule_id),
    allocation_id                      bigint    NOT NULL REFERENCES allocation (allocation_id),
    prisoner_number                    varchar   NOT NULL,
    changed_at                         timestamp NOT NULL,
    changed_by                         varchar   NOT NULL,
    added_sessions                     text,
    removed_sessions                   text
);

create index idx_activity_schedule_change_impact_allocation_id on activity_schedule_change_impact (allocation_id);
create index idx_activity_schedule_change_impact_prisoner_schedule on activity_schedule_change_impact (prisoner_number, activity_schedule_id, changed_at);
