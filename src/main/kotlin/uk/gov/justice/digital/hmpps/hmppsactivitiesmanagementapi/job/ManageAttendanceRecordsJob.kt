package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RolloutPrisonPlan
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAttendancesService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService
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
  private val rolloutPrisonService: RolloutPrisonService,
  private val manageAttendancesService: ManageAttendancesService,
  private val jobRunner: SafeJobRunner,
  private val clock: Clock,
) {
  @Async("asyncExecutor")
  fun execute(mayBePrisonCode: String? = null, date: LocalDate = LocalDate.now(clock), withExpiry: Boolean) {
    // We cannot create future attendance records
    if (date > LocalDate.now(clock)) return

    val rolledOutPrisonCodes = getRolledOutPrisonsForActivities(mayBePrisonCode)

    jobRunner.runJobWithRetry(
      jobDefinition = JobDefinition(
        JobType.ATTENDANCE_CREATE,
      ) {
        rolledOutPrisonCodes.forEach { prisonCode -> manageAttendancesService.createAttendances(date, prisonCode) }
      },
    )

    if (withExpiry) {
      jobRunner.runJob(
        JobDefinition(
          JobType.ATTENDANCE_EXPIRE,
        ) { manageAttendancesService.expireUnmarkedAttendanceRecordsOneDayAfterTheirSession() },
      )
    }
  }

  private fun getRolledOutPrisonsForActivities(mayBePrisonCode: String?) = (
    mayBePrisonCode?.let { listOf(rolloutPrisonService.getByPrisonCode(it)) }
      ?: rolloutPrisonService.getRolloutPrisons()
    ).filter { it.activitiesRolledOut }.map(RolloutPrisonPlan::prisonCode)
}
