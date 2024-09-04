insert into prison_pay_band(prison_pay_band_id,display_sequence, nomis_pay_band, pay_band_alias, pay_band_description, prison_code)
values (100011,1, 1, 'Low', 'Pay band 1 (Lowest)', 'IWI'),
       (200011,2, 2, 'Medium', 'Pay band 2', 'IWI'),
       (300011,3, 3, 'High', 'Pay band 3 (highest)', 'IWI');

insert into prison_regime (prison_regime_id, prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
VALUES (1000, 'FMI', '08:30:00', '11:30:00', '13:30:00', '16:30:00', '18:00:00', '19:00:00');

insert into prison_regime_days_of_week(prison_regime_id, day_of_week)
values (1000, 'MONDAY'),
 (1000, 'TUESDAY'),
 (1000, 'WEDNESDAY'),
 (1000, 'THURSDAY'),
 (1000, 'FRIDAY'),
 (1000, 'SATURDAY'),
 (1000, 'SUNDAY');

