package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AppointmentAttendeeRemovalReason

const val PERMANENT_REMOVAL_BY_USER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID = 1L
const val TEMPORARY_REMOVAL_BY_USER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID = 2L
const val CANCEL_ON_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID = 3L
const val PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID = 4L
const val PRISONER_STATUS_PERMANENT_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID = 5L
const val PRISONER_STATUS_TEMPORARY_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID = 6L

@Repository
interface AppointmentAttendeeRemovalReasonRepository : JpaRepository<AppointmentAttendeeRemovalReason, Long>
