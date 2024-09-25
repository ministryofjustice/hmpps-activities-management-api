insert into prison_regime (prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
VALUES ('WMI', '08:30:00', '11:45:00', '13:45:00', '16:45:00', '17:00:00', '18:30:00');

insert into prison_regime_days_of_week(prison_regime_id, day_of_week)
values ((select prison_regime_id from prison_regime where prison_code = 'WMI'), 'MONDAY'),
((select prison_regime_id from prison_regime where prison_code = 'WMI'), 'TUESDAY'),
((select prison_regime_id from prison_regime where prison_code = 'WMI'), 'WEDNESDAY'),
((select prison_regime_id from prison_regime where prison_code = 'WMI'), 'THURSDAY'),
((select prison_regime_id from prison_regime where prison_code = 'WMI'), 'FRIDAY'),
((select prison_regime_id from prison_regime where prison_code = 'WMI'), 'SATURDAY'),
((select prison_regime_id from prison_regime where prison_code = 'WMI'), 'SUNDAY');