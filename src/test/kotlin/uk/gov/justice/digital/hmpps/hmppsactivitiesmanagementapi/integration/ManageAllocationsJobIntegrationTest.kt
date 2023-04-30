package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
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
    deallocatedAllocations.forEach { it.assertIsDeallocated() }
  }

  @Sql("classpath:test_data/seed-activity-id-12.sql")
  @Test
  fun `deallocate offenders for activity with no end date`() {
    val activeAllocations = allocationRepository.findAll()

    assertThat(activeAllocations).hasSize(3)
    activeAllocations.forEach { it.assertIsActive() }

    webTestClient.manageAllocations()

    val allocations = allocationRepository.findAll()

    assertThat(allocations).hasSize(3)
    allocations.prisoner("A11111A").assertIsActive()
    allocations.prisoner("A22222A").assertIsDeallocated()
    allocations.prisoner("A33333A").assertIsActive()
  }

  private fun Allocation.assertIsActive() {
    assertThat(status(PrisonerStatus.ACTIVE))
    assertThat(deallocatedBy).isNull()
    assertThat(deallocatedReason).isNull()
    assertThat(deallocatedTime).isNull()
  }

  private fun Allocation.assertIsDeallocated() {
    assertThat(status(PrisonerStatus.ENDED))
    assertThat(deallocatedBy).isEqualTo("Activities Management Service")
    assertThat(deallocatedReason).isEqualTo("Allocation end date reached")
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
