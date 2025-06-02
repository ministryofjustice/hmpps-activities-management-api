create or replace view v_attendance_creation_data as
    select
        gen_random_uuid() as id,
        data.*
    from (
        select distinct
            si.scheduled_instance_id,
            si.session_date,
            si.time_slot,
            alloc.allocation_id,
            alloc.prisoner_number,
            alloc.prisoner_status,
            alloc.prison_pay_band_id,
            alloc.start_date                                                as alloc_start,
            alloc.end_date                                                  as alloc_end,
            as1.start_date                                                  as schedule_start,
            as1.end_date                                                    as schedule_end,
            as1.schedule_weeks,
            si.cancelled_reason,
            si.cancelled_by,
            case when e.exclusion_id is not null then true
                else false
            end                                                             as possible_exclusion,
            a.prison_code,
            a.paid,
            a.activity_id,
            pd.planned_date                                                 as planned_deallocation_date,
            case when adv_att.advance_attendance_id is not null then true
               else false
            end                                                             as possible_advance_attendance

            from scheduled_instance si
                join activity_schedule as1 on as1.activity_schedule_id = si.activity_schedule_id
                join activity a on a.activity_id = as1.activity_id
                join allocation alloc on as1.activity_schedule_id = alloc.activity_schedule_id
                left join exclusion e on alloc.allocation_id = e.allocation_id
                left join planned_deallocation pd on alloc.planned_deallocation_id = pd.planned_deallocation_id
                left join advance_attendance adv_att on si.scheduled_instance_id = adv_att.scheduled_instance_id and alloc.prisoner_number = adv_att.prisoner_number
             where alloc.prisoner_status <> 'ENDED'
                and not exists (select 1 from attendance a2 where a2.scheduled_instance_id = si.scheduled_instance_id and a2.prisoner_number = alloc.prisoner_number)
     ) as data