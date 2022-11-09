insert into activity_category(activity_category_id, code, description) select 1, 'EDU', 'Education' where not exists (select 1 from activity_category where activity_category_id = 1);
insert into activity_category(activity_category_id, code, description) select 2, 'SERV', 'Services' where not exists (select 1 from activity_category where activity_category_id = 2);
insert into activity_category(activity_category_id, code, description) select 3, 'LEI', 'Leisure and social' where not exists (select 1 from activity_category where activity_category_id = 3);
insert into activity_category(activity_category_id, code, description) select 4, 'INDUC', 'Induction' where not exists (select 1 from activity_category where activity_category_id = 4);

alter sequence activity_category_activity_category_id_seq restart with 5;
