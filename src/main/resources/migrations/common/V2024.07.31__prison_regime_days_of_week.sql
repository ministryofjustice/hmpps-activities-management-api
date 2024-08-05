DROP INDEX if exists idx_prison_regime_prison_code;

CREATE TABLE prison_regime_days_of_week (
    id                          serial primary key,
    prison_regime_id bigint references prison_regime (prison_regime_id),
    day_of_week             varchar(10)
);

create index prison_regime_days_of_week_idx on prison_regime_days_of_week(prison_regime_id);

insert into prison_regime_days_of_week(prison_regime_id, day_of_week)
values ((select prison_regime_id from prison_regime where prison_code = 'RSI'), 'MONDAY'),
((select prison_regime_id from prison_regime where prison_code = 'RSI'), 'TUESDAY'),
((select prison_regime_id from prison_regime where prison_code = 'RSI'), 'WEDNESDAY'),
((select prison_regime_id from prison_regime where prison_code = 'RSI'), 'THURSDAY'),
((select prison_regime_id from prison_regime where prison_code = 'RSI'), 'FRIDAY'),
((select prison_regime_id from prison_regime where prison_code = 'RSI'), 'SATURDAY'),
((select prison_regime_id from prison_regime where prison_code = 'RSI'), 'SUNDAY'),

((select prison_regime_id from prison_regime where prison_code = 'WDI'), 'MONDAY'),
((select prison_regime_id from prison_regime where prison_code = 'WDI'), 'TUESDAY'),
((select prison_regime_id from prison_regime where prison_code = 'WDI'), 'WEDNESDAY'),
((select prison_regime_id from prison_regime where prison_code = 'WDI'), 'THURSDAY'),
((select prison_regime_id from prison_regime where prison_code = 'WDI'), 'FRIDAY'),
((select prison_regime_id from prison_regime where prison_code = 'WDI'), 'SATURDAY'),
((select prison_regime_id from prison_regime where prison_code = 'WDI'), 'SUNDAY'),

((select prison_regime_id from prison_regime where prison_code = 'LPI'), 'MONDAY'),
((select prison_regime_id from prison_regime where prison_code = 'LPI'), 'TUESDAY'),
((select prison_regime_id from prison_regime where prison_code = 'LPI'), 'WEDNESDAY'),
((select prison_regime_id from prison_regime where prison_code = 'LPI'), 'THURSDAY'),
((select prison_regime_id from prison_regime where prison_code = 'LPI'), 'FRIDAY'),
((select prison_regime_id from prison_regime where prison_code = 'LPI'), 'SATURDAY'),
((select prison_regime_id from prison_regime where prison_code = 'LPI'), 'SUNDAY');

delete from prison_regime where prison_code = 'IWI';

insert into prison_regime (prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
VALUES ('IWI', '08:25:00', '11:35:00', '13:40:00', '16:50:00', '18:00:00', '19:00:00');

insert into prison_regime_days_of_week(prison_regime_id, day_of_week)
values ((select prison_regime_id from prison_regime where prison_code = 'IWI' and am_start = '08:25:00'),'FRIDAY');

insert into prison_regime (prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
VALUES ('IWI', '09:25:00', '11:35:00', '13:40:00', '16:50:00', '18:00:00', '19:00:00');

insert into prison_regime_days_of_week(prison_regime_id, day_of_week)
values ((select prison_regime_id from prison_regime where prison_code = 'IWI' and am_start = '09:25:00'), 'MONDAY'),
((select prison_regime_id from prison_regime where prison_code = 'IWI' and am_start = '09:25:00'), 'TUESDAY'),
((select prison_regime_id from prison_regime where prison_code = 'IWI' and am_start = '09:25:00'), 'WEDNESDAY'),
((select prison_regime_id from prison_regime where prison_code = 'IWI' and am_start = '09:25:00'), 'THURSDAY');

insert into prison_regime (prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
VALUES ('IWI', '09:00:00', '11:30:00', '13:40:00', '16:45:00', '18:00:00', '19:00:00');

insert into prison_regime_days_of_week(prison_regime_id, day_of_week)
values ((select prison_regime_id from prison_regime where prison_code = 'IWI' and am_start = '09:00:00'), 'SATURDAY'),
((select prison_regime_id from prison_regime where prison_code = 'IWI' and am_start = '09:00:00'), 'SUNDAY');

alter table prison_regime drop column max_days_to_expiry;

ALTER TABLE rollout_prison ADD COLUMN max_days_to_expiry integer not null default 21;

alter table activity_schedule_slot drop column use_prison_regime_time;

alter table activity_schedule add column use_prison_regime_time boolean not null default true;

