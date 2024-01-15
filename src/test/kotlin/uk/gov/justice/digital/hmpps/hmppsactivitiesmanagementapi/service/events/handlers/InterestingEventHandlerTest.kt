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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.InmateDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventReview
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
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
    whenever(rolloutPrisonRepository.findByCode(PENTONVILLE_PRISON_CODE)) doReturn rolloutPrison()
    whenever(eventReviewRepository.saveAndFlush(any<EventReview>())) doReturn EventReview(eventReviewId = 1)
  }

  @Test
  fun `stores an interesting event when active allocations exist`() {
    mockPrisoner()

    val activeAllocations =
      listOf(allocation().copy(allocationId = 1, prisonerNumber = "123456", prisonerStatus = PrisonerStatus.ACTIVE))
    mockAllocations(PENTONVILLE_PRISON_CODE, "123456", activeAllocations)

    val inboundEvent = cellMoveEvent("123456")

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(rolloutPrisonRepository).findByCode(PENTONVILLE_PRISON_CODE)
    verify(allocationRepository).findByPrisonCodePrisonerNumberPrisonerStatus(PENTONVILLE_PRISON_CODE, "123456", PrisonerStatus.ACTIVE, PrisonerStatus.PENDING)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo "Cell move for Bobson, Bob (123456)"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.CELL_MOVE.eventType
      prisonCode isEqualTo PENTONVILLE_PRISON_CODE
      prisonerNumber isEqualTo "123456"
    }
  }

  @Test
  fun `stores an interesting event when pending allocations exist`() {
    mockPrisoner(bookId = 2)

    val activeAllocations =
      listOf(allocation().copy(allocationId = 1, prisonerNumber = "123456", prisonerStatus = PrisonerStatus.PENDING))
    mockAllocations(PENTONVILLE_PRISON_CODE, "123456", activeAllocations)

    val inboundEvent = cellMoveEvent("123456")

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(rolloutPrisonRepository).findByCode(PENTONVILLE_PRISON_CODE)
    verify(allocationRepository).findByPrisonCodePrisonerNumberPrisonerStatus(PENTONVILLE_PRISON_CODE, "123456", PrisonerStatus.ACTIVE, PrisonerStatus.PENDING)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 2
      eventData isEqualTo "Cell move for Bobson, Bob (123456)"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.CELL_MOVE.eventType
      prisonCode isEqualTo PENTONVILLE_PRISON_CODE
      prisonerNumber isEqualTo "123456"
    }
  }

  @Test
  fun `stores an iep-review-inserted event despite the reason and prisonId being null`() {
    mockPrisoner(firstname = "Bobby")

    val activeAllocations = listOf(allocation().copy(allocationId = 1, prisonerNumber = "123456"))
    mockAllocations(PENTONVILLE_PRISON_CODE, "123456", activeAllocations)

    val inboundEvent = iepReviewInsertedEvent("123456")

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(rolloutPrisonRepository).findByCode(PENTONVILLE_PRISON_CODE)
    verify(allocationRepository).findByPrisonCodePrisonerNumberPrisonerStatus(PENTONVILLE_PRISON_CODE, "123456", PrisonerStatus.ACTIVE, PrisonerStatus.PENDING)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo "Incentive review created for Bobson, Bobby (123456)"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.INCENTIVES_INSERTED.eventType
      prisonCode isEqualTo PENTONVILLE_PRISON_CODE
      prisonerNumber isEqualTo "123456"
    }
  }

  @Test
  fun `stores a received event when allocations exist`() {
    mockPrisoner(lastname = "Geldof")

    val activeAllocations = listOf(allocation().copy(allocationId = 1, prisonerNumber = "123456"))
    mockAllocations(PENTONVILLE_PRISON_CODE, "123456", activeAllocations)

    val inboundEvent = offenderReceivedFromTemporaryAbsence(PENTONVILLE_PRISON_CODE, "123456")

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(rolloutPrisonRepository).findByCode(PENTONVILLE_PRISON_CODE)
    verify(allocationRepository).findByPrisonCodePrisonerNumberPrisonerStatus(PENTONVILLE_PRISON_CODE, "123456", PrisonerStatus.ACTIVE, PrisonerStatus.PENDING)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo "Prisoner received into prison PVI, Geldof, Bob (123456)"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.OFFENDER_RECEIVED.eventType
      prisonCode isEqualTo PENTONVILLE_PRISON_CODE
      prisonerNumber isEqualTo "123456"
    }
  }

  @Test
  fun `stores an offender released event`() {
    // Note prison code is different to that of the event because they have been release to Pentonville
    mockPrisoner(prisonCode = PENTONVILLE_PRISON_CODE)
    whenever(rolloutPrisonRepository.findByCode(MOORLAND_PRISON_CODE)) doReturn rolloutPrison()
    val inboundEvent = offenderReleasedEvent(MOORLAND_PRISON_CODE, "123456")

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(rolloutPrisonRepository).findByCode(MOORLAND_PRISON_CODE)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo "Prisoner released from prison MDI, Bobson, Bob (123456)"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.OFFENDER_RELEASED.eventType
      prisonCode isEqualTo MOORLAND_PRISON_CODE
      prisonerNumber isEqualTo "123456"
    }
  }

  @Test
  fun `stores an alerts updated event`() {
    mockPrisoner(prisonerNum = "ABC1234")

    val activeAllocations = listOf(allocation().copy(allocationId = 1, prisonerNumber = "ABC1234"))
    mockAllocations(PENTONVILLE_PRISON_CODE, "ABC1234", activeAllocations)

    val inboundEvent = alertsUpdatedEvent(prisonerNumber = "ABC1234")

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(rolloutPrisonRepository).findByCode(PENTONVILLE_PRISON_CODE)
    verify(allocationRepository).findByPrisonCodePrisonerNumberPrisonerStatus(PENTONVILLE_PRISON_CODE, "ABC1234", PrisonerStatus.ACTIVE, PrisonerStatus.PENDING)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo "Alerts updated for Bobson, Bob (ABC1234)"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.ALERTS_UPDATED.eventType
      prisonCode isEqualTo PENTONVILLE_PRISON_CODE
      prisonerNumber isEqualTo "ABC1234"
    }
  }

  @Test
  fun `stores an activities changed event with action END`() {
    // Note prison code is different to that of the event because they have been release to Moorland
    mockPrisoner(prisonerNum = "ABC1234", prisonCode = MOORLAND_PRISON_CODE)
    val inboundEvent =
      activitiesChangedEvent(prisonId = PENTONVILLE_PRISON_CODE, prisonerNumber = "ABC1234", action = Action.END)

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(rolloutPrisonRepository).findByCode(PENTONVILLE_PRISON_CODE)
    verifyNoInteractions(allocationRepository)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo "Activities changed 'END' from prison PVI, for Bobson, Bob (ABC1234)"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.ACTIVITIES_CHANGED.eventType
      prisonCode isEqualTo PENTONVILLE_PRISON_CODE
      prisonerNumber isEqualTo "ABC1234"
    }
  }

  @Test
  fun `stores an activities changed event with action SUSPEND`() {
    mockPrisoner(prisonerNum = "ABC1234")
    val inboundEvent =
      activitiesChangedEvent(prisonId = PENTONVILLE_PRISON_CODE, prisonerNumber = "ABC1234", action = Action.SUSPEND)

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(rolloutPrisonRepository).findByCode(PENTONVILLE_PRISON_CODE)
    verifyNoInteractions(allocationRepository)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo "Activities changed 'SUSPEND' from prison PVI, for Bobson, Bob (ABC1234)"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.ACTIVITIES_CHANGED.eventType
      prisonCode isEqualTo PENTONVILLE_PRISON_CODE
      prisonerNumber isEqualTo "ABC1234"
    }
  }

  @Test
  fun `stores an appointments changed event with action YES`() {
    mockPrisoner(prisonerNum = "ABC1234")
    val inboundEvent =
      appointmentsChangedEvent(prisonId = PENTONVILLE_PRISON_CODE, prisonerNumber = "ABC1234", action = "YES")

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(rolloutPrisonRepository).findByCode(PENTONVILLE_PRISON_CODE)
    verifyNoInteractions(allocationRepository)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo "Appointments changed 'YES' from prison PVI, for Bobson, Bob (ABC1234)"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.APPOINTMENTS_CHANGED.eventType
      prisonCode isEqualTo PENTONVILLE_PRISON_CODE
      prisonerNumber isEqualTo "ABC1234"
    }
  }

  @Test
  fun `ignores prisoners in prisons which are not rolled out`() {
    mockPrisoner()
    val inboundEvent = cellMoveEvent("123456")
    rolloutPrisonRepository.stub {
      on { findByCode(PENTONVILLE_PRISON_CODE) } doReturn
        rolloutPrison().copy(
          code = PENTONVILLE_PRISON_CODE,
          activitiesToBeRolledOut = false,
          activitiesRolloutDate = null,
        )
    }

    handler.handle(inboundEvent).also { it.isSuccess() isBool false }

    verify(rolloutPrisonRepository).findByCode(PENTONVILLE_PRISON_CODE)
    verifyNoInteractions(allocationRepository)
    verifyNoInteractions(eventReviewRepository)
  }

  @Test
  fun `ignores events for a prisoner with no active allocations`() {
    mockPrisoner()
    mockAllocations(PENTONVILLE_PRISON_CODE, "123456", emptyList())
    val inboundEvent = cellMoveEvent("123456")

    handler.handle(inboundEvent).also { it.isSuccess() isBool false }

    verify(rolloutPrisonRepository).findByCode(PENTONVILLE_PRISON_CODE)
    verify(allocationRepository).findByPrisonCodePrisonerNumberPrisonerStatus(PENTONVILLE_PRISON_CODE, "123456", PrisonerStatus.ACTIVE, PrisonerStatus.PENDING)
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
    prisonCode: String = PENTONVILLE_PRISON_CODE,
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

    whenever(prisonApiClient.getPrisonerDetailsLite(prisonerNum)) doReturn prisoner
  }
}
