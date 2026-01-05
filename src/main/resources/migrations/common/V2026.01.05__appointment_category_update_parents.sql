update appointment_category cat
    set appointment_parent_category_id = pcat.appointment_parent_category_id
    from appointment_parent_category pcat
    where pcat.name = 'Interventions'
    and cat.code in ('SMS', 'PROG_SESS');

update appointment_category cat
    set appointment_parent_category_id = pcat.appointment_parent_category_id
    from appointment_parent_category pcat
    where pcat.name = 'Other'
      and cat.code = 'PA';

update appointment_category cat
    set appointment_parent_category_id = pcat.appointment_parent_category_id
    from appointment_parent_category pcat
    where pcat.name = 'Education'
      and cat.code = 'GYMPE';
