package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.FIX_ACTIVITY_ROTL_CATEGORY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivitiesFixRotlCategoryService

class ActivitiesFixRotlCategoryJobTest : JobsTestBase() {
  private val activitiesFixRotlCategoryService: ActivitiesFixRotlCategoryService = mock()
  private val jobDefinitionCaptor = argumentCaptor<JobDefinition>()

  private val job = ActivitiesFixRotlCategoryJob(
    activitiesFixRotlCategoryService,
    safeJobRunner,
  )

  @Test
  fun `activities fix ROTL category job triggered`() {
    mockJobs(FIX_ACTIVITY_ROTL_CATEGORY)

    job.execute()

    verify(safeJobRunner).runJob(jobDefinitionCaptor.capture())

    assertThat(jobDefinitionCaptor.firstValue.jobType).isEqualTo(FIX_ACTIVITY_ROTL_CATEGORY)

    verify(activitiesFixRotlCategoryService).fixCategoriesAndLocations()
  }
}
