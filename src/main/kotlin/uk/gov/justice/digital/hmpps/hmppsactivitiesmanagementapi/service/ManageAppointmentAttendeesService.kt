package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.isAtDifferentLocationTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.isOutOfPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.isPermanentlyReleased
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Job
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.JobEventMessage
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.JobsSqsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.ManageAppointmentAttendeesJobEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RolloutPrisonPlan
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.PRISONER_STATUS_PERMANENT_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AppointmentAttendeeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService.Companion.hasExpired
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class ManageAppointmentAttendeesService(
  private val rolloutPrisonService: RolloutPrisonService,
  private val prisonerSearch: PrisonerSearchApiApplicationClient,
  private val appointmentAttendeeService: AppointmentAttendeeService,
  private val appointmentRepository: AppointmentRepository,
  private val prisonApiClient: PrisonApiClient,
  private val jobsSqsService: JobsSqsService,
  private val jobService: JobService,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun manageAttendees(daysAfterNow: Long) {
    // Do not check if prison has been enabled for appointments as it could still have migrated appointments requiring managing
    rolloutPrisonService.getRolloutPrisons().forEach { prison ->
      manageAttendees(prison.prisonCode, daysAfterNow)
    }
  }

  fun sendEvents(job: Job, daysAfterNow: Long) {
    val rolloutPrisons = rolloutPrisonService.getRolloutPrisons()

    log.info("Sending manage appointment attendees events for ${rolloutPrisons.count()} prisons taking place within $daysAfterNow day(s)")

    jobService.initialiseCounts(job.jobId, rolloutPrisons.count())

    // Do not check if prison has been enabled for appointments as it could still have migrated appointments requiring managing
    rolloutPrisons.forEach { prison ->
      val event = JobEventMessage(
        jobId = job.jobId,
        eventType = JobType.MANAGE_APPOINTMENT_ATTENDEES,
        messageAttributes = ManageAppointmentAttendeesJobEvent(prison.prisonCode, daysAfterNow),
      )

      jobsSqsService.sendJobEvent(event)
    }
  }

  @Transactional
  fun handleEvent(jobId: Long, prisonCode: String, daysAfterNow: Long) {
    manageAttendees(prisonCode, daysAfterNow)

    log.debug("Marking manage appointment attendees sub-task complete for $prisonCode")

    jobService.incrementCount(jobId)
  }

  private fun manageAttendees(prisonCode: String, daysAfterNow: Long) {
    val removedTime = LocalDateTime.now()
    val removedBy = "MANAGE_APPOINTMENT_SERVICE"

    require(daysAfterNow in 0..60) {
      "Supplied days after now must be at least one day and less than 61 days"
    }

    val prisonPlan = rolloutPrisonService.getByPrisonCode(code = prisonCode)
    val prisoners = prisonerSearch.findByPrisonerNumbers(getPrisonNumbersForFutureAppointments(prisonCode, daysAfterNow))
    log.info("Found ${prisoners.size} prisoners for future appointments in prison code '$prisonCode' taking place within '$daysAfterNow' day(s)")

    val permanentlyReleasedPrisoners = prisoners.permanentlyReleased()
    log.info("Found ${permanentlyReleasedPrisoners.size} prisoners permanently released from prison code '$prisonCode'")

    permanentlyReleasedPrisoners.forEach {
      appointmentAttendeeService.removePrisonerFromFutureAppointments(
        prisonCode,
        it.prisonerNumber,
        removedTime,
        PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID,
        removedBy,
      )
      log.info("Removed prisoner '${it.prisonerNumber}' from future appointments as they have been released")
    }

    val prisonersNotInExpectedPrison = prisoners.notInExpectedPrison(prisonCode)
    log.info("Found ${prisonersNotInExpectedPrison.size} prisoners not in prison code '$prisonCode'")
    val expiredMoves = prisonersNotInExpectedPrison.getExpiredMoves(prisonPlan)
    log.info("Found ${expiredMoves.size} prisoners that left prison code '$prisonCode' more than ${prisonPlan.maxDaysToExpiry} day(s) ago")

    expiredMoves.forEach {
      appointmentAttendeeService.removePrisonerFromFutureAppointments(
        prisonCode,
        it.key,
        removedTime,
        PRISONER_STATUS_PERMANENT_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID,
        removedBy,
      )
      log.info("Removed prisoner '${it.key}' from future appointments as they left prison code '$prisonCode' more than ${prisonPlan.maxDaysToExpiry} day(s) ago")
    }
  }

  private fun getPrisonNumbersForFutureAppointments(prisonCode: String, daysAfterNow: Long) = LocalDateRange(LocalDate.now(), LocalDate.now().plusDays(daysAfterNow)).flatMap { date ->
    appointmentRepository.findAllByPrisonCodeAndStartDate(prisonCode, date).flatMap { it.prisonerNumbers() }
  }.distinct()

  private fun List<Prisoner>.permanentlyReleased() = filter { it.isPermanentlyReleased() }

  private fun List<Prisoner>.notInExpectedPrison(prisonCode: String) = filterNot { it.isPermanentlyReleased() }
    .filter { prisoner -> prisoner.isOutOfPrison() || prisoner.isAtDifferentLocationTo(prisonCode) }

  private fun List<Prisoner>.getExpiredMoves(prisonPlan: RolloutPrisonPlan) = prisonApiClient.getMovementsForPrisonersFromPrison(prisonPlan.prisonCode, this.map { it.prisonerNumber }.toSet())
    .groupBy { it.offenderNo }.mapValues { it -> it.value.maxBy { it.movementDateTime() } }
    .filter { prisonPlan.hasExpired { it.value.movementDate } }
}
