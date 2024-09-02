update prison_regime set pm_finish = '16:00:00' where prison_code = 'IWI' and am_start = '08:25:00';
update prison_regime set pm_finish = '16:00:00' where prison_code = 'IWI' and am_start = '09:00:00';

delete from prison_regime_days_of_week where prison_regime_id in (select prison_regime_id from prison_regime where prison_code = 'FKI');
delete from prison_regime where prison_code = 'FKI';

insert into prison_regime (prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
VALUES ('FKI', '08:30:00', '11:35:00', '14:00:00', '16:30:00', '18:00:00', '19:00:00');

insert into prison_regime_days_of_week(prison_regime_id, day_of_week)
values ((select prison_regime_id from prison_regime where prison_code = 'FKI'), 'MONDAY'),
((select prison_regime_id from prison_regime where prison_code = 'FKI'), 'TUESDAY'),
((select prison_regime_id from prison_regime where prison_code = 'FKI'), 'WEDNESDAY'),
((select prison_regime_id from prison_regime where prison_code = 'FKI'), 'THURSDAY'),
((select prison_regime_id from prison_regime where prison_code = 'FKI'), 'FRIDAY');

insert into prison_regime (prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
VALUES ('FKI', '08:30:00', '11:45:00', '14:00:00', '16:30:00', '18:00:00', '19:00:00');

insert into prison_regime_days_of_week(prison_regime_id, day_of_week)
values ((select prison_regime_id from prison_regime where prison_code = 'FKI' and am_finish = '11:45:00'), 'SATURDAY'),
((select prison_regime_id from prison_regime where prison_code = 'FKI' and am_finish = '11:45:00'), 'SUNDAY');
