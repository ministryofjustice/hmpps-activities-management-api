insert into event_priority (event_priority_id, prison_code, event_type, event_category, priority)
values (1, 'MDI', 'COURT_HEARING', null, 1),
       (2, 'MDI', 'EXTERNAL_TRANSFER', null, 2),
       (3, 'MDI', 'ADJUDICATION_HEARING', null, 3),
       (4, 'MDI', 'VISIT', null, 4),
       (5, 'MDI', 'APPOINTMENT', null, 5),
       (6, 'MDI', 'ACTIVITY', null, 6);

alter sequence event_priority_event_priority_id_seq restart with 7;
