insert into prison_regime (prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
VALUES ('GTI', '09:30:00', '11:30:00', '13:45:00', '16:30:00', '16:30:00', '18:30:00');

insert into prison_regime_days_of_week(prison_regime_id, day_of_week)
values ((select prison_regime_id from prison_regime where prison_code = 'GTI'), 'MONDAY'),
((select prison_regime_id from prison_regime where prison_code = 'GTI'), 'TUESDAY'),
((select prison_regime_id from prison_regime where prison_code = 'GTI'), 'WEDNESDAY'),
((select prison_regime_id from prison_regime where prison_code = 'GTI'), 'THURSDAY');

insert into prison_regime (prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
VALUES ('GTI', '08:30:00', '11:30:00', '13:45:00', '16:30:00', '16:30:00', '18:30:00');

insert into prison_regime_days_of_week(prison_regime_id, day_of_week)
values ((select prison_regime_id from prison_regime where prison_code = 'GTI' and am_start = '08:30:00'), 'FRIDAY'),
((select prison_regime_id from prison_regime where prison_code = 'GTI' and am_start = '08:30:00'), 'SATURDAY'),
((select prison_regime_id from prison_regime where prison_code = 'GTI' and am_start = '08:30:00'), 'SUNDAY');