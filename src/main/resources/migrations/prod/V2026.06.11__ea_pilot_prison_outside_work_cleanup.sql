-- Disable outside work for Buckley Hall:
update activity set outside_work = false where prison_code = 'BCI' and activity_id = 2055;
update activity set outside_work = false where prison_code = 'BCI' and activity_id = 1987;

-- Disable outside work for Stafford:
update activity set outside_work = false where prison_code = 'SFI' and activity_id = 14200;

-- Disable outside work for The Mount:
update activity set outside_work = false where prison_code = 'MTI' and activity_id = 2490;

-- Disable outside work for Guys Marsh:
update activity set outside_work = false where prison_code = 'GMI' and activity_id = 17937;
update activity set outside_work = false where prison_code = 'GMI' and activity_id = 17835;
update activity set outside_work = false where prison_code = 'GMI' and activity_id = 17845;
