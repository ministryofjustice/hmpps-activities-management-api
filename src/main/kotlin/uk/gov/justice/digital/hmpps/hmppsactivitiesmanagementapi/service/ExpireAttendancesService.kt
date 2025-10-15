package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Job
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.JobEventMessage
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.JobsSqsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.PrisonCodeJobEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService
import java.time.Clock
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

@Service
class ExpireAttendancesService(
  private val rolloutPrisonService: RolloutPrisonService,
  private val attendanceRepository: AttendanceRepository,
  private val jobsSqsService: JobsSqsService,
  private val jobService: JobService,
  private val outboundEventsService: OutboundEventsService,
  private val clock: Clock,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun expireUnmarkedAttendances() {
    rolloutPrisonService.getRolloutPrisons().forEach { expireUnmarkedAttendanceRecordsOneDayAfterTheirSession(it.prisonCode) }
  }

  fun sendEvents(job: Job) {
    val rolloutPrisons = rolloutPrisonService.getRolloutPrisons()

    log.info("Sending expire attendances job events for ${rolloutPrisons.count()} prisons")

    jobService.initialiseCounts(job.jobId, rolloutPrisons.count())

    rolloutPrisons.forEach { prison ->
      val event = JobEventMessage(
        jobId = job.jobId,
        eventType = JobType.ATTENDANCE_EXPIRE,
        messageAttributes = PrisonCodeJobEvent(prison.prisonCode),
      )

      jobsSqsService.sendJobEvent(event)
    }
  }

  fun handleEvent(jobId: Long, prisonCode: String) {
    expireUnmarkedAttendanceRecordsOneDayAfterTheirSession(prisonCode)

    log.debug("Marking expire attendances sub-task complete for $prisonCode")

    jobService.incrementCount(jobId)
  }

  /**
   * This makes no local changes - it ONLY fires sync events to replicate the NOMIS behaviour
   * which expires attendances at the end of the day and sets the internal movement status to 'EXP'.
   */
  private fun expireUnmarkedAttendanceRecordsOneDayAfterTheirSession(prisonCode: String) {
    log.info("Expiring WAITING attendances from yesterday.")

    LocalDate.now(clock).minusDays(1).let { yesterday ->

      val counter = AtomicInteger(0)

      attendanceRepository.findWaitingAttendancesOnDate(prisonCode, yesterday)
        .forEach { waitingAttendance ->
          runCatching {
            outboundEventsService.send(OutboundEvent.PRISONER_ATTENDANCE_EXPIRED, waitingAttendance.attendanceId)
          }.onFailure {
            log.error("Failed to send expire event for attendance ID ${waitingAttendance.attendanceId}", it)
          }.onSuccess {
            counter.incrementAndGet()
          }
        }

      log.info("${counter.get()} attendance record(s) expired for $prisonCode")
    }
  }
}
