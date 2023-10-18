package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentCancellationReason

const val CREATED_IN_ERROR_APPOINTMENT_CANCELLATION_REASON_ID = 1L
const val CANCELLED_APPOINTMENT_CANCELLATION_REASON_ID = 2L
const val DELETE_MIGRATED_APPOINTMENT_CANCELLATION_REASON_ID = 3L

@Repository
interface AppointmentCancellationReasonRepository : JpaRepository<AppointmentCancellationReason, Long>
