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
((select prison_regime_id from prison_regime where prison_code = 'LPI'), 'SUNDAY'),

((select prison_regime_id from prison_regime where prison_code = 'IWI'), 'MONDAY'),
((select prison_regime_id from prison_regime where prison_code = 'IWI'), 'TUESDAY'),
((select prison_regime_id from prison_regime where prison_code = 'IWI'), 'WEDNESDAY'),
((select prison_regime_id from prison_regime where prison_code = 'IWI'), 'THURSDAY'),
((select prison_regime_id from prison_regime where prison_code = 'IWI'), 'FRIDAY'),
((select prison_regime_id from prison_regime where prison_code = 'IWI'), 'SATURDAY'),
((select prison_regime_id from prison_regime where prison_code = 'IWI'), 'SUNDAY');
