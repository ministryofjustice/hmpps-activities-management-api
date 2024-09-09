update activity_schedule set use_prison_regime_time = false where activity_schedule_id in (
select as2.activity_schedule_id  from activity a
join activity_schedule as2 on as2.activity_id = a.activity_id
join activity_schedule_slot ass on ass.activity_schedule_id = as2.activity_schedule_id
join prison_regime pr on pr.prison_code = a.prison_code
where as2.use_prison_regime_time is true and a.prison_code in ('RSI', 'WDI', 'LPI')
 and ((ass.time_slot = 'ED' and (ass.start_time != pr.ed_start or ass.end_time != pr.ed_finish) )
 or (ass.time_slot = 'PM' and (ass.start_time != pr.pm_start  or ass.end_time != pr.pm_finish) )
 or (ass.time_slot = 'AM' and (ass.start_time != pr.am_start  or ass.end_time != pr.am_finish) )
 ));