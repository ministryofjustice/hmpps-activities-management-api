package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.MovementType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason.ENDED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason.TEMPORARILY_RELEASED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus.ACTIVE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus.AUTO_SUSPENDED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus.PENDING
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus.ALLOCATED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus.DECLINED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus.REMOVED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.movement
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.pentonvillePrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.WaitingListRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.HmppsAuditApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.HmppsAuditEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import java.time.LocalDate
import java.time.LocalDateTime

@TestPropertySource(
  properties = [
    "feature.audit.service.local.enabled=true",
    "feature.audit.service.hmpps.enabled=true",
  ],
)
class ManageAllocationsJobIntegrationTest : IntegrationTestBase() {

  @MockBean
  private lateinit var outboundEventsService: OutboundEventsService

  @MockBean
  private lateinit var hmppsAuditApiClient: HmppsAuditApiClient

  @Autowired
  private lateinit var allocationRepository: AllocationRepository

  @Autowired
  private lateinit var waitingListRepository: WaitingListRepository

  private val hmppsAuditEventCaptor = argumentCaptor<HmppsAuditEvent>()

  @Sql("classpath:test_data/seed-activity-id-11.sql")
  @Test
  fun `deallocate offenders for activity ending today`() {
    val activeAllocations = with(allocationRepository.findAll().filterNot(Allocation::isEnded)) {
      size isEqualTo 3
      onEach { it isStatus ACTIVE }
    }

    with(waitingListRepository.findAll()) {
      size isEqualTo 3
      none { it.isStatus(ALLOCATED, DECLINED, REMOVED) } isBool true
    }

    webTestClient.manageAllocations(withDeallocate = true)

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 1L)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 2L)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 3L)
    verifyNoMoreInteractions(outboundEventsService)

    with(allocationRepository.findAllById(activeAllocations.map { it.allocationId })) {
      size isEqualTo 3
      onEach { it isDeallocatedWithReason ENDED }
    }

    with(waitingListRepository.findAll()) {
      onEach { it isStatus DECLINED }
      onEach { it.declinedReason isEqualTo "Activity ended" }
    }
  }

  @Sql("classpath:test_data/seed-activity-id-12.sql")
  @Test
  fun `deallocate offenders for activity with no end date`() {
    with(allocationRepository.findAll()) {
      size isEqualTo 3
      onEach { it isStatus ACTIVE }
    }

    webTestClient.manageAllocations(withDeallocate = true)

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 2L)
    verifyNoMoreInteractions(outboundEventsService)

    with(allocationRepository.findAll()) {
      size isEqualTo 3
      prisoner("A11111A") isStatus ACTIVE
      prisoner("A22222A") isDeallocatedWithReason ENDED
      prisoner("A33333A") isStatus ACTIVE
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
    prisonApiMockServer.stubPrisonerMovements(
      listOf("A11111A"),
      listOf(movement("A11111A", fromPrisonCode = pentonvillePrisonCode, movementDate = TimeSource.daysInPast(10))),
    )

    with(allocationRepository.findAll()) {
      this hasSize 2
      prisonerAllocation(AUTO_SUSPENDED).prisonerNumber isEqualTo "A11111A"
      prisonerAllocation(PENDING).prisonerNumber isEqualTo "A11111A"
    }

    webTestClient.manageAllocations(withDeallocate = true)

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 1L)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 2L)
    verifyNoMoreInteractions(outboundEventsService)

    val expiredAllocations = allocationRepository.findAll().also { it hasSize 2 }

    expiredAllocations.forEach { allocation -> allocation isDeallocatedWithReason TEMPORARILY_RELEASED }
  }

  @Sql("classpath:test_data/seed-offender-with-waiting-list-application.sql")
  @Test
  fun `remove offenders waitlist applications due to expire`() {
    val prisoner = PrisonerSearchPrisonerFixture.instance(
      prisonerNumber = "A11111A",
      inOutStatus = Prisoner.InOutStatus.OUT,
      lastMovementType = MovementType.RELEASE,
      releaseDate = LocalDate.now().minusDays(5),
    )

    with(waitingListRepository.findAll().prisoner("A11111A")) {
      status isStatus WaitingListStatus.PENDING
      updatedBy isEqualTo null
      updatedTime isEqualTo null
    }

    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(listOf("A11111A"), listOf(prisoner))
    prisonApiMockServer.stubPrisonerMovements(
      listOf("A11111A"),
      listOf(movement("A11111A", fromPrisonCode = pentonvillePrisonCode, movementDate = TimeSource.daysInPast(10))),
    )

    webTestClient.manageAllocations(withDeallocate = true)

    with(waitingListRepository.findAll().prisoner("A11111A")) {
      status isStatus REMOVED
      updatedTime isCloseTo LocalDateTime.now()
      updatedBy isEqualTo "Activities Management Service"
    }

    verify(hmppsAuditApiClient, times(1)).createEvent(hmppsAuditEventCaptor.capture())

    hmppsAuditEventCaptor.firstValue.what isEqualTo "PRISONER_REMOVED_FROM_WAITING_LIST"
  }

  @Sql("classpath:test_data/seed-allocations-pending.sql")
  @Test
  fun `pending allocations on or before today are activated when prisoners are in prison`() {
    listOf("PAST", "TODAY", "FUTURE").map { prisonerNumber ->
      PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = prisonerNumber,
        inOutStatus = Prisoner.InOutStatus.IN,
      )
    }.also { prisoners -> prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(listOf("PAST", "TODAY"), prisoners) }

    with(allocationRepository.findAll()) {
      size isEqualTo 3
      prisoner("PAST") isStatus PENDING
      prisoner("TODAY") isStatus PENDING
      prisoner("FUTURE") isStatus PENDING
    }

    webTestClient.manageAllocations(withActivate = true)

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 1L)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 3L)
    verifyNoMoreInteractions(outboundEventsService)

    with(allocationRepository.findAll()) {
      prisoner("PAST") isStatus ACTIVE
      prisoner("TODAY") isStatus ACTIVE
      prisoner("FUTURE") isStatus PENDING
    }
  }

  @Sql("classpath:test_data/seed-allocations-pending.sql")
  @Test
  fun `pending allocations on or before today are suspended when prisoners are out of prison`() {
    listOf("PAST", "TODAY").map { prisonerNumber ->
      PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = prisonerNumber,
        inOutStatus = Prisoner.InOutStatus.OUT,
      )
    }.also { prisoners -> prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(listOf("PAST", "TODAY"), prisoners) }

    with(allocationRepository.findAll()) {
      size isEqualTo 3
      prisoner("PAST") isStatus PENDING
      prisoner("TODAY") isStatus PENDING
      prisoner("FUTURE") isStatus PENDING
    }

    webTestClient.manageAllocations(withActivate = true)

    with(allocationRepository.findAll()) {
      prisoner("PAST") isStatus AUTO_SUSPENDED
      prisoner("TODAY") isStatus AUTO_SUSPENDED
      prisoner("FUTURE") isStatus PENDING
    }
  }

  private infix fun WaitingList.isStatus(status: WaitingListStatus) {
    this.status isEqualTo status
  }

  private infix fun WaitingListStatus.isStatus(status: WaitingListStatus) {
    this isEqualTo status
  }

  private fun List<Allocation>.prisoner(number: String) = single { it.prisonerNumber == number }

  private fun List<Allocation>.prisonerAllocation(prisonerStatus: PrisonerStatus) =
    single { it.prisonerStatus == prisonerStatus }

  private fun List<WaitingList>.prisoner(number: String) = single { it.prisonerNumber == number }

  private infix fun Allocation.isStatus(status: PrisonerStatus) {
    assertThat(this.prisonerStatus).isEqualTo(status)
  }

  private infix fun Allocation.isDeallocatedWithReason(reason: DeallocationReason) {
    prisonerStatus isEqualTo PrisonerStatus.ENDED
    deallocatedBy isEqualTo "Activities Management Service"
    deallocatedReason isEqualTo reason
    deallocatedTime isCloseTo TimeSource.now()
  }

  private fun WebTestClient.manageAllocations(withActivate: Boolean = false, withDeallocate: Boolean = false) {
    post()
      .uri("/job/manage-allocations?withActivate=$withActivate&withDeallocate=$withDeallocate")
      .accept(MediaType.TEXT_PLAIN)
      .exchange()
      .expectStatus().isCreated
    Thread.sleep(1000)
  }
}
