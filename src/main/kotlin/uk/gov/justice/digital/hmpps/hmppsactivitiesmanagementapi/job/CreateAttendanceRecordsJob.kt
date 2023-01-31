package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AttendancesService
import java.time.LocalDate

/**
 * This job creates attendances on the date of execution for instances scheduled and allocations active on this date.
 *
 * At present, we do also create attendance records for suspended schedules but not for cancelled schedules. As we learn
 * more this will likely change the behaviour of this job.
 */
@Component
class CreateAttendanceRecordsJob(private val attendancesService: AttendancesService) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Async("asyncExecutor")
  fun execute() {
    val today = LocalDate.now()

    attendancesService.createAttendanceRecordsFor(today)
    log.info("Created attendance records for date: $today")
  }
}
