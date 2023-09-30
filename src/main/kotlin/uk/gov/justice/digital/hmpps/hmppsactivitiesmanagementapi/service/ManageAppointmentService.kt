package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.isPermanentlyReleased
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.onOrBefore
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentAttendeeRemovalReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import java.time.LocalDateTime

@Service
@Transactional
class ManageAppointmentService(
  private val appointmentRepository: AppointmentRepository,
  private val appointmentAttendeeRemovalReasonRepository: AppointmentAttendeeRemovalReasonRepository,
  private val prisonerSearch: PrisonerSearchApiApplicationClient,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun manageAppointmentAttendees(localDateRange: LocalDateRange) {
    val now = LocalDateTime.now()
    val releasedAppointmentAttendeeRemovalReason = appointmentAttendeeRemovalReasonRepository.findOrThrowNotFound(PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID)
    val user = "MANAGE_APPOINTMENT_SERVICE"

    localDateRange.forEach { startDate ->
      val appointments = appointmentRepository.findAllByStartDate(startDate)

      val prisoners = prisonerSearch.findByPrisonerNumbers(appointments.flatMap { it.prisonerNumbers() }.distinct()).block()!!

      val prisonersReleasedBeforeStartDate = prisoners.filter { it.isPermanentlyReleased() && it.confirmedReleaseDate?.onOrBefore(startDate) == true }.associateBy { it.prisonerNumber }

      appointments.flatMap { it.attendees() }.filter { prisonersReleasedBeforeStartDate.containsKey(it.prisonerNumber) }.forEach {
        it.remove(now, releasedAppointmentAttendeeRemovalReason, user)
        log.info("Removed appointment attendee '${it.appointmentAttendeeId}' from appointment '${it.appointment.appointmentId} as associated prisoner was released on or before '$startDate'")
      }
    }
  }
}
