package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.InmateDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventReview
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.pentonvillePrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.rolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventReviewRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.Action
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.InboundEventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.activitiesChangedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.alertsUpdatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.appointmentsChangedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.cellMoveEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.iepReviewInsertedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.offenderReceivedFromTemporaryAbsence
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.offenderReleasedEvent

class InterestingEventHandlerTest {
  private val rolloutPrisonRepository: RolloutPrisonRepository = mock()
  private val allocationRepository: AllocationRepository = mock()
  private val eventReviewRepository: EventReviewRepository = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiApplicationClient = mock()
  private val prisonApiClient: PrisonApiApplicationClient = mock()
  private val eventReviewCaptor = argumentCaptor<EventReview>()

  private val handler =
    InterestingEventHandler(
      rolloutPrisonRepository,
      allocationRepository,
      eventReviewRepository,
      prisonApiClient,
    )

  @BeforeEach
  fun beforeTests() {
    whenever(rolloutPrisonRepository.findByCode(pentonvillePrisonCode)) doReturn rolloutPrison()
    whenever(eventReviewRepository.saveAndFlush(any<EventReview>())) doReturn EventReview(eventReviewId = 1)
  }

  @Test
  fun `stores an interesting event when active allocations exist`() {
    mockPrisoner()

    val activeAllocations =
      listOf(allocation().copy(allocationId = 1, prisonerNumber = "123456", prisonerStatus = PrisonerStatus.ACTIVE))
    mockAllocations(pentonvillePrisonCode, "123456", activeAllocations)

    val inboundEvent = cellMoveEvent("123456")

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(rolloutPrisonRepository).findByCode(pentonvillePrisonCode)
    verify(allocationRepository).findByPrisonCodePrisonerNumberPrisonerStatus(pentonvillePrisonCode, "123456", PrisonerStatus.ACTIVE, PrisonerStatus.PENDING)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo "Cell move for Bobson, Bob (123456)"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.CELL_MOVE.eventType
      prisonCode isEqualTo pentonvillePrisonCode
      prisonerNumber isEqualTo "123456"
    }
  }

  @Test
  fun `stores an interesting event when pending allocations exist`() {
    mockPrisoner(bookId = 2)

    val activeAllocations =
      listOf(allocation().copy(allocationId = 1, prisonerNumber = "123456", prisonerStatus = PrisonerStatus.PENDING))
    mockAllocations(pentonvillePrisonCode, "123456", activeAllocations)

    val inboundEvent = cellMoveEvent("123456")

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(rolloutPrisonRepository).findByCode(pentonvillePrisonCode)
    verify(allocationRepository).findByPrisonCodePrisonerNumberPrisonerStatus(pentonvillePrisonCode, "123456", PrisonerStatus.ACTIVE, PrisonerStatus.PENDING)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 2
      eventData isEqualTo "Cell move for Bobson, Bob (123456)"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.CELL_MOVE.eventType
      prisonCode isEqualTo pentonvillePrisonCode
      prisonerNumber isEqualTo "123456"
    }
  }

  @Test
  fun `stores an iep-review-inserted event despite the reason and prisonId being null`() {
    mockPrisoner(firstname = "Bobby")

    val activeAllocations = listOf(allocation().copy(allocationId = 1, prisonerNumber = "123456"))
    mockAllocations(pentonvillePrisonCode, "123456", activeAllocations)

    val inboundEvent = iepReviewInsertedEvent("123456")

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(rolloutPrisonRepository).findByCode(pentonvillePrisonCode)
    verify(allocationRepository).findByPrisonCodePrisonerNumberPrisonerStatus(pentonvillePrisonCode, "123456", PrisonerStatus.ACTIVE, PrisonerStatus.PENDING)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo "Incentive review created for Bobson, Bobby (123456)"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.INCENTIVES_INSERTED.eventType
      prisonCode isEqualTo pentonvillePrisonCode
      prisonerNumber isEqualTo "123456"
    }
  }

  @Test
  fun `stores a received event when allocations exist`() {
    mockPrisoner(lastname = "Geldof")

    val activeAllocations = listOf(allocation().copy(allocationId = 1, prisonerNumber = "123456"))
    mockAllocations(pentonvillePrisonCode, "123456", activeAllocations)

    val inboundEvent = offenderReceivedFromTemporaryAbsence(pentonvillePrisonCode, "123456")

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(rolloutPrisonRepository).findByCode(pentonvillePrisonCode)
    verify(allocationRepository).findByPrisonCodePrisonerNumberPrisonerStatus(pentonvillePrisonCode, "123456", PrisonerStatus.ACTIVE, PrisonerStatus.PENDING)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo "Prisoner received into prison PVI, Geldof, Bob (123456)"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.OFFENDER_RECEIVED.eventType
      prisonCode isEqualTo pentonvillePrisonCode
      prisonerNumber isEqualTo "123456"
    }
  }

  @Test
  fun `stores an offender released event`() {
    // Note prison code is different to that of the event because they have been release to Pentonville
    mockPrisoner(prisonCode = pentonvillePrisonCode)
    whenever(rolloutPrisonRepository.findByCode(moorlandPrisonCode)) doReturn rolloutPrison()
    val inboundEvent = offenderReleasedEvent(moorlandPrisonCode, "123456")

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(rolloutPrisonRepository).findByCode(moorlandPrisonCode)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo "Prisoner released from prison MDI, Bobson, Bob (123456)"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.OFFENDER_RELEASED.eventType
      prisonCode isEqualTo moorlandPrisonCode
      prisonerNumber isEqualTo "123456"
    }
  }

  @Test
  fun `stores an alerts updated event`() {
    mockPrisoner(prisonerNum = "ABC1234")

    val activeAllocations = listOf(allocation().copy(allocationId = 1, prisonerNumber = "ABC1234"))
    mockAllocations(pentonvillePrisonCode, "ABC1234", activeAllocations)

    val inboundEvent = alertsUpdatedEvent(prisonerNumber = "ABC1234")

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(rolloutPrisonRepository).findByCode(pentonvillePrisonCode)
    verify(allocationRepository).findByPrisonCodePrisonerNumberPrisonerStatus(pentonvillePrisonCode, "ABC1234", PrisonerStatus.ACTIVE, PrisonerStatus.PENDING)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo "Alerts updated for Bobson, Bob (ABC1234)"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.ALERTS_UPDATED.eventType
      prisonCode isEqualTo pentonvillePrisonCode
      prisonerNumber isEqualTo "ABC1234"
    }
  }

  @Test
  fun `stores an activities changed event with action END`() {
    // Note prison code is different to that of the event because they have been release to Moorland
    mockPrisoner(prisonerNum = "ABC1234", prisonCode = moorlandPrisonCode)
    val inboundEvent =
      activitiesChangedEvent(prisonId = pentonvillePrisonCode, prisonerNumber = "ABC1234", action = Action.END)

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(rolloutPrisonRepository).findByCode(pentonvillePrisonCode)
    verifyNoInteractions(allocationRepository)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo "Activities changed 'END' from prison PVI, for Bobson, Bob (ABC1234)"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.ACTIVITIES_CHANGED.eventType
      prisonCode isEqualTo pentonvillePrisonCode
      prisonerNumber isEqualTo "ABC1234"
    }
  }

  @Test
  fun `stores an activities changed event with action SUSPEND`() {
    mockPrisoner(prisonerNum = "ABC1234")
    val inboundEvent =
      activitiesChangedEvent(prisonId = pentonvillePrisonCode, prisonerNumber = "ABC1234", action = Action.SUSPEND)

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(rolloutPrisonRepository).findByCode(pentonvillePrisonCode)
    verifyNoInteractions(allocationRepository)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo "Activities changed 'SUSPEND' from prison PVI, for Bobson, Bob (ABC1234)"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.ACTIVITIES_CHANGED.eventType
      prisonCode isEqualTo pentonvillePrisonCode
      prisonerNumber isEqualTo "ABC1234"
    }
  }

  @Test
  fun `stores an appointments changed event with action YES`() {
    mockPrisoner(prisonerNum = "ABC1234")
    val inboundEvent =
      appointmentsChangedEvent(prisonId = pentonvillePrisonCode, prisonerNumber = "ABC1234", action = "YES")

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(rolloutPrisonRepository).findByCode(pentonvillePrisonCode)
    verifyNoInteractions(allocationRepository)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo "Appointments changed 'YES' from prison PVI, for Bobson, Bob (ABC1234)"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.APPOINTMENTS_CHANGED.eventType
      prisonCode isEqualTo pentonvillePrisonCode
      prisonerNumber isEqualTo "ABC1234"
    }
  }

  @Test
  fun `ignores prisoners in prisons which are not rolled out`() {
    mockPrisoner()
    val inboundEvent = cellMoveEvent("123456")
    rolloutPrisonRepository.stub {
      on { findByCode(pentonvillePrisonCode) } doReturn
        rolloutPrison().copy(
          code = pentonvillePrisonCode,
          activitiesToBeRolledOut = false,
          activitiesRolloutDate = null,
        )
    }

    handler.handle(inboundEvent).also { it.isSuccess() isBool false }

    verify(rolloutPrisonRepository).findByCode(pentonvillePrisonCode)
    verifyNoInteractions(allocationRepository)
    verifyNoInteractions(eventReviewRepository)
  }

  @Test
  fun `ignores events for a prisoner with no active allocations`() {
    mockPrisoner()
    mockAllocations(pentonvillePrisonCode, "123456", emptyList())
    val inboundEvent = cellMoveEvent("123456")

    handler.handle(inboundEvent).also { it.isSuccess() isBool false }

    verify(rolloutPrisonRepository).findByCode(pentonvillePrisonCode)
    verify(allocationRepository).findByPrisonCodePrisonerNumberPrisonerStatus(pentonvillePrisonCode, "123456", PrisonerStatus.ACTIVE, PrisonerStatus.PENDING)
    verifyNoInteractions(eventReviewRepository)
  }

  private fun mockAllocations(prisonCode: String, prisonerNumber: String, allocations: List<Allocation>) {
    whenever(
      allocationRepository.findByPrisonCodePrisonerNumberPrisonerStatus(
        prisonCode,
        prisonerNumber,
        PrisonerStatus.ACTIVE,
        PrisonerStatus.PENDING,
      ),
    ) doReturn allocations
  }

  private fun mockPrisoner(
    prisonCode: String = pentonvillePrisonCode,
    prisonerNum: String = "123456",
    firstname: String = "Bob",
    lastname: String = "Bobson",
    bookId: Long = 1,
  ) {
    val prisoner: InmateDetail = mock {
      on { agencyId } doReturn prisonCode
      on { offenderNo } doReturn prisonerNum
      on { firstName } doReturn firstname
      on { lastName } doReturn lastname
      on { bookingId } doReturn bookId
    }

    whenever(prisonApiClient.getPrisonerDetails(prisonerNum)) doReturn Mono.just(prisoner)
  }
}
