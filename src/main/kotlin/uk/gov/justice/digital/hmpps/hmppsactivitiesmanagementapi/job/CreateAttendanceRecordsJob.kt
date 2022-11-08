package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AttendancesService
import java.time.LocalDate

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
