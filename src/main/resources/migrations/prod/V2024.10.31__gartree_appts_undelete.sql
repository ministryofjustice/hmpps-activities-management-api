-- Restores migrated appointments for Gartree that were deleted using the delete-migrated-appts deletion tool
-- identifies future appts that are not Chaplaincy for gartree that were not manually restored and reinstates them

update appointment_series 
set cancelled_by = null, 
	cancelled_time = null, 
	cancellation_start_date = null,
	cancellation_start_time = null,
	updated_time = now(),
	updated_by = 'Migrated Appt Undelete'
where appointment_series_id in (
	select p.appointment_series_id
	from (
		select 
			ase.appointment_series_id, 
			a.appointment_id, 
			apa.appointment_attendee_id, 
			min(ase.start_date) OVER (PARTITION BY apa.prisoner_number, ase.start_time, To_Char(a.start_date, 'DAY')) as firstOccurrence
		from appointment_series ase 
		inner join appointment a on a.appointment_series_id = ase.appointment_series_id 
		inner join appointment_attendee apa on apa.appointment_id = a.appointment_id 
		where ase.prison_code = 'GTI' and ase.is_migrated is true and a.start_date >= to_date('2024-10-30', 'YYYY-MM-DD')
		and ase.category_code != 'CHAP'
	) p 
	where p.firstoccurrence >= to_date('2024-11-04', 'YYYY-MM-DD')
);

update appointment 
set is_deleted = false,
	cancelled_time = null,
	cancellation_reason_id = null,
	cancelled_by = null, 
	updated_time = now(),
	updated_by = 'Migrated Appt Undelete'
where appointment_id in (
	select p.appointment_id
	from (
		select 
			ase.appointment_series_id, 
			a.appointment_id, 
			apa.appointment_attendee_id, 
			min(ase.start_date) OVER (PARTITION BY apa.prisoner_number, ase.start_time, To_Char(a.start_date, 'DAY')) as firstOccurrence
		from appointment_series ase 
		inner join appointment a on a.appointment_series_id = ase.appointment_series_id 
		inner join appointment_attendee apa on apa.appointment_id = a.appointment_id 
		where ase.prison_code = 'GTI' and ase.is_migrated is true and a.start_date >= to_date('2024-10-30', 'YYYY-MM-DD')
		and ase.category_code != 'CHAP'
	) p 
	where p.firstoccurrence >= to_date('2024-11-04', 'YYYY-MM-DD')
);