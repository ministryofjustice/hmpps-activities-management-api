package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.Feature
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.ATTENDANCE_CREATE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.ATTENDANCE_EXPIRE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ExpireAttendancesService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageNewAttendancesService
import java.time.Clock
import java.time.LocalDate

/**
 * This job is responsible for creating daily attendance records and emitting expiry events for 1-day old unmarked attendances.
 *
 * At present, we also create attendance records for suspended schedules but not for cancelled schedules. As we learn
 * more this will likely change the behaviour of this job.
 */
@Component
class ManageAttendanceRecordsJob(
  private val manageNewAttendancesService: ManageNewAttendancesService,
  private val expireAttendancesService: ExpireAttendancesService,
  private val jobRunner: SafeJobRunner,
  private val clock: Clock,
  featureSwitches: FeatureSwitches,
) {
  private val sqsEnabled = featureSwitches.isEnabled(Feature.JOBS_SQS_MANAGE_ATTENDANCES_ENABLED)

  @Async("asyncExecutor")
  fun execute(mayBePrisonCode: String? = null, date: LocalDate = LocalDate.now(clock), withExpiry: Boolean) {
    // We cannot create future attendance records
    if (date > LocalDate.now(clock)) return

    if (sqsEnabled) {
      jobRunner.runDistributedJob(ATTENDANCE_CREATE) { job ->
        manageNewAttendancesService.sendEvents(job, date, mayBePrisonCode, withExpiry)
      }
    } else {
      jobRunner.runJobWithRetry(
        jobDefinition = JobDefinition(ATTENDANCE_CREATE) { manageNewAttendancesService.createAttendances(date, mayBePrisonCode) },
      )

      if (withExpiry) {
        jobRunner.runJob(
          JobDefinition(ATTENDANCE_EXPIRE) { expireAttendancesService.expireUnmarkedAttendances() },
        )
      }
    }
  }
}
