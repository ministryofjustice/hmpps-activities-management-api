package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentCancellationReason

const val CANCELLED_APPOINTMENT_CANCELLATION_REASON_ID = 2L

@Repository
interface AppointmentCancellationReasonRepository : JpaRepository<AppointmentCancellationReason, Long>
