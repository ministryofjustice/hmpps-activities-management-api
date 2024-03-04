--
-- This corrects those dellocations that are incorrectly ended without a reference back to the planned deallocation.
--
UPDATE allocation a
SET
    deallocated_reason = pd.planned_reason,
    deallocated_by = pd.planned_by,
    end_date = pd.planned_date,
    deallocation_case_note_id = pd.case_note_id
FROM planned_deallocation pd
WHERE
    a.planned_deallocation_id = pd.planned_deallocation_id AND
    a.allocation_id IN (
        select alloc.allocation_id from allocation alloc
                                            join planned_deallocation pd on alloc.allocation_id = pd.allocation_id
                                            join activity_schedule _as on alloc.activity_schedule_id = _as.activity_schedule_id
        where alloc.prisoner_status = 'ENDED'
          and alloc.deallocated_reason = 'ENDED'
          and alloc.end_date > pd.planned_date
          and (_as.end_date is null or _as.end_date >= pd.planned_date)
    )