package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AttendanceOperation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAttendancesService

class ManageAttendanceRecordsJobTest {
  private val attendancesService: ManageAttendancesService = mock()
  private val job = ManageAttendanceRecordsJob(attendancesService)

  @Test
  fun `desired attendance operations triggered`() {
    job.execute()

    verify(attendancesService).attendances(AttendanceOperation.CREATE)
    verify(attendancesService).attendances(AttendanceOperation.EXPIRE)
  }
}
