package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.MovementType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AllocationOperation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class ManageAllocationsJobIntegrationTest : IntegrationTestBase() {

  @Autowired
  private lateinit var allocationRepository: AllocationRepository

  @Sql("classpath:test_data/seed-activity-id-11.sql")
  @Test
  fun `deallocate offenders for activity ending today`() {
    val activeAllocations = allocationRepository.findAll()

    assertThat(activeAllocations).hasSize(3)
    activeAllocations.forEach { it.assertIs(PrisonerStatus.ACTIVE) }

    webTestClient.manageAllocations(listOf(AllocationOperation.DEALLOCATE_ENDING))

    val deallocatedAllocations = allocationRepository.findAll()

    assertThat(deallocatedAllocations).hasSize(3)
    deallocatedAllocations.forEach { it.assertIsDeallocated(DeallocationReason.ENDED) }
  }

  @Sql("classpath:test_data/seed-activity-id-12.sql")
  @Test
  fun `deallocate offenders for activity with no end date`() {
    val allocations = allocationRepository.findAll().also {
      it.prisoner("A11111A").assertIs(PrisonerStatus.ACTIVE)
      it.prisoner("A22222A").assertIs(PrisonerStatus.ACTIVE)
      it.prisoner("A33333A").assertIs(PrisonerStatus.ACTIVE)
    }

    assertThat(allocations).hasSize(3)

    webTestClient.manageAllocations(listOf(AllocationOperation.DEALLOCATE_ENDING))

    allocationRepository.findAll().let {
      assertThat(it).hasSize(3)
      it.prisoner("A11111A").assertIs(PrisonerStatus.ACTIVE)
      it.prisoner("A22222A").assertIsDeallocated(DeallocationReason.ENDED)
      it.prisoner("A33333A").assertIs(PrisonerStatus.ACTIVE)
    }
  }

  @Sql("classpath:test_data/seed-allocations-due-to-expire.sql")
  @Test
  fun `deallocate offenders allocations due to expire`() {
    val prisoner = PrisonerSearchPrisonerFixture.instance(
      prisonerNumber = "A11111A",
      inOutStatus = Prisoner.InOutStatus.OUT,
      lastMovementType = MovementType.RELEASE,
      releaseDate = LocalDate.now().minusDays(5),
    )

    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(listOf("A11111A"), listOf(prisoner))

    val allocations = allocationRepository.findAll().also {
      it.prisoner("A11111A").assertIsAutoSuspended()
    }

    assertThat(allocations).hasSize(1)

    webTestClient.manageAllocations(listOf(AllocationOperation.DEALLOCATE_EXPIRING))

    allocationRepository.findAll().prisoner("A11111A").assertIsDeallocated(DeallocationReason.EXPIRED)
  }

  @Sql("classpath:test_data/seed-allocations-pending.sql")
  @Test
  fun `pending allocation is activated`() {
    val allocations = allocationRepository.findAll().also {
      it.prisoner("A11111A").assertIs(PrisonerStatus.PENDING)
    }

    assertThat(allocations).hasSize(1)

    webTestClient.manageAllocations(listOf(AllocationOperation.STARTING_TODAY))

    allocationRepository.findAll().prisoner("A11111A").assertIs(PrisonerStatus.ACTIVE)
  }

  private fun Allocation.assertIs(status: PrisonerStatus) {
    assertThat(this.prisonerStatus).isEqualTo(status)
  }

  private fun Allocation.assertIsAutoSuspended() {
    assertThat(this.prisonerStatus).isEqualTo(PrisonerStatus.AUTO_SUSPENDED)
  }

  private fun Allocation.assertIsDeallocated(reason: DeallocationReason) {
    assertThat(prisonerStatus).isEqualTo(PrisonerStatus.ENDED)
    assertThat(deallocatedBy).isEqualTo("Activities Management Service")
    assertThat(deallocatedReason).isEqualTo(reason)
    assertThat(deallocatedTime).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
  }

  private fun List<Allocation>.prisoner(number: String) = first { it.prisonerNumber == number }

  private fun WebTestClient.manageAllocations(operations: List<AllocationOperation>) {
    post()
      .uri("/job/manage-allocations")
      .accept(MediaType.TEXT_PLAIN)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(mapper.writeValueAsString(operations))
      .exchange()
      .expectStatus().isCreated
    Thread.sleep(1000)
  }
}
