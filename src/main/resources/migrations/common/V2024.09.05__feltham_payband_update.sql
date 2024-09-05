-- Correction to Pay bands for HMP Feltham
update prison_pay_band set pay_band_alias = 'Pay band 2', pay_band_description = 'Pay band 2' where prison_code = 'FMI' and nomis_pay_band = 2;

insert into prison_pay_band (display_sequence, nomis_pay_band, pay_band_alias, pay_band_description, prison_code)
values (3, 3, 'Pay band 3 (Highest)', 'Pay band 3 (Highest)', 'FMI');
