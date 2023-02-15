package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.testPentonvillePayBandOne
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.testPentonvillePayBandThree
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.testPentonvillePayBandTwo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.PrisonerAllocations
import java.time.LocalDate
import java.time.LocalDateTime

class PrisonerAllocationIntegrationTest : IntegrationTestBase() {

  @Sql(
    "classpath:test_data/seed-activity-id-6.sql"
  )
  @Test
  fun `get only active allocations for a group of prisoners at Pentonville`() {
    val prisonerAllocations = webTestClient.getAllocations("PVI", listOf("A11111A", "A22222A", "A33333A"), true)

    assertThat(prisonerAllocations).hasSize(2)

    with(prisonerAllocations.prisoner("A11111A")) {
      assertThat(allocations).containsExactlyInAnyOrder(
        Allocation(
          id = 1,
          prisonerNumber = "A11111A",
          bookingId = 10001,
          activitySummary = "Maths",
          scheduleId = 1,
          scheduleDescription = "Maths AM",
          prisonPayBand = testPentonvillePayBandOne,
          startDate = LocalDate.of(2022, 10, 10),
          endDate = null,
          allocatedTime = LocalDateTime.of(2022, 10, 10, 9, 0),
          allocatedBy = "MR BLOGS"
        ),
        Allocation(
          id = 4,
          prisonerNumber = "A11111A",
          bookingId = 10001,
          activitySummary = "Maths",
          scheduleId = 2,
          scheduleDescription = "Maths PM",
          prisonPayBand = testPentonvillePayBandThree,
          startDate = LocalDate.of(2022, 10, 10),
          endDate = null,
          allocatedTime = LocalDateTime.of(2022, 10, 10, 10, 0),
          allocatedBy = "MR BLOGS"
        )
      )
    }

    with(prisonerAllocations.prisoner("A22222A")) {
      assertThat(allocations).containsExactlyInAnyOrder(
        Allocation(
          id = 2,
          prisonerNumber = "A22222A",
          bookingId = 10002,
          activitySummary = "Maths",
          scheduleId = 1,
          scheduleDescription = "Maths AM",
          prisonPayBand = testPentonvillePayBandTwo,
          startDate = LocalDate.of(2022, 10, 10),
          endDate = null,
          allocatedTime = LocalDateTime.of(2022, 10, 10, 9, 0),
          allocatedBy = "MRS BLOGS"
        ),
        Allocation(
          id = 5,
          prisonerNumber = "A22222A",
          bookingId = 10002,
          activitySummary = "Maths",
          scheduleId = 2,
          scheduleDescription = "Maths PM",
          prisonPayBand = testPentonvillePayBandThree,
          startDate = LocalDate.of(2022, 10, 10),
          endDate = null,
          allocatedTime = LocalDateTime.of(2022, 10, 10, 10, 0),
          allocatedBy = "MRS BLOGS"
        )
      )
    }

    assertThat(prisonerAllocations.none { it.prisonerNumber == "A33333A" }).isTrue
  }

  @Sql(
    "classpath:test_data/seed-activity-id-6.sql"
  )
  @Test
  fun `get all allocations for a group of prisoners at Pentonville`() {
    val prisonerAllocations = webTestClient.getAllocations("PVI", listOf("A11111A", "A22222A", "A33333A"), false)

    assertThat(prisonerAllocations).hasSize(3)

    with(prisonerAllocations.prisoner("A11111A")) {
      assertThat(allocations).containsExactlyInAnyOrder(
        Allocation(
          id = 1,
          prisonerNumber = "A11111A",
          bookingId = 10001,
          activitySummary = "Maths",
          scheduleId = 1,
          scheduleDescription = "Maths AM",
          prisonPayBand = testPentonvillePayBandOne,
          startDate = LocalDate.of(2022, 10, 10),
          endDate = null,
          allocatedTime = LocalDateTime.of(2022, 10, 10, 9, 0),
          allocatedBy = "MR BLOGS"
        ),
        Allocation(
          id = 4,
          prisonerNumber = "A11111A",
          bookingId = 10001,
          activitySummary = "Maths",
          scheduleId = 2,
          scheduleDescription = "Maths PM",
          prisonPayBand = testPentonvillePayBandThree,
          startDate = LocalDate.of(2022, 10, 10),
          endDate = null,
          allocatedTime = LocalDateTime.of(2022, 10, 10, 10, 0),
          allocatedBy = "MR BLOGS"
        )
      )
    }

    with(prisonerAllocations.prisoner("A22222A")) {
      assertThat(allocations).containsExactlyInAnyOrder(
        Allocation(
          id = 2,
          prisonerNumber = "A22222A",
          bookingId = 10002,
          activitySummary = "Maths",
          scheduleId = 1,
          scheduleDescription = "Maths AM",
          prisonPayBand = testPentonvillePayBandTwo,
          startDate = LocalDate.of(2022, 10, 10),
          endDate = null,
          allocatedTime = LocalDateTime.of(2022, 10, 10, 9, 0),
          allocatedBy = "MRS BLOGS"
        ),
        Allocation(
          id = 5,
          prisonerNumber = "A22222A",
          bookingId = 10002,
          activitySummary = "Maths",
          scheduleId = 2,
          scheduleDescription = "Maths PM",
          prisonPayBand = testPentonvillePayBandThree,
          startDate = LocalDate.of(2022, 10, 10),
          endDate = null,
          allocatedTime = LocalDateTime.of(2022, 10, 10, 10, 0),
          allocatedBy = "MRS BLOGS"
        )
      )
    }

    with(prisonerAllocations.prisoner("A33333A")) {
      assertThat(allocations).containsOnly(
        Allocation(
          id = 3,
          prisonerNumber = "A33333A",
          bookingId = 10003,
          activitySummary = "Maths",
          scheduleId = 1,
          scheduleDescription = "Maths AM",
          prisonPayBand = testPentonvillePayBandTwo,
          startDate = LocalDate.of(2022, 10, 10),
          endDate = LocalDate.of(2022, 10, 11),
          allocatedTime = LocalDateTime.of(2022, 10, 10, 9, 0),
          allocatedBy = "MRS BLOGS"
        )
      )
    }
  }

  private fun List<PrisonerAllocations>.prisoner(prisonerNumber: String) = first { it.prisonerNumber == prisonerNumber }

  @Sql(
    "classpath:test_data/seed-activity-id-6.sql"
  )
  @Test
  fun `no allocations found for a group of prisoners at Moorland`() {
    assertThat(webTestClient.getAllocations("MDI", listOf("A11111A", "A22222A"), false)).isEmpty()
  }

  private fun WebTestClient.getAllocations(prisonCode: String, prisonerNumbers: List<String>, activeOnly: Boolean) =
    post()
      .uri("/prisons/$prisonCode/prisoner-allocations?activeOnly=$activeOnly")
      .bodyValue(prisonerNumbers)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(PrisonerAllocations::class.java)
      .returnResult().responseBody!!
}
