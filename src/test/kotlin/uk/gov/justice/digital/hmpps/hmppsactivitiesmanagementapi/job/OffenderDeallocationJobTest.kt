package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.OffenderDeallocationService

class OffenderDeallocationJobTest {
  private val offenderDeallocationService: OffenderDeallocationService = mock()
  private val job = OffenderDeallocationJob(offenderDeallocationService)

  @Test
  fun `attendance records creation triggered for today`() {
    job.execute()

    verify(offenderDeallocationService).deallocateOffendersWhenEndDatesReached()
  }
}
