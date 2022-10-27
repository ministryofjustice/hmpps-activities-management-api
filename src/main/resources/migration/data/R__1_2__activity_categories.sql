insert into activity_category(activity_category_id, code, description) select 1, 'Education', 'Education classes' where not exists (select 1 from activity_category where activity_category_id = 1);

alter sequence activity_category_activity_category_id_seq restart with 2;