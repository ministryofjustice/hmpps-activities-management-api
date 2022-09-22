package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityPrisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySession
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityTier
import java.time.LocalDate
import java.time.LocalDateTime

class ActivityIntegrationTest : IntegrationTestBase() {

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql"
  )
  @Test
  fun `get maths activity with morning and afternoon sessions`() {
    val activity = webTestClient.getActivityById(1)!!

    with(activity) {
      assertThat(prisonCode).isEqualTo("PVI")
      assertThat(summary).isEqualTo("Maths")
      assertThat(description).isEqualTo("Maths Level 1")
      assertThat(category).isEqualTo(ActivityCategory(1, "C1", "Category 1"))
      assertThat(tier).isEqualTo(ActivityTier(1, "T1", "Tier 1"))
      assertThat(pay?.iepBasicRate).isEqualTo(100)
      assertThat(pay?.iepStandardRate).isEqualTo(125)
      assertThat(pay?.iepEnhancedRate).isEqualTo(150)
      assertThat(pay?.pieceRate).isEqualTo(0)
      assertThat(pay?.pieceRateItems).isEqualTo(0)
      assertThat(pay?.bands).isEmpty()
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 21))
      assertThat(endDate).isNull()
      assertThat(active).isTrue
      assertThat(createdBy).isEqualTo("SEED USER")
      assertThat(createdTime).isEqualTo(LocalDate.of(2022, 9, 21).atStartOfDay())
      assertThat(sessions).hasSize(2)
    }

    val mathsMorningSession = with(activity.session("Maths AM")) {
      assertThat(capacity).isEqualTo(10)
      assertThat(daysOfWeek).isEqualTo("1000000")
      assertThat(prisoners).hasSize(2)
      this
    }

    with(mathsMorningSession.prisoner("A11111A")) {
      assertThat(iepLevel).isNull() // TODO example of this?
      assertThat(payBand).isNull() // TODO example of this?
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 21))
      assertThat(endDate).isNull()
      assertThat(allocatedBy).isEqualTo("MR BLOGS")
      assertThat(allocatedTime).isEqualTo(LocalDateTime.of(2022, 10, 21, 9, 0))
    }

    with(mathsMorningSession.prisoner("A22222A")) {
      assertThat(iepLevel).isNull() // TODO example of this?
      assertThat(payBand).isNull() // TODO example of this?
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 21))
      assertThat(endDate).isNull()
      assertThat(allocatedBy).isEqualTo("MRS BLOGS")
      assertThat(allocatedTime).isEqualTo(LocalDateTime.of(2022, 10, 21, 9, 0))
    }

    val mathsAfternoonSession = with(activity.session("Maths PM")) {
      assertThat(capacity).isEqualTo(10)
      assertThat(daysOfWeek).isEqualTo("1000000")
      assertThat(prisoners).hasSize(2)
      this
    }

    with(mathsAfternoonSession.prisoner("A11111A")) {
      assertThat(iepLevel).isNull() // TODO example of this?
      assertThat(payBand).isNull() // TODO example of this?
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 21))
      assertThat(endDate).isNull()
      assertThat(allocatedBy).isEqualTo("MR BLOGS")
      assertThat(allocatedTime).isEqualTo(LocalDateTime.of(2022, 10, 21, 10, 0))
    }

    with(mathsAfternoonSession.prisoner("A22222A")) {
      assertThat(iepLevel).isNull() // TODO example of this?
      assertThat(payBand).isNull() // TODO example of this?
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 21))
      assertThat(endDate).isNull()
      assertThat(allocatedBy).isEqualTo("MRS BLOGS")
      assertThat(allocatedTime).isEqualTo(LocalDateTime.of(2022, 10, 21, 10, 0))
    }
  }

  @Sql(
    "classpath:test_data/seed-activity-id-2.sql"
  )
  @Test
  fun `get english activity with morning and afternoon sessions`() {
    val activity = webTestClient.getActivityById(2)!!

    with(activity) {
      assertThat(summary).isEqualTo("English")
      assertThat(description).isEqualTo("English Level 2")
      assertThat(category).isEqualTo(ActivityCategory(2, "C2", "Category 2"))
      assertThat(tier).isEqualTo(ActivityTier(2, "T2", "Tier 2"))
      assertThat(pay?.iepBasicRate).isEqualTo(50)
      assertThat(pay?.iepStandardRate).isEqualTo(75)
      assertThat(pay?.iepEnhancedRate).isEqualTo(100)
      assertThat(pay?.pieceRate).isEqualTo(0)
      assertThat(pay?.pieceRateItems).isEqualTo(0)
      assertThat(pay?.bands).isEmpty()
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 21))
      assertThat(endDate).isNull()
      assertThat(active).isTrue
      assertThat(createdBy).isEqualTo("SEED USER")
      assertThat(createdTime).isEqualTo(LocalDate.of(2022, 9, 21).atStartOfDay())
      assertThat(sessions).hasSize(2)
    }

    val englishMorningSession = with(activity.session("English AM")) {
      assertThat(capacity).isEqualTo(10)
      assertThat(daysOfWeek).isEqualTo("1000000")
      assertThat(prisoners).hasSize(2)
      this
    }

    with(englishMorningSession.prisoner("B11111B")) {
      assertThat(iepLevel).isNull() // TODO example of this?
      assertThat(payBand).isNull() // TODO example of this?
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 21))
      assertThat(endDate).isNull()
      assertThat(allocatedBy).isEqualTo("MR BLOGS")
      assertThat(allocatedTime).isEqualTo(LocalDateTime.of(2022, 10, 21, 0, 0))
    }

    with(englishMorningSession.prisoner("B22222B")) {
      assertThat(iepLevel).isNull() // TODO example of this?
      assertThat(payBand).isNull() // TODO example of this?
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 21))
      assertThat(endDate).isNull()
      assertThat(allocatedBy).isEqualTo("MRS BLOGS")
      assertThat(allocatedTime).isEqualTo(LocalDateTime.of(2022, 10, 21, 0, 0))
    }

    val englishAfternoonSession = with(activity.session("English PM")) {
      assertThat(description).isEqualTo("English PM")
      assertThat(capacity).isEqualTo(10)
      assertThat(daysOfWeek).isEqualTo("1000000")
      assertThat(prisoners).hasSize(2)
      this
    }

    with(englishAfternoonSession.prisoner("B11111B")) {
      assertThat(iepLevel).isNull() // TODO example of this?
      assertThat(payBand).isNull() // TODO example of this?
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 21))
      assertThat(endDate).isNull()
      assertThat(allocatedBy).isEqualTo("MR BLOGS")
      assertThat(allocatedTime).isEqualTo(LocalDateTime.of(2022, 10, 21, 0, 0))
    }

    with(englishAfternoonSession.prisoner("B22222B")) {
      assertThat(iepLevel).isNull() // TODO example of this?
      assertThat(payBand).isNull() // TODO example of this?
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 21))
      assertThat(endDate).isNull()
      assertThat(allocatedBy).isEqualTo("MRS BLOGS")
      assertThat(allocatedTime).isEqualTo(LocalDateTime.of(2022, 10, 21, 0, 0))
    }
  }

  private fun WebTestClient.getActivityById(id: Long) =
    get()
      .uri("/activities/$id")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Activity::class.java)
      .returnResult().responseBody

  private fun Activity.session(description: String) = sessions.session(description)

  private fun List<ActivitySession>.session(description: String) =
    firstOrNull() { it.description.uppercase() == description.uppercase() }
      ?: throw RuntimeException("Activity session $description not found.")

  private fun ActivitySession.prisoner(prisonNumber: String) = prisoners.prisoner(prisonNumber)
  private fun List<ActivityPrisoner>.prisoner(prisonNumber: String) =
    firstOrNull() { it.prisonerNumber.uppercase() == prisonNumber.uppercase() }
      ?: throw RuntimeException("Activity prisoner $prisonNumber not found.")
}
