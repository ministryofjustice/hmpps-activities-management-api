package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.testPentonvillePayBandOne
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.testPentonvillePayBandThree
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.testPentonvillePayBandTwo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PlannedDeallocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.PrisonerAllocations
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import java.time.LocalDate
import java.time.LocalDateTime

class PrisonerAllocationIntegrationTest : IntegrationTestBase() {

  @Sql(
    "classpath:test_data/seed-activity-id-6.sql",
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
          activitySummary = "Retirement",
          scheduleId = 1,
          scheduleDescription = "Retirement AM",
          prisonPayBand = testPentonvillePayBandOne,
          isUnemployment = true,
          startDate = LocalDate.of(2022, 10, 10),
          endDate = null,
          allocatedTime = LocalDateTime.of(2022, 10, 10, 9, 0),
          allocatedBy = "MR BLOGS",
          status = PrisonerStatus.ACTIVE,
          plannedDeallocation = null,
        ),
        Allocation(
          id = 4,
          prisonerNumber = "A11111A",
          bookingId = 10001,
          activitySummary = "Retirement",
          scheduleId = 2,
          scheduleDescription = "Retirement PM",
          prisonPayBand = testPentonvillePayBandThree,
          isUnemployment = true,
          startDate = LocalDate.of(2022, 10, 10),
          endDate = null,
          allocatedTime = LocalDateTime.of(2022, 10, 10, 10, 0),
          allocatedBy = "MR BLOGS",
          status = PrisonerStatus.ACTIVE,
          plannedDeallocation = null,
        ),
      )
    }

    with(prisonerAllocations.prisoner("A22222A")) {
      assertThat(allocations).containsExactlyInAnyOrder(
        Allocation(
          id = 2,
          prisonerNumber = "A22222A",
          bookingId = 10002,
          activitySummary = "Retirement",
          scheduleId = 1,
          scheduleDescription = "Retirement AM",
          prisonPayBand = testPentonvillePayBandTwo,
          isUnemployment = true,
          startDate = LocalDate.of(2022, 10, 10),
          endDate = null,
          allocatedTime = LocalDateTime.of(2022, 10, 10, 9, 0),
          allocatedBy = "MRS BLOGS",
          status = PrisonerStatus.ACTIVE,
          plannedDeallocation = null,
        ),
        Allocation(
          id = 5,
          prisonerNumber = "A22222A",
          bookingId = 10002,
          activitySummary = "Retirement",
          scheduleId = 2,
          scheduleDescription = "Retirement PM",
          prisonPayBand = testPentonvillePayBandThree,
          isUnemployment = true,
          startDate = LocalDate.of(2022, 10, 10),
          endDate = null,
          allocatedTime = LocalDateTime.of(2022, 10, 10, 10, 0),
          allocatedBy = "MRS BLOGS",
          suspendedTime = LocalDateTime.of(2022, 10, 11, 10, 0),
          suspendedBy = "SYSTEM",
          suspendedReason = "Temporary absence",
          status = PrisonerStatus.AUTO_SUSPENDED,
          plannedDeallocation = null,
        ),
      )
    }

    assertThat(prisonerAllocations.none { it.prisonerNumber == "A33333A" }).isTrue
  }

  @Sql(
    "classpath:test_data/seed-activity-id-6.sql",
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
          activitySummary = "Retirement",
          scheduleId = 1,
          scheduleDescription = "Retirement AM",
          prisonPayBand = testPentonvillePayBandOne,
          isUnemployment = true,
          startDate = LocalDate.of(2022, 10, 10),
          endDate = null,
          allocatedTime = LocalDateTime.of(2022, 10, 10, 9, 0),
          allocatedBy = "MR BLOGS",
          status = PrisonerStatus.ACTIVE,
          plannedDeallocation = null,
        ),
        Allocation(
          id = 4,
          prisonerNumber = "A11111A",
          bookingId = 10001,
          activitySummary = "Retirement",
          scheduleId = 2,
          scheduleDescription = "Retirement PM",
          prisonPayBand = testPentonvillePayBandThree,
          isUnemployment = true,
          startDate = LocalDate.of(2022, 10, 10),
          endDate = null,
          allocatedTime = LocalDateTime.of(2022, 10, 10, 10, 0),
          allocatedBy = "MR BLOGS",
          status = PrisonerStatus.ACTIVE,
          plannedDeallocation = null,
        ),
      )
    }

    with(prisonerAllocations.prisoner("A22222A")) {
      assertThat(allocations).containsExactlyInAnyOrder(
        Allocation(
          id = 2,
          prisonerNumber = "A22222A",
          bookingId = 10002,
          activitySummary = "Retirement",
          scheduleId = 1,
          scheduleDescription = "Retirement AM",
          prisonPayBand = testPentonvillePayBandTwo,
          isUnemployment = true,
          startDate = LocalDate.of(2022, 10, 10),
          endDate = null,
          allocatedTime = LocalDateTime.of(2022, 10, 10, 9, 0),
          allocatedBy = "MRS BLOGS",
          status = PrisonerStatus.ACTIVE,
          plannedDeallocation = null,
        ),
        Allocation(
          id = 5,
          prisonerNumber = "A22222A",
          bookingId = 10002,
          activitySummary = "Retirement",
          scheduleId = 2,
          scheduleDescription = "Retirement PM",
          prisonPayBand = testPentonvillePayBandThree,
          isUnemployment = true,
          startDate = LocalDate.of(2022, 10, 10),
          endDate = null,
          allocatedTime = LocalDateTime.of(2022, 10, 10, 10, 0),
          allocatedBy = "MRS BLOGS",
          status = PrisonerStatus.AUTO_SUSPENDED,
          suspendedBy = "SYSTEM",
          suspendedTime = LocalDateTime.of(2022, 10, 11, 10, 0),
          suspendedReason = "Temporary absence",
          plannedDeallocation = null,
        ),
      )
    }

    with(prisonerAllocations.prisoner("A33333A")) {
      assertThat(allocations).containsOnly(
        Allocation(
          id = 3,
          prisonerNumber = "A33333A",
          bookingId = 10003,
          activitySummary = "Retirement",
          scheduleId = 1,
          scheduleDescription = "Retirement AM",
          prisonPayBand = testPentonvillePayBandTwo,
          isUnemployment = true,
          startDate = LocalDate.of(2022, 10, 10),
          endDate = LocalDate.of(2022, 10, 11),
          allocatedTime = LocalDateTime.of(2022, 10, 10, 9, 0),
          allocatedBy = "MRS BLOGS",
          status = PrisonerStatus.ENDED,
          deallocatedBy = "SYSTEM",
          deallocatedReason = DeallocationReason.ENDED.toModel(),
          deallocatedTime = LocalDateTime.of(2022, 10, 11, 9, 0),
          plannedDeallocation =
          PlannedDeallocation(
            id = 1,
            plannedDate = LocalDate.of(2022, 10, 11),
            plannedBy = "MR BLOGS",
            plannedReason = DeallocationReason.PLANNED.toModel(),
            plannedAt = LocalDateTime.of(2022, 10, 11, 9, 0),
          ),
        ),
      )
    }
  }

  private fun List<PrisonerAllocations>.prisoner(prisonerNumber: String) = first { it.prisonerNumber == prisonerNumber }

  @Sql(
    "classpath:test_data/seed-activity-id-6.sql",
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
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(PrisonerAllocations::class.java)
      .returnResult().responseBody!!
}
