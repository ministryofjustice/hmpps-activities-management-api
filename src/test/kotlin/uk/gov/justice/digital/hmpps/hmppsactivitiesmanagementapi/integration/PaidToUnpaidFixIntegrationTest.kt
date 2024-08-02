package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.CASELOAD_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_ACTIVITY_ADMIN
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.activeInRisleyPrisoner
import java.time.LocalDate

class PaidToUnpaidFixIntegrationTest : IntegrationTestBase() {

  @Autowired
  private lateinit var repository: ActivityScheduleRepository

  @BeforeEach
  fun initMocks() {
    prisonApiMockServer.resetAll()
    prisonerSearchApiMockServer.resetAll()
  }

  @Sql(
    "classpath:test_data/seed-fix-unpaid-to-paid-and-deallocate.sql",
  )
  @Test
  fun `after running deallocate prisoners should all be deallocated`() {

    webTestClient.post().uri("/job/fix-zero-pay?deallocate=true")
      .headers(setAuthorisation(isClientToken = true, roles = listOf(ROLE_ACTIVITY_ADMIN)))
      .header(CASELOAD_ID, "RSI")
      .exchange()
      .expectStatus().isAccepted

    Thread.sleep(3000)

    val allocations = repository.findById(151).orElseThrow().allocations()

    assertThat(allocations).hasSize(6)

    with(allocations.first()){
      assertThat(prisonerNumber).isEqualTo("A7175CH")
      assertThat(prisonerStatus).isEqualTo(PrisonerStatus.ACTIVE)
      assertThat(plannedDeallocation?.plannedReason).isEqualTo(DeallocationReason.OTHER)
      assertThat(plannedDeallocation?.plannedDate).isEqualTo(LocalDate.now())
    }
  }

  @Sql(
    "classpath:test_data/seed-fix-unpaid-to-paid-and-reallocate.sql",
  )
  @Test
  fun `after running reset to paid and reallocate activity should be unpaid and the prisoners reallocated`() {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(activeInRisleyPrisoner.copy(prisonerNumber = "A7175CH"))
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(activeInRisleyPrisoner.copy(prisonerNumber = "A3903DM"))
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(activeInRisleyPrisoner.copy(prisonerNumber = "A2539EW"))
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(activeInRisleyPrisoner.copy(prisonerNumber = "A3084EX"))
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(activeInRisleyPrisoner.copy(prisonerNumber = "A4778DA"))
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(activeInRisleyPrisoner.copy(prisonerNumber = "A5617CQ"))

    val activitySchedule = repository.findById(151).orElseThrow()

    assertThat(activitySchedule.activity.paid).isTrue()
    assertThat(activitySchedule.activity.activityPay()).hasSize(30)

    webTestClient.post().uri("/job/fix-zero-pay?allocate=true&makeUnpaid=true")
      .headers(setAuthorisation(isClientToken = false, roles = listOf(ROLE_ACTIVITY_ADMIN)))
      .header(CASELOAD_ID, "RSI")
      .exchange()
      .expectStatus().isAccepted

    Thread.sleep(3000)

    val activityScheduleUpdated = repository.findById(151).orElseThrow()

    assertThat(activityScheduleUpdated.activity.paid).isFalse()
    assertThat(activityScheduleUpdated.activity.activityPay()).hasSize(0)

    assertThat(activityScheduleUpdated.allocations()).hasSize(12)

    with(activityScheduleUpdated.allocations()) {
      this.single { it.prisonerNumber == "A7175CH" && it.prisonerStatus == PrisonerStatus.ENDED }
      this.single { it.prisonerNumber == "A7175CH" && it.prisonerStatus == PrisonerStatus.PENDING && it.startDate == LocalDate.now().plusDays(1) }
    }
  }
}
