insert into event_priority (event_priority_id, prison_code, event_type, event_category, priority)
values (1, 'MDI', 'COURT_HEARING', null, 1),
       (2, 'MDI', 'ADJUDICATION_HEARING', null, 2),
       (3, 'MDI', 'VISIT', null, 3),
       (4, 'MDI', 'APPOINTMENT', null, 4),
       (5, 'MDI', 'ACTIVITY', null, 5);

alter sequence event_priority_event_priority_id_seq restart with 6;
