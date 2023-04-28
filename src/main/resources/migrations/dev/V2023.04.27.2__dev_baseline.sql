-- =============================================
-- ROLLOUT DATA
-- =============================================
insert into rollout_prison (rollout_prison_id, code, description, activities_to_be_rolled_out,
                            activities_rollout_date, appointments_to_be_rolled_out,
                            appointments_rollout_date)
values (1, 'LEI', 'HMP Leeds', true, '2022-12-19', false, null),
       (2, 'MDI', 'HMP Moorland', true, '2022-11-20', true, '2022-11-20'),
       (3, 'PVI', 'HMP Pentonville', true, '2024-01-01', false, null);
alter sequence if exists rollout_prison_rollout_prison_id_seq restart with 4;


-- =============================================
-- EVENT PRIORITY DATA
-- =============================================
insert into event_priority (event_priority_id, prison_code, event_type, event_category, priority)
values (1, 'MDI', 'COURT_HEARING', null, 1),
       (2, 'MDI', 'EXTERNAL_TRANSFER', null, 2),
       (3, 'MDI', 'ADJUDICATION_HEARING', null, 3),
       (4, 'MDI', 'VISIT', null, 4),
       (5, 'MDI', 'APPOINTMENT', null, 5),
       (6, 'MDI', 'ACTIVITY', null, 6);
alter sequence if exists event_priority_event_priority_id_seq restart with 7;

-- =============================================
-- PRISON REGIME DATA
-- =============================================
insert into prison_regime
(prison_regime_id, prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
VALUES (1, 'MDI', '09:00:00', '12:00:00', '13:00:00', '16:30:00', '18:00:00', '20:00:00'),
       (2, 'LEI', '09:00:00', '12:00:00', '13:00:00', '16:30:00', '18:00:00', '20:00:00');
alter sequence if exists prison_regime_prison_regime_id_seq restart with 3;

-- =============================================
-- PRISON PAY BAND DATA
-- =============================================
insert into prison_pay_band (prison_pay_band_id, display_sequence, nomis_pay_band, pay_band_alias,
                             pay_band_description, prison_code)
values (1, 1, 1, 'Pay band 1 (lowest)', 'Pay band 1 (lowest)', 'LEI'),
       (2, 2, 2, 'Pay band 2', 'Pay band 2', 'LEI'),
       (3, 3, 3, 'Pay band 3', 'Pay band 3', 'LEI'),
       (4, 4, 4, 'Pay band 4', 'Pay band 4', 'LEI'),
       (5, 5, 5, 'Pay band 5', 'Pay band 5', 'LEI'),
       (6, 6, 6, 'Pay band 6', 'Pay band 6', 'LEI'),
       (7, 7, 7, 'Pay band 7', 'Pay band 7', 'LEI'),
       (8, 8, 8, 'Pay band 8', 'Pay band 8', 'LEI'),
       (9, 9, 9, 'Pay band 9', 'Pay band 9', 'LEI'),
       (10, 10, 10, 'Pay band 10 (highest)', 'Pay band 10 (highest)', 'LEI'),
       (11, 1, 1, 'Low', 'Pay band 1 (Lowest)', 'MDI'),
       (12, 2, 2, 'Medium', 'Pay band 2', 'MDI'),
       (13, 3, 3, 'High', 'Pay band 3 (highest)', 'MDI');
alter sequence if exists event_priority_event_priority_id_seq restart with 14;
