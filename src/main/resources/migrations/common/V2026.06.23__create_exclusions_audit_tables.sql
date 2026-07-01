create table exclusion_aud
(
    exclusion_id  bigint    not null,
    allocation_id bigserial not null references allocation (allocation_id),
    week_number   integer not null,
    start_date    date,
    end_date      date,
    time_slot     char(2)   not null,
    rev           bigint    not null references revision (id),
    revtype       smallint  not null,
    primary key (exclusion_id, rev)
);

create index idx_exclusion_aud_allocation_id on exclusion_aud (allocation_id);

create table exclusion_days_of_week_aud
(
    id           bigint   not null,
    exclusion_id bigint   not null,
    day_of_week  varchar(10),
    rev          bigint   not null references revision (id),
    revtype      smallint not null,
    primary key (id, rev)
);

create index idx_exclusion_days_of_week_exclusion_id on exclusion_days_of_week_aud (exclusion_id);