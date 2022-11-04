package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityTier
import java.time.LocalDate
import java.time.LocalDateTime

class ActivityIntegrationTest : IntegrationTestBase() {

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql"
  )
  @Test
  fun `get scheduled maths activities with morning and afternoon`() {
    val mathsLevelOneActivity = with(webTestClient.getActivityById(1)!!) {
      assertThat(prisonCode).isEqualTo("PVI")
      assertThat(summary).isEqualTo("Maths")
      assertThat(description).isEqualTo("Maths Level 1")
      assertThat(category).isEqualTo(ActivityCategory(1, "C1", "Category 1"))
      assertThat(tier).isEqualTo(ActivityTier(1, "T1", "Tier 1"))
      assertThat(pay).hasSize(1)
      pay.map {
        assertThat(it.incentiveLevel).isEqualTo("BAS")
        assertThat(it.payBand).isEqualTo("A")
        assertThat(it.rate).isEqualTo(125)
        assertThat(it.pieceRate).isEqualTo(150)
        assertThat(it.pieceRateItems).isEqualTo(1)
      }
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 10))
      assertThat(endDate).isNull()
      assertThat(active).isTrue
      assertThat(createdBy).isEqualTo("SEED USER")
      assertThat(createdTime).isEqualTo(LocalDate.of(2022, 9, 21).atStartOfDay())
      assertThat(schedules).hasSize(2)
      this
    }

    val mathsMorning = with(mathsLevelOneActivity.schedule("Maths AM")) {
      assertThat(capacity).isEqualTo(10)
      assertThat(daysOfWeek).isEqualTo(listOf("Mon"))
      assertThat(allocations).hasSize(2)
      assertThat(internalLocation?.id).isEqualTo(1)
      assertThat(internalLocation?.code).isEqualTo("L1")
      assertThat(internalLocation?.description).isEqualTo("Location 1")
      this
    }

    with(mathsMorning.allocatedPrisoner("A11111A")) {
      assertThat(incentiveLevel).isEqualTo("BAS")
      assertThat(payBand).isEqualTo("A")
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 10))
      assertThat(endDate).isNull()
      assertThat(allocatedBy).isEqualTo("MR BLOGS")
      assertThat(allocatedTime).isEqualTo(LocalDateTime.of(2022, 10, 10, 9, 0))
    }

    with(mathsMorning.allocatedPrisoner("A22222A")) {
      assertThat(incentiveLevel).isEqualTo("STD")
      assertThat(payBand).isEqualTo("B")
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 10))
      assertThat(endDate).isNull()
      assertThat(allocatedBy).isEqualTo("MRS BLOGS")
      assertThat(allocatedTime).isEqualTo(LocalDateTime.of(2022, 10, 10, 9, 0))
    }

    val mathsAfternoon = with(mathsLevelOneActivity.schedule("Maths PM")) {
      assertThat(capacity).isEqualTo(10)
      assertThat(daysOfWeek).isEqualTo(listOf("Mon"))
      assertThat(allocations).hasSize(2)
      assertThat(internalLocation?.id).isEqualTo(2)
      assertThat(internalLocation?.code).isEqualTo("L2")
      assertThat(internalLocation?.description).isEqualTo("Location 2")
      this
    }

    with(mathsAfternoon.allocatedPrisoner("A11111A")) {
      assertThat(incentiveLevel).isEqualTo("STD")
      assertThat(payBand).isEqualTo("C")
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 10))
      assertThat(endDate).isNull()
      assertThat(allocatedBy).isEqualTo("MR BLOGS")
      assertThat(allocatedTime).isEqualTo(LocalDateTime.of(2022, 10, 10, 10, 0))
    }

    with(mathsAfternoon.allocatedPrisoner("A22222A")) {
      assertThat(incentiveLevel).isEqualTo("ENH")
      assertThat(payBand).isEqualTo("D")
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 10))
      assertThat(endDate).isNull()
      assertThat(allocatedBy).isEqualTo("MRS BLOGS")
      assertThat(allocatedTime).isEqualTo(LocalDateTime.of(2022, 10, 10, 10, 0))
    }
  }

  @Sql(
    "classpath:test_data/seed-activity-id-2.sql"
  )
  @Test
  fun `get scheduled english activities for morning and afternoon`() {
    val englishLevelTwoActivity = with(webTestClient.getActivityById(2)!!) {
      assertThat(summary).isEqualTo("English")
      assertThat(description).isEqualTo("English Level 2")
      assertThat(category).isEqualTo(ActivityCategory(2, "C2", "Category 2"))
      assertThat(tier).isEqualTo(ActivityTier(2, "T2", "Tier 2"))
      pay.map {
        assertThat(it.incentiveLevel).isEqualTo("BAS")
        assertThat(it.payBand).isEqualTo("A")
        assertThat(it.rate).isEqualTo(75)
        assertThat(it.pieceRate).isEqualTo(0)
        assertThat(it.pieceRateItems).isEqualTo(0)
      }
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 21))
      assertThat(endDate).isNull()
      assertThat(active).isTrue
      assertThat(createdBy).isEqualTo("SEED USER")
      assertThat(createdTime).isEqualTo(LocalDate.of(2022, 9, 21).atStartOfDay())
      assertThat(schedules).hasSize(2)
      this
    }

    val englishMorning = with(englishLevelTwoActivity.schedule("English AM")) {
      assertThat(capacity).isEqualTo(10)
      assertThat(daysOfWeek).isEqualTo(listOf("Mon"))
      assertThat(allocations).hasSize(2)
      assertThat(internalLocation?.id).isEqualTo(3)
      assertThat(internalLocation?.code).isEqualTo("L3")
      assertThat(internalLocation?.description).isEqualTo("Location 3")
      this
    }

    with(englishMorning.allocatedPrisoner("B11111B")) {
      assertThat(incentiveLevel).isEqualTo("ENH")
      assertThat(payBand).isEqualTo("A")
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 21))
      assertThat(endDate).isNull()
      assertThat(allocatedBy).isEqualTo("MR BLOGS")
      assertThat(allocatedTime).isEqualTo(LocalDateTime.of(2022, 10, 21, 0, 0))
    }

    with(englishMorning.allocatedPrisoner("B22222B")) {
      assertThat(incentiveLevel).isEqualTo("BAS")
      assertThat(payBand).isEqualTo("B")
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 21))
      assertThat(endDate).isNull()
      assertThat(allocatedBy).isEqualTo("MRS BLOGS")
      assertThat(allocatedTime).isEqualTo(LocalDateTime.of(2022, 10, 21, 0, 0))
    }

    val englishAfternoon = with(englishLevelTwoActivity.schedule("English PM")) {
      assertThat(description).isEqualTo("English PM")
      assertThat(capacity).isEqualTo(10)
      assertThat(daysOfWeek).isEqualTo(listOf("Mon"))
      assertThat(allocations).hasSize(2)
      assertThat(internalLocation?.id).isEqualTo(4)
      assertThat(internalLocation?.code).isEqualTo("L4")
      assertThat(internalLocation?.description).isEqualTo("Location 4")
      this
    }

    with(englishAfternoon.allocatedPrisoner("B11111B")) {
      assertThat(incentiveLevel).isEqualTo("STD")
      assertThat(payBand).isEqualTo("C")
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 21))
      assertThat(endDate).isNull()
      assertThat(allocatedBy).isEqualTo("MR BLOGS")
      assertThat(allocatedTime).isEqualTo(LocalDateTime.of(2022, 10, 21, 0, 0))
    }

    with(englishAfternoon.allocatedPrisoner("B22222B")) {
      assertThat(incentiveLevel).isEqualTo("STD")
      assertThat(payBand).isEqualTo("D")
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

  private fun Activity.schedule(description: String) = schedules.schedule(description)

  private fun List<ActivitySchedule>.schedule(description: String) =
    firstOrNull { it.description.uppercase() == description.uppercase() }
      ?: throw RuntimeException("Activity schedule $description not found.")
}
