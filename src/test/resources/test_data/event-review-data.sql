insert into event_review (event_review_id, service_identifier, event_type, event_time, prison_code, prisoner_number, booking_id, event_data, event_description)
values (1, 'myservice', 'event-name', '2023-5-10 10:20:00', 'MDI', 'G1234DX', 1, 'aaaaaaa', null);

insert into event_review (event_review_id, service_identifier, event_type, event_time, prison_code, prisoner_number, booking_id, event_data, event_description)
values (2, 'myservice', 'event-name', '2023-5-10 10:21:00', 'MDI', 'G1234DY', 1, 'aaaaaaa', 'TEMPORARY_RELEASE');

insert into event_review (event_review_id, service_identifier, event_type, event_time, prison_code, prisoner_number, booking_id, event_data)
values (3, 'myservice', 'event-name', '2023-5-10 10:22:00', 'MDI', 'G1234DD', 1, 'aaaaaaa');

insert into event_review (event_review_id, service_identifier, event_type, event_time, prison_code, prisoner_number, booking_id, event_data)
values (4, 'myservice', 'event-name', '2023-5-10 10:23:00', 'MDI', 'G1234DD', 1, 'aaaaaaa');

insert into event_review (event_review_id, service_identifier, event_type, event_time, prison_code, prisoner_number, booking_id, event_data)
values (5, 'myservice', 'event-name', '2023-5-10 10:24:00', 'MDI', 'G1234DD', 1, 'aaaaaaa');

insert into event_review (event_review_id, service_identifier, event_type, event_time, prison_code, prisoner_number,booking_id,	event_data)
values (6, 'myservice', 'event-name', '2023-5-10 10:25:00', 'MDI', 'G1234DD', 1, 'aaaaaaa');

insert into event_review (event_review_id, service_identifier, event_type, event_time, prison_code, prisoner_number, booking_id, event_data)
values (7, 'myservice', 'event-name', '2023-5-10 10:26:00', 'MDI', 'G1234DD', 1, 'aaaaaaa');

-- Different prisoner below
insert into event_review (event_review_id, service_identifier, event_type, event_time, prison_code, prisoner_number, booking_id, event_data)
values (8, 'myservice', 'event-name', '2023-5-10 10:27:00', 'MDI', 'A1234AA', 1, 'aaaaaaa');

insert into event_review (event_review_id, service_identifier, event_type, event_time, prison_code, prisoner_number, booking_id, event_data)
values (9, 'myservice', 'event-name', '2023-5-10 10:28:00', 'MDI', 'A1234AA', 1, 'aaaaaaa');

insert into event_review (event_review_id, service_identifier, event_type, event_time, prison_code, prisoner_number, booking_id, event_data)
values (10, 'myservice', 'event-name', '2023-5-10 10:29:00', 'MDI', 'A1234AA', 1, 'aaaaaaa');

insert into event_review (event_review_id, service_identifier, event_type, event_time, prison_code, prisoner_number, booking_id, event_data)
values (11, 'myservice', 'event-name', '2023-5-10 10:30:00', 'MDI', 'A1234AA', 1, 'aaaaaaa');

insert into event_review (event_review_id, service_identifier, event_type, event_time, prison_code, prisoner_number, booking_id, event_data)
values (12, 'myservice', 'event-name', '2023-5-10 10:31:00', 'MDI', 'A1234AA', 1, 'aaaaaaa');

-- ACKNOWLEDGED
insert into event_review (event_review_id, service_identifier, event_type, event_time, prison_code, prisoner_number, booking_id, event_data, acknowledged_time, acknowledged_by)
values (13, 'myservice', 'event-name', '2023-5-10 10:32:00', 'MDI', 'A1234AA', 1, 'aaaaaaa', '2023-5-11 10:29:00', 'YYY');

-- Different date
insert into event_review (event_review_id, service_identifier, event_type, event_time, prison_code, prisoner_number, booking_id, event_data)
values (14, 'myservice', 'event-name', '2023-5-11 11:30:01', 'MDI', 'G1234DD', 1, 'aaaaaaa');

-- Different date
insert into event_review (event_review_id, service_identifier, event_type, event_time, prison_code, prisoner_number, booking_id, event_data)
values (15, 'myservice', 'event-name', '2023-5-11 11:31:02', 'MDI', 'G1234DD', 1, 'aaaaaaa');
