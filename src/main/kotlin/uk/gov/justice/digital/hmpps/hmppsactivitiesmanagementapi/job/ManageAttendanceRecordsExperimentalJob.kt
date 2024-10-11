package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RolloutPrisonPlan
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAttendancesExperimentalService
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
class ManageAttendanceRecordsExperimentalJob(
  private val rolloutPrisonService: RolloutPrisonService,
  private val manageExperimentalAttendancesService: ManageAttendancesExperimentalService,
  private val jobRunner: SafeJobRunner,
  private val clock: Clock,
) {
  @Async("asyncExecutor")
  fun execute(mayBePrisonCode: String? = null, date: LocalDate = LocalDate.now(clock)) {
    // We cannot create future attendance records
    if (date > LocalDate.now(clock)) return

    val rolledOutPrisonCodes = getRolledOutPrisonsForActivities(mayBePrisonCode)

    jobRunner.runJobWithRetry(
      jobDefinition = JobDefinition(
        JobType.ATTENDANCE_CREATE_EXPERIMENTAL,
      ) {
        // rolledOutPrisonCodes.forEach { prisonCode -> manageExperimentalAttendancesService.createAttendances(date, prisonCode) }
        manageExperimentalAttendancesService.createAttendances(date.minusDays(1), "IWI")
      },
    )
  }

  private fun getRolledOutPrisonsForActivities(mayBePrisonCode: String?) =
    (
      mayBePrisonCode?.let { listOf(rolloutPrisonService.getByPrisonCode(it)) }
        ?: rolloutPrisonService.getRolloutPrisons()
      ).filter { it.activitiesRolledOut }.map(RolloutPrisonPlan::prisonCode)
}
