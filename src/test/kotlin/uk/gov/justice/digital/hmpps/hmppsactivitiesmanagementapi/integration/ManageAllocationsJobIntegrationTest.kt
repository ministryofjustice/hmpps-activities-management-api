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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.daysAgo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason.ENDED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason.TEMPORARILY_RELEASED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus.ACTIVE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus.AUTO_SUSPENDED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus.PENDING
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus.SUSPENDED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus.ALLOCATED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus.DECLINED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus.REMOVED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.movement
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.WaitingListRepository
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
    "jobs.deallocate-allocations-ending.days-start=2",
  ],
)
class ManageAllocationsJobIntegrationTest : IntegrationTestBase() {

  @MockBean
  private lateinit var outboundEventsService: OutboundEventsService

  @Autowired
  private lateinit var allocationRepository: AllocationRepository

  @Autowired
  private lateinit var waitingListRepository: WaitingListRepository

  private val hmppsAuditEventCaptor = argumentCaptor<HmppsAuditEvent>()

  @Sql("classpath:test_data/seed-activity-id-11.sql")
  @Test
  fun `deallocate offenders for activity ending yesterday`() {
    val activeAllocations = with(allocationRepository.findAll().filterNot(Allocation::isEnded)) {
      size isEqualTo 3
      onEach { it isStatus ACTIVE }
    }

    with(waitingListRepository.findAll()) {
      size isEqualTo 3
      none { it.isStatus(ALLOCATED, DECLINED, REMOVED) } isBool true
    }

    waitForJobs({ webTestClient.manageAllocations(withDeallocateEnding = true) })

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

    waitForJobs({ webTestClient.manageAllocations(withDeallocateEnding = true) })

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 2L)
    verifyNoMoreInteractions(outboundEventsService)

    with(allocationRepository.findAll()) {
      size isEqualTo 3
      prisoner("A11111A") isStatus ACTIVE
      prisoner("A22222A") isDeallocatedWithReason ENDED
      prisoner("A33333A") isStatus ACTIVE
    }
  }

  @Sql("classpath:test_data/seed-activity-id-28.sql")
  @Test
  fun `do not deallocate offenders for activity if allocated end date is before job window`() {
    with(allocationRepository.findAll()) {
      size isEqualTo 3
      onEach { it isStatus ACTIVE }
    }

    waitForJobs({ webTestClient.manageAllocations(withDeallocateEnding = true) })

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
      listOf(movement("A11111A", fromPrisonCode = PENTONVILLE_PRISON_CODE, movementDate = 10.daysAgo())),
    )

    with(allocationRepository.findAll()) {
      this hasSize 2
      prisonerAllocation(AUTO_SUSPENDED).prisonerNumber isEqualTo "A11111A"
      prisonerAllocation(PENDING).prisonerNumber isEqualTo "A11111A"
    }

    waitForJobs({ webTestClient.manageAllocations(withDeallocateExpiring = true) })

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
      listOf(movement("A11111A", fromPrisonCode = PENTONVILLE_PRISON_CODE, movementDate = 10.daysAgo())),
    )

    waitForJobs({ webTestClient.manageAllocations(withDeallocateExpiring = true) })

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
        prisonId = "PVI",
      )
    }.also { prisoners -> prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(listOf("PAST", "TODAY"), prisoners) }

    with(allocationRepository.findAll()) {
      size isEqualTo 3
      prisoner("PAST") isStatus PENDING
      prisoner("TODAY") isStatus PENDING
      prisoner("FUTURE") isStatus PENDING
    }

    waitForJobs({ webTestClient.manageAllocations(withActivate = true) }, 3)

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

    waitForJobs({ webTestClient.manageAllocations(withActivate = true) }, 3)

    with(allocationRepository.findAll()) {
      prisoner("PAST") isStatus AUTO_SUSPENDED
      prisoner("TODAY") isStatus AUTO_SUSPENDED
      prisoner("FUTURE") isStatus PENDING
    }
  }

  @Sql("classpath:test_data/seed-allocations-with-planned-suspension.sql")
  @Test
  fun `active and pending allocations on or before today are suspended when they have a planned suspension`() {
    listOf("TODAY").map { prisonerNumber ->
      PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = prisonerNumber,
        prisonId = "PVI",
      )
    }.also { prisoners -> prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(listOf("TODAY"), prisoners) }

    with(allocationRepository.findAll()) {
      size isEqualTo 3
      prisoner("PAST") isStatus ACTIVE
      prisoner("TODAY") isStatus PENDING
      prisoner("FUTURE") isStatus PENDING
    }

    waitForJobs({ webTestClient.manageAllocations(withActivate = true) }, 3)

    with(allocationRepository.findAll()) {
      prisoner("PAST") isStatus SUSPENDED
      prisoner("TODAY") isStatus SUSPENDED
      prisoner("FUTURE") isStatus PENDING
    }
  }

  @Sql("classpath:test_data/seed-allocation-with-planned-suspension-ending-today.sql")
  @Test
  fun `suspended allocations which are planned to be un-suspended today are set to ACTIVE`() {
    with(allocationRepository.findAll()) {
      size isEqualTo 1
      prisoner("G4508UU") isStatus SUSPENDED
    }

    waitForJobs({ webTestClient.manageAllocations(withActivate = true) }, 3)

    with(allocationRepository.findAll()) {
      prisoner("G4508UU") isStatus ACTIVE
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

  private fun WebTestClient.manageAllocations(withActivate: Boolean = false, withDeallocateEnding: Boolean = false, withDeallocateExpiring: Boolean = false, numJobs: Int = 1) {
    post()
      .uri("/job/manage-allocations?withActivate=$withActivate&withDeallocateEnding=$withDeallocateEnding&withDeallocateExpiring=$withDeallocateExpiring")
      .accept(MediaType.TEXT_PLAIN)
      .exchange()
      .expectStatus().isCreated
  }
}
