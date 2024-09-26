package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.DataFixRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.CASELOAD_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_ACTIVITY_ADMIN
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.activeInRisleyPrisoner
import java.time.LocalDate

class PaidToUnpaidFixIntegrationTest : IntegrationTestBase() {

  @Autowired
  private lateinit var repository: ActivityScheduleRepository

  @Autowired
  private lateinit var dataFixRepository: DataFixRepository

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
    webTestClient.post().uri("/job/fix-zero-pay?deallocate=true&prisonCode=RSI&activityScheduleId=129")
      .headers(setAuthorisation(isClientToken = true, roles = listOf(ROLE_ACTIVITY_ADMIN)))
      .header(CASELOAD_ID, "RSI")
      .exchange()
      .expectStatus().isAccepted

    Thread.sleep(3000)

    val allocations = repository.findById(129).orElseThrow().allocations()

    assertThat(allocations).hasSize(23)

    with(allocations) {
      this.single {
        it.prisonerNumber == "A8862DW" &&
          it.prisonerStatus == PrisonerStatus.ACTIVE &&
          it.plannedDeallocation?.plannedReason == DeallocationReason.OTHER &&
          it.plannedDeallocation?.plannedDate == LocalDate.now()
      }
    }

    val dataFixList = dataFixRepository.findAll()
    assertThat(dataFixList).hasSize(22)
    with(dataFixList) {
      this.none {
        it.prisonerNumber == "A3322FA"
      }
      this.single {
        it.prisonerNumber == "A8862DW" &&
          it.startDate == LocalDate.of(2023, 9, 30) &&
          it.activityScheduleId == 129L
      }
      this.single {
        it.prisonerNumber == "A4774FD" &&
          it.startDate == LocalDate.now().plusDays(3) &&
          it.activityScheduleId == 129L
      }
    }
  }

  @Sql(
    "classpath:test_data/seed-fix-unpaid-to-paid-and-reallocate.sql",
  )
  @Test
  fun `after running reset to paid and reallocate activity should be unpaid and the prisoners reallocated`() {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(activeInRisleyPrisoner.copy(prisonerNumber = "A8862DW"))
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(activeInRisleyPrisoner.copy(prisonerNumber = "A0334EZ"))
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(activeInRisleyPrisoner.copy(prisonerNumber = "A1611AF"))
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(activeInRisleyPrisoner.copy(prisonerNumber = "A6015FC"))

    val activitySchedule = repository.findById(129).orElseThrow()

    assertThat(activitySchedule.activity.paid).isTrue()
    assertThat(activitySchedule.activity.activityPay()).hasSize(30)

    webTestClient.post().uri("/job/fix-zero-pay?allocate=true&makeUnpaid=true&prisonCode=RSI&activityScheduleId=129")
      .headers(setAuthorisation(isClientToken = false, roles = listOf(ROLE_ACTIVITY_ADMIN)))
      .header(CASELOAD_ID, "RSI")
      .exchange()
      .expectStatus().isAccepted

    Thread.sleep(3000)

    val activityScheduleUpdated = repository.findById(129).orElseThrow()

    assertThat(activityScheduleUpdated.activity.paid).isFalse()
    assertThat(activityScheduleUpdated.activity.activityPay()).hasSize(0)

    assertThat(activityScheduleUpdated.allocations()).hasSize(6)

    with(activityScheduleUpdated.allocations()) {
      this.single { it.prisonerNumber == "A8862DW" && it.prisonerStatus == PrisonerStatus.ENDED }
      this.single { it.prisonerNumber == "A0334EZ" && it.prisonerStatus == PrisonerStatus.ENDED }
      this.single { it.prisonerNumber == "A1611AF" && it.prisonerStatus == PrisonerStatus.ENDED }
      this.single { it.prisonerNumber == "A6015FC" && it.prisonerStatus == PrisonerStatus.ENDED }
      this.single { it.prisonerNumber == "A8862DW" && it.prisonerStatus == PrisonerStatus.ACTIVE && it.startDate == LocalDate.now() }
      this.single { it.prisonerNumber == "A0334EZ" && it.prisonerStatus == PrisonerStatus.PENDING && it.startDate == LocalDate.now().plusDays(3) }
    }

    assertThat(dataFixRepository.findAll()).hasSize(0)
  }
}
