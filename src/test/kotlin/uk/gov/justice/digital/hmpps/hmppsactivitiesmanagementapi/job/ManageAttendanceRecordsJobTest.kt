package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Job
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.ATTENDANCE_CREATE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageNewAttendancesService
import java.time.Clock
import java.time.LocalDate

class ManageAttendanceRecordsJobTest : JobsTestBase() {

  private val manageNewAttendancesService: ManageNewAttendancesService = mock()

  private lateinit var job: ManageAttendanceRecordsJob

  private val captor = argumentCaptor<Job>()

  @BeforeEach
  fun setUp() {
    job = ManageAttendanceRecordsJob(
      manageNewAttendancesService,
      safeJobRunner,
      Clock.systemDefaultZone(),
    )
  }

  @Test
  fun `attendance operations triggered for single prison`() {
    mockJobs(ATTENDANCE_CREATE)

    job.execute(mayBePrisonCode = PENTONVILLE_PRISON_CODE, withExpiry = true)

    verify(safeJobRunner).runDistributedJob(eq(ATTENDANCE_CREATE), any())
    verify(manageNewAttendancesService).sendEvents(captor.capture(), eq(LocalDate.now()), eq(PENTONVILLE_PRISON_CODE), eq(true))
    assertThat(captor.firstValue.jobType).isEqualTo(ATTENDANCE_CREATE)

    verifyNoMoreInteractions(safeJobRunner)
    verifyNoMoreInteractions(manageNewAttendancesService)
  }
}
