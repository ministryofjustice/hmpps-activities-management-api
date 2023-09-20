
-- Temporarily drop the unique contraints that would be broken
drop index idx_prison_pay_band_prison_code_display_sequence;
drop index idx_prison_pay_band_prison_code_nomis_pay_band;

-- Create pay bands 1-10 for Risley
insert into prison_pay_band (display_sequence, nomis_pay_band, pay_band_alias, pay_band_description, prison_code)
values (1,  1,  'Pay band 1 (Lowest)', 'Pay band 1 (Lowest)', 'RSI'),
       (2,  2,  'Pay band 2', 'Pay band 2', 'RSI'),
       (3,  3,  'Pay band 3', 'Pay band 3', 'RSI'),
       (4,  4,  'Pay band 4', 'Pay band 4', 'RSI'),
       (5,  5,  'Pay band 5', 'Pay band 5', 'RSI'),
       (6,  6,  'Pay band 6', 'Pay band 6', 'RSI'),
       (7,  7,  'Pay band 7', 'Pay band 7', 'RSI'),
       (8,  8,  'Pay band 8', 'Pay band 8', 'RSI'),
       (9,  9,  'Pay band 9', 'Pay band 9', 'RSI'),
       (10, 10, 'Pay band 10 (Highest)', 'Pay band 10 (Highest)', 'RSI');

-- Update activity pay rows which are using the Risley pay band alias `Low` to use new pay band alias '1'
update activity_pay 
set prison_pay_band_id = (select prison_pay_band_id from prison_pay_band where prison_code = 'RSI' and pay_band_alias = 'Pay band 1 (Lowest)')
where prison_pay_band_id = (select prison_pay_band_id from prison_pay_band where prison_code = 'RSI' and pay_band_alias = 'Low');

-- Update activity pay rows which are using the Risley pay band alias `Medium` to use new pay band alias '2'
update activity_pay 
set prison_pay_band_id = (select prison_pay_band_id from prison_pay_band where prison_code = 'RSI' and pay_band_alias = 'Pay band 2')
where prison_pay_band_id = (select prison_pay_band_id from prison_pay_band where prison_code = 'RSI' and pay_band_alias = 'Medium');

-- Update activity pay which are usign the Risley pay band alias `High` to use new pay band alias '3'
update activity_pay 
set prison_pay_band_id = (select prison_pay_band_id from prison_pay_band where prison_code = 'RSI' and pay_band_alias = 'Pay band 3')
where prison_pay_band_id = (select prison_pay_band_id from prison_pay_band where prison_code = 'RSI' and pay_band_alias = 'High');


-- Update allocations using the Risley and pay band alias `Low` to pay band alias '1'
update allocation
set prison_pay_band_id = (select prison_pay_band_id from prison_pay_band where prison_code = 'RSI' and pay_band_alias = 'Pay band 1')
where prison_pay_band_id = (select prison_pay_band_id from prison_pay_band where prison_code = 'RSI' and pay_band_alias = 'Low');

-- Update allocations using the Risley pay band alias `Medium` to pay band alias '2'
update allocation
set prison_pay_band_id = (select prison_pay_band_id from prison_pay_band where prison_code = 'RSI' and pay_band_alias = 'Pay band 2')
where prison_pay_band_id = (select prison_pay_band_id from prison_pay_band where prison_code = 'RSI' and pay_band_alias = 'Medium');

-- Update allocations using the Risley pay band alias `High` to pay band alias '3'
update allocation
set prison_pay_band_id = (select prison_pay_band_id from prison_pay_band where prison_code = 'RSI' and pay_band_alias = 'Pay band 3')
where prison_pay_band_id = (select prison_pay_band_id from prison_pay_band where prison_code = 'RSI' and pay_band_alias = 'High');

-- Remove the old pay bands for Risley
delete from prison_pay_band where prison_code = 'RSI' and pay_band_alias in ('Low', 'Medium', 'High');

-- Reinstate the unique indexes
create unique index idx_prison_pay_band_prison_code_display_sequence ON prison_pay_band USING btree (prison_code, display_sequence);
create unique index idx_prison_pay_band_prison_code_nomis_pay_band ON prison_pay_band USING btree (prison_code, nomis_pay_band);

-- End
