insert into activity_category(activity_category_id, code, description) select 1, 'Education', 'Education classes' where not exists (select 1 from activity_category where activity_category_id = 1);
