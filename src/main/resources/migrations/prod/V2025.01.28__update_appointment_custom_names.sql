--Update of custom name data after migration
--Set the extra_information field to be the same as the custom_name where it is empty
update appointment a
set extra_information = custom_name
where a.prison_code = 'EYI'
  and a.category_code in ('VLLA', 'VLB', 'VLOO', 'VLPA', 'VLPM')
  and a.custom_name is not null and a.extra_information is null;

update appointment_series as2
set extra_information = custom_name
where as2.prison_code = 'EYI'
  and as2.category_code in ('VLLA', 'VLB', 'VLOO', 'VLPA', 'VLPM')
  and as2.custom_name is not null and as2.extra_information is null;

--Update the custom name to be blank
update appointment a
set custom_name = null
where a.prison_code = 'EYI'
  and a.category_code in ('VLLA', 'VLB', 'VLOO', 'VLPA', 'VLPM')
  and a.custom_name is not null;

update appointment_series as2
set custom_name = null
where as2.prison_code = 'EYI'
  and as2.category_code in ('VLLA', 'VLB', 'VLOO', 'VLPA', 'VLPM')
  and as2.custom_name is not null;
