create index appointment_category_code on appointment(prison_code, start_date, category_code);
create index appointment_custom_name on appointment(prison_code, start_date, custom_name);
create index appointment_category_and_custom on appointment(prison_code, start_date, category_code, custom_name);

