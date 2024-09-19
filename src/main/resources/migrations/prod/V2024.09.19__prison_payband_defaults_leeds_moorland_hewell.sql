-- These 3 prisons were previously configured in DEV and so their default paybands must now be set in prod folder instead of common
insert into prison_pay_band (display_sequence, nomis_pay_band, pay_band_alias, pay_band_description, prison_code)
values (1,  1,  'Pay band 1 (Lowest)', 'Pay band 1 (Lowest)', 'LEI'),
       (2,  2,  'Pay band 2', 'Pay band 2', 'LEI'),
       (3,  3,  'Pay band 3', 'Pay band 3', 'LEI'),
       (4,  4,  'Pay band 4', 'Pay band 4', 'LEI'),
       (5,  5,  'Pay band 5', 'Pay band 5', 'LEI'),
       (6,  6,  'Pay band 6', 'Pay band 6', 'LEI'),
       (7,  7,  'Pay band 7', 'Pay band 7', 'LEI'),
       (8,  8,  'Pay band 8', 'Pay band 8', 'LEI'),
       (9,  9,  'Pay band 9', 'Pay band 9', 'LEI'),
       (10, 10, 'Pay band 10 (Highest)', 'Pay band 10 (Highest)', 'LEI');
insert into prison_pay_band (display_sequence, nomis_pay_band, pay_band_alias, pay_band_description, prison_code)
values (1,  1,  'Pay band 1 (Lowest)', 'Pay band 1 (Lowest)', 'MDI'),
       (2,  2,  'Pay band 2', 'Pay band 2', 'MDI'),
       (3,  3,  'Pay band 3', 'Pay band 3', 'MDI'),
       (4,  4,  'Pay band 4', 'Pay band 4', 'MDI'),
       (5,  5,  'Pay band 5', 'Pay band 5', 'MDI'),
       (6,  6,  'Pay band 6', 'Pay band 6', 'MDI'),
       (7,  7,  'Pay band 7', 'Pay band 7', 'MDI'),
       (8,  8,  'Pay band 8', 'Pay band 8', 'MDI'),
       (9,  9,  'Pay band 9', 'Pay band 9', 'MDI'),
       (10, 10, 'Pay band 10 (Highest)', 'Pay band 10 (Highest)', 'MDI');
insert into prison_pay_band (display_sequence, nomis_pay_band, pay_band_alias, pay_band_description, prison_code)
values (1,  1,  'Pay band 1 (Lowest)', 'Pay band 1 (Lowest)', 'HEI'),
       (2,  2,  'Pay band 2', 'Pay band 2', 'HEI'),
       (3,  3,  'Pay band 3', 'Pay band 3', 'HEI'),
       (4,  4,  'Pay band 4', 'Pay band 4', 'HEI'),
       (5,  5,  'Pay band 5', 'Pay band 5', 'HEI'),
       (6,  6,  'Pay band 6', 'Pay band 6', 'HEI'),
       (7,  7,  'Pay band 7', 'Pay band 7', 'HEI'),
       (8,  8,  'Pay band 8', 'Pay band 8', 'HEI'),
       (9,  9,  'Pay band 9', 'Pay band 9', 'HEI'),
       (10, 10, 'Pay band 10 (Highest)', 'Pay band 10 (Highest)', 'HEI');