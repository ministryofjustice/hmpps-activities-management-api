package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityState
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.educationCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityMinimumEducationLevel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PayPerSession
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivitySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import java.time.LocalDateTime

class PrisonIntegrationTest : IntegrationTestBase() {

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `get all activities in a category for a prison`() {
    val activities = webTestClient.getActivitiesForCategory("PVI", 1)!!

    activities.single() isEqualTo
      ActivityLite(
        id = 1,
        prisonCode = "PVI",
        attendanceRequired = true,
        inCell = false,
        onWing = false,
        offWing = false,
        pieceWork = false,
        outsideWork = false,
        payPerSession = PayPerSession.H,
        summary = "Maths",
        description = "Maths Level 1",
        riskLevel = "high",
        minimumEducationLevel = listOf(
          ActivityMinimumEducationLevel(
            id = 1,
            educationLevelCode = "1",
            educationLevelDescription = "Reading Measure 1.0",
            studyAreaCode = "ENGLA",
            studyAreaDescription = "English Language",
          ),
        ),
        category = educationCategory,
        capacity = 20,
        allocated = 5,
        createdTime = LocalDateTime.of(2022, 9, 21, 0, 0, 0),
        activityState = ActivityState.LIVE,
        paid = true,
      )
  }

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

  private fun WebTestClient.getActivitiesForCategory(prisonCode: String, categoryId: Long) =
    get()
      .uri("/prison/$prisonCode/activity-categories/$categoryId/activities")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(ActivityLite::class.java)
      .returnResult().responseBody

  private fun WebTestClient.getActivities(prisonCode: String) =
    get()
      .uri("/prison/$prisonCode/activities")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(ActivitySummary::class.java)
      .returnResult().responseBody
}
