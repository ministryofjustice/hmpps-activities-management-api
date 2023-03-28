insert into prison_regime
(prison_regime_id, prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish) VALUES
    (1, 'MDI', '09:00:00', '12:00:00', '13:00:00', '16:30:00', '18:00:00', '20:00:00'),
    (2, 'LEI', '09:00:00', '12:00:00', '13:00:00', '16:30:00', '18:00:00', '20:00:00');

alter sequence prison_regime_prison_regime_id_seq restart with 3;
