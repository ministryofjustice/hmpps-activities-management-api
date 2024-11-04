package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.junit.jupiter.api.Test
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityState
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.educationCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivitySummary
import java.time.LocalDateTime

class PrisonIntegrationTest : ActivitiesIntegrationTestBase() {
  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `get all activities for a prison`() {
    val activities = webTestClient.getActivities("PVI")!!

    activities.single() isEqualTo
      ActivitySummary(
        id = 1,
        activityName = "Maths Level 1",
        category = educationCategory,
        capacity = 10,
        allocated = 5,
        waitlisted = 1,
        createdTime = LocalDateTime.of(2022, 9, 21, 0, 0, 0),
        activityState = ActivityState.LIVE,
      )
  }
}
