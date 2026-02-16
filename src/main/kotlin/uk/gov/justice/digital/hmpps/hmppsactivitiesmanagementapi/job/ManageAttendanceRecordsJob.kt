package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.ATTENDANCE_CREATE
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
  private val jobRunner: SafeJobRunner,
  private val clock: Clock,
) {
  @Async("asyncExecutor")
  fun execute(mayBePrisonCode: String? = null, date: LocalDate = LocalDate.now(clock), withExpiry: Boolean) {
    // We cannot create future attendance records
    if (date > LocalDate.now(clock)) return

    jobRunner.runDistributedJob(ATTENDANCE_CREATE) { job ->
      manageNewAttendancesService.sendEvents(job, date, mayBePrisonCode, withExpiry)
    }
  }
}
