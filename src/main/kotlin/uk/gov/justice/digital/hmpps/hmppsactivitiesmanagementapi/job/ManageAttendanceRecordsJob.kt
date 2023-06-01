package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AttendanceOperation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAttendancesService

/**
 * This job is responsible for creating daily attendance records and emitting expiry events for 1-day old unmarked attendances.
 *
 * At present, we also create attendance records for suspended schedules but not for cancelled schedules. As we learn
 * more this will likely change the behaviour of this job.
 */
@Component
class ManageAttendanceRecordsJob(private val attendancesService: ManageAttendancesService) {
  @Async("asyncExecutor")
  fun execute(withExpiry: Boolean) {
    attendancesService.attendances(AttendanceOperation.CREATE)
    if (withExpiry) {
      attendancesService.attendances(AttendanceOperation.EXPIRE)
    }
  }
}
