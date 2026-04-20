package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.junit.jupiter.api.Test
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityState
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.educationCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.industriesCategory
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
        outsideWork = false,
        activityState = ActivityState.LIVE,
      )
  }

  @Sql(
    "classpath:test_data/seed-activity-id-3.sql",
  )
  @Test
  fun `get activities for a prison when filtered by a search term`() {
    val activities = webTestClient.getActivities("PVI", nameSearch = "enGliSh")!!

    activities.single() isEqualTo
      ActivitySummary(
        id = 4,
        activityName = "English Level 2",
        category = industriesCategory,
        capacity = 10,
        allocated = 4,
        waitlisted = 0,
        createdTime = LocalDateTime.of(2022, 9, 21, 0, 0, 0),
        outsideWork = false,
        activityState = ActivityState.LIVE,
      )
  }

  @Sql("classpath:test_data/seed-activity-archived.sql")
  @Test
  fun `activity is ARCHIVED when the end date is before today`() {
    val activities = webTestClient.getActivities("PVI", excludeArchived = false)!!

    val archivedActivity = activities.single { it.activityName == "Maths Level 1" }
    archivedActivity.activityState isEqualTo ActivityState.ARCHIVED
  }

  @Sql("classpath:test_data/seed-activity-archived.sql")
  @Test
  fun `activity is LIVE when the end date is today`() {
    val activities = webTestClient.getActivities("PVI")!!

    val liveActivity = activities.single { it.activityName == "English Level 1" }
    liveActivity.activityState isEqualTo ActivityState.LIVE
  }

  @Sql("classpath:test_data/seed-activity-archived.sql")
  @Test
  fun `activity is LIVE when the end date is in the future`() {
    val activities = webTestClient.getActivities("PVI")!!

    val liveActivity = activities.single { it.activityName == "History Level 1" }
    liveActivity.activityState isEqualTo ActivityState.LIVE
  }

  @Sql("classpath:test_data/seed-activity-archived.sql")
  @Test
  fun `activity is LIVE when there is no end date`() {
    val activities = webTestClient.getActivities("PVI")!!

    val liveActivity = activities.single { it.activityName == "Science Level 1" }
    liveActivity.activityState isEqualTo ActivityState.LIVE
  }
}
