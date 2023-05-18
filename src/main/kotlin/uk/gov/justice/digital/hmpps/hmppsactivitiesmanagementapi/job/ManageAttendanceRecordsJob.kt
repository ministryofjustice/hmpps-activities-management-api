package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AttendanceOperation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAttendancesService

/**
 * This job is responsible for creating (new) daily attendance records and locking existing (old) attendance records.
 *
 * At present, we do also create attendance records for suspended schedules but not for cancelled schedules. As we learn
 * more this will likely change the behaviour of this job.
 */
@Component
class ManageAttendanceRecordsJob(private val attendancesService: ManageAttendancesService) {
  @Async("asyncExecutor")
  fun execute() {
    attendancesService.attendances(AttendanceOperation.CREATE)
    attendancesService.attendances(AttendanceOperation.LOCK)
  }
}
