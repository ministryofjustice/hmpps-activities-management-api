package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.isPermanentlyReleased
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.onOrBefore
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentAttendeeRemovalReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
class ManageAppointmentService(
  private val appointmentRepository: AppointmentRepository,
  private val appointmentAttendeeRemovalReasonRepository: AppointmentAttendeeRemovalReasonRepository,
  private val prisonerSearch: PrisonerSearchApiApplicationClient,
  private val transactionHandler: TransactionHandler,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun manageAppointmentAttendees(prisonCode: String, localDateRange: LocalDateRange) {
    require(ChronoUnit.DAYS.between(localDateRange.start, localDateRange.endInclusive) in 0..60) {
      "Supplied date range must be at least one day and less than 61 days"
    }

    val now = LocalDateTime.now()
    val releasedAppointmentAttendeeRemovalReason = appointmentAttendeeRemovalReasonRepository.findOrThrowNotFound(PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID)
    val user = "MANAGE_APPOINTMENT_SERVICE"

    localDateRange.forEach { date ->
      log.info("Removing attendees from appointments in prison code '$prisonCode' scheduled for '$date' where the attendee has been released on or before that date")

      val appointments = appointmentRepository.findAllByPrisonCodeAndStartDate(prisonCode, date)
      log.info("Found ${appointments.size} appointments in prison code '$prisonCode' scheduled for '$date'")

      val prisoners = prisonerSearch.findByPrisonerNumbers(appointments.flatMap { it.prisonerNumbers() }.distinct()).block()!!
      log.info("Found ${prisoners.size} prisoners for appointments in prison code '$prisonCode' scheduled for '$date'")

      val prisonersReleasedBeforeStartDate = prisoners.permanentlyReleaseOnOrBefore(date).map { it.prisonerNumber }
      log.info("Found ${prisonersReleasedBeforeStartDate.size} prisoners permanently released from prison code '$prisonCode' on or before for '$date'")

      if (prisonersReleasedBeforeStartDate.isNotEmpty()) {
        appointments.forEach {
          transactionHandler.newSpringTransaction {
            it.findAttendees(prisonersReleasedBeforeStartDate).forEach {
              it.remove(now, releasedAppointmentAttendeeRemovalReason, user)
              log.info("Removed appointment attendee '${it.appointmentAttendeeId}' from appointment '${it.appointment.appointmentId}' as associated prisoner was released on or before '$date'")
            }
          }
        }
      }
    }
  }

  private fun List<Prisoner>.permanentlyReleaseOnOrBefore(date: LocalDate) = filter { it.isPermanentlyReleased() && it.confirmedReleaseDate?.onOrBefore(date) == true }
}
