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

  fun removePrisonerFromFutureAppointments(prisonCode: String, prisonerNumber: String, removedTime: LocalDateTime, removalReasonId: Long, removedBy: String) {
    val removalReason = appointmentAttendeeRemovalReasonRepository.findOrThrowNotFound(removalReasonId)
    appointmentInstanceRepository.findByPrisonCodeAndPrisonerNumberFromNow(prisonCode, prisonerNumber)
      .forEach {
        appointmentAttendeeRepository.findById(it.appointmentAttendeeId)
          .ifPresent { attendee ->
            transactionHandler.newSpringTransaction {
              attendee.remove(removedTime, removalReason, removedBy)
            }

            log.info("Removed appointment attendee with id '${it.appointmentAttendeeId}' for prisoner '$prisonerNumber' from appointment with id '${it.appointmentId}'")

            // TODO: Rename to more generic PrisonerRemovedFromFutureAppointments and pass in reason
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

  fun manageAppointmentAttendees(prisonCode: String, daysAfterNow: Long) {
    val removedTime = LocalDateTime.now()
    val removedBy = "MANAGE_APPOINTMENT_SERVICE"

    require(daysAfterNow in 0..60) {
      "Supplied days after now must be at least one day and less than 61 days"
    }

    val regime = prisonRegimeRepository.findByPrisonCode(prisonCode)
    if (regime == null) {
      log.warn("Rolled out prison $prisonCode is missing a prison regime.")
    } else {
      val prisoners = prisonerSearch.findByPrisonerNumbers(getPrisonNumbersForFutureAppointments(prisonCode, daysAfterNow)).block()!!
      log.info("Found ${prisoners.size} prisoners for future appointments in prison code '$prisonCode' taking place within '$daysAfterNow' day(s)")

      val permanentlyReleasedPrisoners = prisoners.permanentlyReleased()
      log.info("Found ${permanentlyReleasedPrisoners.size} prisoners permanently released from prison code '$prisonCode'")

      permanentlyReleasedPrisoners.forEach {
        removePrisonerFromFutureAppointments(prisonCode, it.prisonerNumber, removedTime, PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID, removedBy)
        log.info("Removed prisoner '${it.prisonerNumber}' from future appointments as they have been released")
      }

      val prisonersNotInExpectedPrison = prisoners.notInExpectedPrison(prisonCode)
      log.info("Found ${prisonersNotInExpectedPrison.size} prisoners not in prison code '$prisonCode'")
      val expiredMoves = prisonersNotInExpectedPrison.getExpiredMoves(regime)
      log.info("Found ${expiredMoves.size} prisoners that left prison code '$prisonCode' more than ${regime.maxDaysToExpiry} day(s) ago")

      expiredMoves.forEach {
        removePrisonerFromFutureAppointments(prisonCode, it.key, removedTime, PRISONER_STATUS_PERMANENT_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID, removedBy)
        log.info("Removed prisoner '${it.key}' from future appointments as they left prison code '$prisonCode' more than ${regime.maxDaysToExpiry} day(s) ago")
      }
    }
  }

  private fun getPrisonNumbersForFutureAppointments(prisonCode: String, daysAfterNow: Long) =
    LocalDateRange(LocalDate.now(), LocalDate.now().plusDays(daysAfterNow)).flatMap { date ->
      appointmentRepository.findAllByPrisonCodeAndStartDate(prisonCode, date).flatMap { it.prisonerNumbers() }
    }.distinct()

  private fun List<Prisoner>.permanentlyReleased() = filter { it.isPermanentlyReleased() }

  private fun List<Prisoner>.notInExpectedPrison(prisonCode: String) =
    filterNot { it.isPermanentlyReleased() }
      .filter { prisoner -> prisoner.isOutOfPrison() || prisoner.isAtDifferentLocationTo(prisonCode) }

  private fun List<Prisoner>.getExpiredMoves(regime: PrisonRegime) =
    prisonApi.getMovementsForPrisonersFromPrison(regime.prisonCode, this.map { it.prisonerNumber }.toSet())
      .groupBy { it.offenderNo }.mapValues { it -> it.value.maxBy { it.movementDateTime() } }
      .filter { regime.hasExpired { it.value.movementDate } }
}
