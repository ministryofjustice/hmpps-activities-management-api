-- This fixes the data where we missed updating booking ID's on the back of an offender merge.
update allocation set booking_id = 2866250 where prisoner_number = 'A7158AF' and booking_id != 2866250;

update event_review set booking_id = 2866250 where prisoner_number = 'A7158AF' and booking_id != 2866250;

update appointment_attendee set booking_id = 2866250 where prisoner_number = 'A7158AF' and booking_id != 2866250;
