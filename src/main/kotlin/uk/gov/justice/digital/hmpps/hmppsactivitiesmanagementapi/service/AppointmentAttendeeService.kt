package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.isAtDifferentLocationTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.isOutOfPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.isPermanentlyReleased
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.onOrBefore
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentCancelledOnTransferEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentAttendeeRemovalReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentAttendeeRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PRISONER_STATUS_PERMANENT_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonRegimeRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
@Transactional
class AppointmentAttendeeService(
  private val appointmentRepository: AppointmentRepository,
  private val appointmentAttendeeRepository: AppointmentAttendeeRepository,
  private val appointmentInstanceRepository: AppointmentInstanceRepository,
  private val appointmentAttendeeRemovalReasonRepository: AppointmentAttendeeRemovalReasonRepository,
  private val prisonRegimeRepository: PrisonRegimeRepository,
  private val prisonerSearch: PrisonerSearchApiApplicationClient,
  private val prisonApi: PrisonApiApplicationClient,
  private val transactionHandler: TransactionHandler,
  private val auditService: AuditService,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun removePrisonerFromFutureAppointments(prisonCode: String, prisonerNumber: String, removalReasonId: Long, removedTime: LocalDateTime, removedBy: String) {
    val removalReason = appointmentAttendeeRemovalReasonRepository.findOrThrowNotFound(removalReasonId)
    appointmentInstanceRepository.findByPrisonCodeAndPrisonerNumberFromNow(prisonCode, prisonerNumber)
      .forEach {
        appointmentAttendeeRepository.findById(it.appointmentAttendeeId)
          .ifPresent { attendee ->
            transactionHandler.newSpringTransaction {
              attendee.remove(removedTime, removalReason, removedBy)
            }

            log.info("Removed appointment attendee with id '${it.appointmentAttendeeId}' for prisoner '$prisonerNumber' from appointment with id '${it.appointmentId}'")

            auditService.logEvent(
              AppointmentCancelledOnTransferEvent(
                appointmentSeriesId = it.appointmentSeriesId,
                appointmentId = it.appointmentId,
                prisonCode = it.prisonCode,
                prisonerNumber = it.prisonerNumber,
                createdAt = removedTime,
              ),
            )
          }
      }
  }

  fun manageAppointmentAttendees(prisonCode: String, localDateRange: LocalDateRange) {
    require(ChronoUnit.DAYS.between(localDateRange.start, localDateRange.endInclusive) in 0..60) {
      "Supplied date range must be at least one day and less than 61 days"
    }

    val regime = prisonRegimeRepository.findByPrisonCode(prisonCode)
    require(regime != null) {
      "Rolled out prison $prisonCode is missing a prison regime."
    }

    val removedTime = LocalDateTime.now()
    val removedBy = "MANAGE_APPOINTMENT_SERVICE"

    localDateRange.forEach { date ->
      log.info("Removing attendees from appointments in prison code '$prisonCode' scheduled for '$date' where the attendee has been released on or before that date")

      val appointments = appointmentRepository.findAllByPrisonCodeAndStartDate(prisonCode, date)
      log.info("Found ${appointments.size} appointments in prison code '$prisonCode' scheduled for '$date'")

      val prisoners = prisonerSearch.findByPrisonerNumbers(appointments.flatMap { it.prisonerNumbers() }.distinct()).block()!!
      log.info("Found ${prisoners.size} prisoners for appointments in prison code '$prisonCode' scheduled for '$date'")

      val prisonersReleasedBeforeStartDate = prisoners.permanentlyReleaseOnOrBefore(date)
      log.info("Found ${prisonersReleasedBeforeStartDate.size} prisoners permanently released from prison code '$prisonCode' on or before for '$date'")

      prisonersReleasedBeforeStartDate.forEach {
        removePrisonerFromFutureAppointments(prisonCode, it.prisonerNumber, PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID, removedTime, removedBy)
        log.info("Removed prisoner '${it.prisonerNumber}' from future appointments as they were released on or before '$date'")
      }

      val prisonersNotInExpectedPrison = prisoners.notInExpectedPrison(prisonCode)
      log.info("Found ${prisonersNotInExpectedPrison.size} prisoners not in prison code '$prisonCode'")
      val expiredMoves = prisonersNotInExpectedPrison.getExpiredMoves(regime)
      log.info("Found ${expiredMoves.size} prisoners not in prison code '$prisonCode' long enough to be considered permanently transferred")

      expiredMoves.forEach {
        removePrisonerFromFutureAppointments(prisonCode, it.key, PRISONER_STATUS_PERMANENT_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID, removedTime, removedBy)
        log.info("Removed prisoner '${it.key}' from future appointments as they were not in prison code '$prisonCode' long enough to be considered permanently transferred")
      }
    }
  }

  private fun List<Prisoner>.permanentlyReleaseOnOrBefore(date: LocalDate) =
    filter { it.isPermanentlyReleased() && it.confirmedReleaseDate?.onOrBefore(date) == true }

  private fun List<Prisoner>.notInExpectedPrison(prisonCode: String) =
    filter { prisoner -> prisoner.isOutOfPrison() || prisoner.isAtDifferentLocationTo(prisonCode) }

  private fun List<Prisoner>.getExpiredMoves(regime: PrisonRegime) =
    prisonApi.getMovementsForPrisonersFromPrison(regime.prisonCode, this.map { it.prisonerNumber }.toSet())
      .groupBy { it.offenderNo }.mapValues { it -> it.value.maxBy { it.movementDateTime() } }
      .filter { regime.hasExpired { it.value.movementDate } }
}
