package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AttendanceOperation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAttendancesService

class ManageAttendanceRecordsJobTest {
  private val attendancesService: ManageAttendancesService = mock()
  private val job = ManageAttendanceRecordsJob(attendancesService)

  @Test
  fun `attendance operations triggered - without expiry`() {
    job.execute(withExpiry = false)
    verify(attendancesService).attendances(AttendanceOperation.CREATE)
    verify(attendancesService, never()).attendances(AttendanceOperation.EXPIRE)
  }

  @Test
  fun `attendance operations triggered - with expiry`() {
    job.execute(withExpiry = true)
    verify(attendancesService).attendances(AttendanceOperation.CREATE)
    verify(attendancesService).attendances(AttendanceOperation.EXPIRE)
  }
}
