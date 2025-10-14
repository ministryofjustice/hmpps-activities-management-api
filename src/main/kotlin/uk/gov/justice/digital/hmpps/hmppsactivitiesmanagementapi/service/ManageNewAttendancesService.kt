package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Job
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.JobEventMessage
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.JobsSqsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.NewActivityAttendanceJobEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.SafeJobRunner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RolloutPrisonPlan
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService
import java.time.LocalDate

@Service
class ManageNewAttendancesService(
  private val rolloutPrisonService: RolloutPrisonService,
  private val manageAttendancesService: ManageAttendancesService,
  private val expireAttendancesService: ExpireAttendancesService,
  private val jobsSqsService: JobsSqsService,
  private val jobService: JobService,
  private val jobRunner: SafeJobRunner,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun createAttendances(date: LocalDate, mayBePrisonCode: String? = null) {
    getRolledOutPrisonsForActivities(mayBePrisonCode).forEach { manageAttendancesService.createAttendances(date, it) }
  }

  fun sendEvents(job: Job, date: LocalDate, mayBePrisonCode: String? = null, expireUnmarkedAttendances: Boolean = false) {
    val prisonCodes = getRolledOutPrisonsForActivities(mayBePrisonCode)

    log.info("Sending create activity attendances events for ${prisonCodes.count()} prisons")

    jobService.initialiseCounts(job.jobId, prisonCodes.count())

    prisonCodes.forEach { prisonCode ->
      val event = JobEventMessage(
        jobId = job.jobId,
        eventType = JobType.ATTENDANCE_CREATE,
        messageAttributes = NewActivityAttendanceJobEvent(prisonCode, date, expireUnmarkedAttendances),
      )

      jobsSqsService.sendJobEvent(event)
    }
  }

  fun handleEvent(jobId: Long, prisonCode: String, date: LocalDate, expireUnmarkedAttendances: Boolean = false) {
    manageAttendancesService.createAttendances(date, prisonCode)

    log.debug("Marking create activity attendances sub-task complete for $prisonCode")

    if (jobService.incrementCount(jobId) && expireUnmarkedAttendances) {
      log.info("Starting expire attendances")
      jobRunner.runDistributedJob(JobType.ATTENDANCE_EXPIRE, expireAttendancesService::sendEvents)
    }
  }

  private fun getRolledOutPrisonsForActivities(maybePrisonCode: String?) = (
    maybePrisonCode?.let { listOf(rolloutPrisonService.getByPrisonCode(it)) }
      ?: rolloutPrisonService.getRolloutPrisons()
    ).filter { it.activitiesRolledOut }.map(RolloutPrisonPlan::prisonCode)
}
