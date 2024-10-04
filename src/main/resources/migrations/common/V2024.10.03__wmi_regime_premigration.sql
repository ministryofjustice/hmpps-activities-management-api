-- delete all regime time data for WMI, ensuring what follows is repeatable regardless of current state of WMI regime times
delete from prison_regime_days_of_week prdow 
where prison_regime_id in (select prison_regime_id from prison_regime where prison_code='WMI');

delete from prison_regime where prison_code = 'WMI';

-- insert the regime times needed for WMI prior to migration
-- Mon - Friday
insert into prison_regime (prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
VALUES ('WMI', '08:30:00', '11:45:00', '13:45:00', '16:45:00', '17:30:00', '19:00:00');

insert into prison_regime_days_of_week(prison_regime_id, day_of_week)
values ((select prison_regime_id from prison_regime where prison_code = 'WMI'), 'MONDAY'),
((select prison_regime_id from prison_regime where prison_code = 'WMI'), 'TUESDAY'),
((select prison_regime_id from prison_regime where prison_code = 'WMI'), 'WEDNESDAY'),
((select prison_regime_id from prison_regime where prison_code = 'WMI'), 'THURSDAY'),
((select prison_regime_id from prison_regime where prison_code = 'WMI'), 'FRIDAY');

-- Sat - Sunday
insert into prison_regime (prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
VALUES ('WMI', '09:00:00', '11:30:00', '13:45:00', '15:45:00', '17:30:00', '19:30:00');

insert into prison_regime_days_of_week(prison_regime_id, day_of_week)
values ((select prison_regime_id from prison_regime where prison_code = 'WMI' and am_start = '09:00:00'), 'SATURDAY'),
((select prison_regime_id from prison_regime where prison_code = 'WMI' and am_start = '09:00:00'), 'SUNDAY');
