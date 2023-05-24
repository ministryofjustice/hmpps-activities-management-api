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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
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
    activeAllocations.forEach { it.assertIsActive() }

    webTestClient.manageAllocations()

    val deallocatedAllocations = allocationRepository.findAll()

    assertThat(deallocatedAllocations).hasSize(3)
    deallocatedAllocations.forEach { it.assertIsDeallocated("Allocation end date reached") }
  }

  @Sql("classpath:test_data/seed-activity-id-12.sql")
  @Test
  fun `deallocate offenders for activity with no end date`() {
    val allocations = allocationRepository.findAll().also {
      it.prisoner("A11111A").assertIsActive()
      it.prisoner("A22222A").assertIsActive()
      it.prisoner("A33333A").assertIsActive()
    }

    assertThat(allocations).hasSize(3)

    webTestClient.manageAllocations()

    allocationRepository.findAll().let {
      assertThat(it).hasSize(3)
      it.prisoner("A11111A").assertIsActive()
      it.prisoner("A22222A").assertIsDeallocated("Allocation end date reached")
      it.prisoner("A33333A").assertIsActive()
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

    webTestClient.manageAllocations()

    allocationRepository.findAll().prisoner("A11111A").assertIsDeallocated("Expired")
  }

  private fun Allocation.assertIsActive() {
    assertThat(prisonerStatus).isEqualTo(PrisonerStatus.ACTIVE)
    assertThat(deallocatedBy).isNull()
    assertThat(deallocatedReason).isNull()
    assertThat(deallocatedTime).isNull()
  }

  private fun Allocation.assertIsAutoSuspended() {
    assertThat(this.prisonerStatus).isEqualTo(PrisonerStatus.AUTO_SUSPENDED)
  }

  private fun Allocation.assertIsDeallocated(reason: String) {
    assertThat(prisonerStatus).isEqualTo(PrisonerStatus.ENDED)
    assertThat(deallocatedBy).isEqualTo("Activities Management Service")
    assertThat(deallocatedReason).isEqualTo(reason)
    assertThat(deallocatedTime).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
  }

  private fun List<Allocation>.prisoner(number: String) = first { it.prisonerNumber == number }

  private fun WebTestClient.manageAllocations() {
    post()
      .uri("/job/manage-allocations")
      .accept(MediaType.TEXT_PLAIN)
      .exchange()
      .expectStatus().isCreated
    Thread.sleep(1000)
  }
}
