create or replace view v_prisoner_scheduled_activities as
    select
        si.scheduled_instance_id,
        alloc.allocation_id,
        act.prison_code,
        si.session_date,
        si.start_time,
        si.end_time,
        si.time_slot,
        alloc.prisoner_number,
        alloc.booking_id,
        schedule.internal_location_id,
        schedule.internal_location_code,
        schedule.internal_location_description,
        schedule.description                                                                  as schedule_description,
        act.activity_id,
        category.code                                                                         as activity_category,
        act.summary                                                                           as activity_summary,
        si.cancelled,
        exists (
            select 1
            from planned_suspension ps
            where alloc.allocation_id = ps.allocation_id
            and ps.planned_start_date <= si.session_date
            and (ps.planned_end_date > si.session_date or ps.planned_end_date is null)
        )                                                                                     as suspended,
        act.in_cell,
        act.on_wing,
        act.off_wing,
        exists (
            select 1
            where alloc.prisoner_status = 'AUTO_SUSPENDED'
            and alloc.suspended_time::date <= si.session_date
        )                                                                                     as auto_suspended,
        att.issue_payment                                                                     as issue_payment,
        att.status                                                                            as attendance_status,
        attr.code                                                                             as attendance_reason_code,
        act.paid                                                                              as paid_activity,
        schedule.dps_location_id,
        case when adv_att.advance_attendance_id is not null then true
            else false
        end                                                                                   as possible_advance_attendance
    from scheduled_instance si
        join activity_schedule schedule on
            schedule.activity_schedule_id = si.activity_schedule_id and
            si.session_date >= schedule.start_date and
            (schedule.end_date is null or schedule.end_date >= si.session_date)
        join allocation alloc on
            alloc.activity_schedule_id = si.activity_schedule_id and
            si.session_date >= alloc.start_date and
            (alloc.end_date is null or alloc.end_date >= si.session_date) and
            (alloc.deallocated_time is null or alloc.deallocated_time >= (si.session_date + si.start_time))
         join activity act on
            act.activity_id = schedule.activity_id and
            (act.end_date is null or act.end_date >= si.session_date)
         join activity_category category on
             category.activity_category_id = act.activity_category_id
         left join attendance att on
             si.scheduled_instance_id = att.scheduled_instance_id and
             att.prisoner_number = alloc.prisoner_number
         left join attendance_reason attr on
             attr.attendance_reason_id = att.attendance_reason_id
         left join planned_deallocation pdl on
             alloc.planned_deallocation_id = pdl.planned_deallocation_id
         left join advance_attendance adv_att on
             si.scheduled_instance_id = adv_att.scheduled_instance_id and
             alloc.prisoner_number = adv_att.prisoner_number
    where
        (pdl.planned_date is null or pdl.planned_date < current_date or pdl.planned_date >= si.session_date) and
        trim(to_char(si.session_date, 'DAY')) not in
            (
                select edw.day_of_week from exclusion_days_of_week edw
                join exclusion ex on ex.exclusion_id = edw.exclusion_id
                where
                    alloc.allocation_id = ex.allocation_id
                        and ex.start_date <= si.session_date
                        and (ex.end_date >= si.session_date or ex.end_date is null)
                        and ex.time_slot = si.time_slot
                        and ex.week_number = floor(mod(extract(day from (si.session_date - date_trunc('week', schedule.start_date))), schedule.schedule_weeks * 7) / 7) + 1
          );
