package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCategory
import java.time.LocalDate

class PrisonIntegrationTest : IntegrationTestBase() {

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql"
  )
  @Test
  fun `get all activities in a category for a prison`() {
    val activities = webTestClient.getActivitiesForCategory("PVI", 1)

    assertThat(activities).containsExactlyInAnyOrder(
      ActivityLite(
        id = 1,
        prisonCode = "PVI",
        attendanceRequired = true,
        summary = "Maths",
        description = "Maths Level 1",
        category = ActivityCategory(
          id = 1L,
          code = "C1",
          description = "Category 1"
        )
      )
    )
  }

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql"
  )
  @Test
  fun `get all scheduled prison locations for HMP Pentonville on Oct 10th 2022`() {
    val locations = webTestClient.getLocationsPrisonByCode("PVI", LocalDate.of(2022, 10, 10))

    assertThat(locations).containsExactlyInAnyOrder(
      InternalLocation(1, "L1", "Location 1"),
      InternalLocation(2, "L2", "Location 2"),
    )
  }

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql"
  )
  @Test
  fun `get all scheduled prison locations for HMP Pentonville morning of Oct 10th 2022`() {
    val locations =
      webTestClient.getLocationsPrisonByCode("PVI", LocalDate.of(2022, 10, 10), TimeSlot.AM)

    assertThat(locations).containsExactly(InternalLocation(1, "L1", "Location 1"))
  }

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql"
  )
  @Test
  fun `get all scheduled prison locations for HMP Pentonville afternoon of Oct 10th 2022`() {
    val locations =
      webTestClient.getLocationsPrisonByCode("PVI", LocalDate.of(2022, 10, 10), TimeSlot.PM)

    assertThat(locations).containsExactly(InternalLocation(2, "L2", "Location 2"))
  }

  private fun WebTestClient.getActivitiesForCategory(prisonCode: String, categoryId: Long) =
    get()
      .uri("/prison/$prisonCode/activity-categories/$categoryId/activities")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(ActivityLite::class.java)
      .returnResult().responseBody

  private fun WebTestClient.getLocationsPrisonByCode(
    code: String,
    date: LocalDate? = LocalDate.now(),
    timeSlot: TimeSlot? = null
  ) =
    get()
      .uri("/prison/$code/locations?date=$date${timeSlot?.let { "&timeSlot=$it" } ?: ""}")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(InternalLocation::class.java)
      .returnResult().responseBody
}
