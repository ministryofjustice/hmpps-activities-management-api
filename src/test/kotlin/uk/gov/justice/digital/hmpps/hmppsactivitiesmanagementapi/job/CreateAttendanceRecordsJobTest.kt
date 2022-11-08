package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AttendancesService
import java.time.LocalDate

class CreateAttendanceRecordsJobTest {
  private val attendancesService: AttendancesService = mock()
  private val job = CreateAttendanceRecordsJob(attendancesService)

  @Test
  fun `attendance records creation triggered for today`() {
    job.execute()

    val today = LocalDate.now()
    verify(attendancesService).createAttendanceRecordsFor(today)
  }
}
