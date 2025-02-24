package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.FIX_ACTIVITY_LOCATIONS
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivitiesFixLocationsService

class ActivitiesFixLocationsJobTest : JobsTestBase() {
  private val activitiesFixLocationsService: ActivitiesFixLocationsService = mock()
  private val jobDefinitionCaptor = argumentCaptor<JobDefinition>()

  private val job = ActivitiesFixLocationsJob(activitiesFixLocationsService, safeJobRunner)

  @Test
  fun `fix activities locations operation triggered`() {
    mockJobs(FIX_ACTIVITY_LOCATIONS)

    job.execute()

    verify(safeJobRunner).runJob(jobDefinitionCaptor.capture())

    assertThat(jobDefinitionCaptor.firstValue.jobType).isEqualTo(FIX_ACTIVITY_LOCATIONS)

    verify(activitiesFixLocationsService).fixActivityLocations()
  }
}
