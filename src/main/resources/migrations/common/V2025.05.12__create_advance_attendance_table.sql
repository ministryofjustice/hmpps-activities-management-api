create table advance_attendance
(
    advance_attendance_id          bigserial  not null constraint advance_attendance_pk primary key,
    scheduled_instance_id          bigint     not null references scheduled_instance (scheduled_instance_id),
    prisoner_number                varchar(7) not null,
    issue_payment                  bool not null,
    recorded_time                  timestamp,
    recorded_by                    varchar(100)
);

create index idx_advance_attendance_scheduled_instance_id on advance_attendance (scheduled_instance_id);
create index idx_advance_attendance_prisoner_number on advance_attendance (prisoner_number);
create index idx_advance_attendance_recorded_time on advance_attendance (recorded_time);
create unique index idx_advance_attendance_scheduled_instance_id_prison_number on advance_attendance (scheduled_instance_id, prisoner_number);

create table advance_attendance_history
(
    advance_attendance_history_id  bigserial    not null constraint advance_attendance_history_pk primary key,
    advance_attendance_id          bigint       not null references advance_attendance (advance_attendance_id),
    recorded_time                  timestamp    not null,
    recorded_by                    varchar(100) not null,
    issue_payment                  bool
);

create index idx_advance_attendance_history_attendance_id ON advance_attendance_history (advance_attendance_id);
