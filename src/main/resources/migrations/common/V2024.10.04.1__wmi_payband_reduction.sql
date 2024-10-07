-- Reduce WMI paybands from 10 to just 4. Rename 4 so its marked as highest.

DELETE FROM prison_pay_band where prison_code = 'WMI' and nomis_pay_band > 4;

update prison_pay_band 
set
    pay_band_alias = 'Pay band 4 (Highest)',
    pay_band_description = 'Pay band 4 (Highest)'
where prison_code = 'WMI' and nomis_pay_band = 4;