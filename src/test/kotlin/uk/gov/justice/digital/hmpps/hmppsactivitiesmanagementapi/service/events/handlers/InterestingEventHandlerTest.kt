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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventReview
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventReviewDescription
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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.iepReviewDeletedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.iepReviewInsertedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.iepReviewUpdatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.nonAssociationsChangedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.offenderMergedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.prisonerReceivedFromTemporaryAbsence
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.prisonerReleasedEvent

class InterestingEventHandlerTest {
  private val rolloutPrisonRepository: RolloutPrisonRepository = mock()
  private val allocationRepository: AllocationRepository = mock()
  private val eventReviewRepository: EventReviewRepository = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiApplicationClient = mock()
  private val eventReviewCaptor = argumentCaptor<EventReview>()

  private val handler =
    InterestingEventHandler(
      rolloutPrisonRepository,
      allocationRepository,
      eventReviewRepository,
      prisonerSearchApiClient,
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
      eventData isEqualTo "Cell move"
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
      eventData isEqualTo "Cell move"
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
      eventData isEqualTo "Incentive review created"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.INCENTIVES_INSERTED.eventType
      prisonCode isEqualTo PENTONVILLE_PRISON_CODE
      prisonerNumber isEqualTo "123456"
    }
  }

  @Test
  fun `stores a prisoner received event when allocations exist`() {
    mockPrisoner(lastname = "Geldof")

    val activeAllocations = listOf(allocation().copy(allocationId = 1, prisonerNumber = "123456"))
    mockAllocations(PENTONVILLE_PRISON_CODE, "123456", activeAllocations)

    val inboundEvent = prisonerReceivedFromTemporaryAbsence(PENTONVILLE_PRISON_CODE, "123456")

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(rolloutPrisonRepository).findByCode(PENTONVILLE_PRISON_CODE)
    verify(allocationRepository).findByPrisonCodePrisonerNumberPrisonerStatus(PENTONVILLE_PRISON_CODE, "123456", PrisonerStatus.ACTIVE, PrisonerStatus.PENDING)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo "Prisoner received"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.PRISONER_RECEIVED.eventType
      prisonCode isEqualTo PENTONVILLE_PRISON_CODE
      prisonerNumber isEqualTo "123456"
    }
  }

  @Test
  fun `stores a permanent prisoner released event`() {
    // Note prison code is different to that of the event because they have been release to Pentonville
    mockPrisoner(prisonCode = PENTONVILLE_PRISON_CODE)
    whenever(rolloutPrisonRepository.findByCode(MOORLAND_PRISON_CODE)) doReturn rolloutPrison()
    val inboundEvent = prisonerReleasedEvent(MOORLAND_PRISON_CODE, "123456")

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(rolloutPrisonRepository).findByCode(MOORLAND_PRISON_CODE)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo "Prisoner released"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.PRISONER_RELEASED.eventType
      prisonCode isEqualTo MOORLAND_PRISON_CODE
      prisonerNumber isEqualTo "123456"
      eventDescription isEqualTo EventReviewDescription.PERMANENT_RELEASE
    }
  }

  @Test
  fun `stores a temporary prisoner released event`() {
    // Note prison code is different to that of the event because they have been release to Pentonville
    mockPrisoner(prisonCode = PENTONVILLE_PRISON_CODE)
    whenever(rolloutPrisonRepository.findByCode(MOORLAND_PRISON_CODE)) doReturn rolloutPrison()
    val inboundEvent = prisonerReleasedEvent(MOORLAND_PRISON_CODE, "123456", reason = "TEMPORARY_ABSENCE_RELEASE")

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(rolloutPrisonRepository).findByCode(MOORLAND_PRISON_CODE)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo "Prisoner released"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.PRISONER_RELEASED.eventType
      prisonCode isEqualTo MOORLAND_PRISON_CODE
      prisonerNumber isEqualTo "123456"
      eventDescription isEqualTo EventReviewDescription.TEMPORARY_RELEASE
    }
  }

  @Test
  fun `stores a prisoner released event which is not handled as temporary or permanent`() {
    // Note prison code is different to that of the event because they have been release to Pentonville
    mockPrisoner(prisonCode = PENTONVILLE_PRISON_CODE)
    whenever(rolloutPrisonRepository.findByCode(MOORLAND_PRISON_CODE)) doReturn rolloutPrison()
    val inboundEvent = prisonerReleasedEvent(MOORLAND_PRISON_CODE, "123456", reason = "UNEXPECTED")

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(rolloutPrisonRepository).findByCode(MOORLAND_PRISON_CODE)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo "Prisoner released"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.PRISONER_RELEASED.eventType
      prisonCode isEqualTo MOORLAND_PRISON_CODE
      prisonerNumber isEqualTo "123456"
      eventDescription isEqualTo EventReviewDescription.RELEASED
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
      eventData isEqualTo "Alerts updated"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.ALERTS_UPDATED.eventType
      prisonCode isEqualTo PENTONVILLE_PRISON_CODE
      prisonerNumber isEqualTo "ABC1234"
    }
  }

  @Test
  fun `stores an Non Associations updated event`() {
    mockPrisoner(prisonerNum = "ABC1234")

    val activeAllocations = listOf(allocation().copy(allocationId = 1, prisonerNumber = "ABC1234"))
    mockAllocations(PENTONVILLE_PRISON_CODE, "ABC1234", activeAllocations)

    val inboundEvent = nonAssociationsChangedEvent(prisonerNumber = "ABC1234")

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(rolloutPrisonRepository).findByCode(PENTONVILLE_PRISON_CODE)
    verify(allocationRepository).findByPrisonCodePrisonerNumberPrisonerStatus(PENTONVILLE_PRISON_CODE, "ABC1234", PrisonerStatus.ACTIVE, PrisonerStatus.PENDING)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo "Non-associations changed"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.NON_ASSOCIATIONS.eventType
      prisonCode isEqualTo PENTONVILLE_PRISON_CODE
      prisonerNumber isEqualTo "ABC1234"
    }
  }

  @Test
  fun `stores an incentives inserted event`() {
    mockPrisoner(prisonerNum = "ABC1234")

    val activeAllocations = listOf(allocation().copy(allocationId = 1, prisonerNumber = "ABC1234"))
    mockAllocations(PENTONVILLE_PRISON_CODE, "ABC1234", activeAllocations)

    val inboundEvent = iepReviewInsertedEvent(prisonerNumber = "ABC1234")

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(rolloutPrisonRepository).findByCode(PENTONVILLE_PRISON_CODE)
    verify(allocationRepository).findByPrisonCodePrisonerNumberPrisonerStatus(PENTONVILLE_PRISON_CODE, "ABC1234", PrisonerStatus.ACTIVE, PrisonerStatus.PENDING)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo "Incentive review created"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.INCENTIVES_INSERTED.eventType
      prisonCode isEqualTo PENTONVILLE_PRISON_CODE
      prisonerNumber isEqualTo "ABC1234"
    }
  }

  @Test
  fun `stores an incentives updated event`() {
    mockPrisoner(prisonerNum = "ABC1234")

    val activeAllocations = listOf(allocation().copy(allocationId = 1, prisonerNumber = "ABC1234"))
    mockAllocations(PENTONVILLE_PRISON_CODE, "ABC1234", activeAllocations)

    val inboundEvent = iepReviewUpdatedEvent(prisonerNumber = "ABC1234")

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(rolloutPrisonRepository).findByCode(PENTONVILLE_PRISON_CODE)
    verify(allocationRepository).findByPrisonCodePrisonerNumberPrisonerStatus(PENTONVILLE_PRISON_CODE, "ABC1234", PrisonerStatus.ACTIVE, PrisonerStatus.PENDING)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo "Incentive review updated"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.INCENTIVES_UPDATED.eventType
      prisonCode isEqualTo PENTONVILLE_PRISON_CODE
      prisonerNumber isEqualTo "ABC1234"
    }
  }

  @Test
  fun `stores an incentives deleted event`() {
    mockPrisoner(prisonerNum = "ABC1234")

    val activeAllocations = listOf(allocation().copy(allocationId = 1, prisonerNumber = "ABC1234"))
    mockAllocations(PENTONVILLE_PRISON_CODE, "ABC1234", activeAllocations)

    val inboundEvent = iepReviewDeletedEvent(prisonerNumber = "ABC1234")

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(rolloutPrisonRepository).findByCode(PENTONVILLE_PRISON_CODE)
    verify(allocationRepository).findByPrisonCodePrisonerNumberPrisonerStatus(PENTONVILLE_PRISON_CODE, "ABC1234", PrisonerStatus.ACTIVE, PrisonerStatus.PENDING)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo "Incentive review deleted"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.INCENTIVES_DELETED.eventType
      prisonCode isEqualTo PENTONVILLE_PRISON_CODE
      prisonerNumber isEqualTo "ABC1234"
    }
  }

  @Test
  fun `stores an offender merged event`() {
    mockPrisoner(prisonerNum = "ABC1234")

    val inboundEvent = offenderMergedEvent(prisonerNumber = "ABC1234", removedPrisonerNumber = "DEF9876")

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(rolloutPrisonRepository).findByCode(PENTONVILLE_PRISON_CODE)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo "Prisoner merged from 'DEF9876' to 'ABC1234'"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.OFFENDER_MERGED.eventType
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
      eventData isEqualTo "Activities changed"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.ACTIVITIES_CHANGED.eventType
      prisonCode isEqualTo PENTONVILLE_PRISON_CODE
      prisonerNumber isEqualTo "ABC1234"
      eventDescription isEqualTo EventReviewDescription.ACTIVITY_ENDED
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
      eventData isEqualTo "Activities changed"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.ACTIVITIES_CHANGED.eventType
      prisonCode isEqualTo PENTONVILLE_PRISON_CODE
      prisonerNumber isEqualTo "ABC1234"
      eventDescription isEqualTo EventReviewDescription.ACTIVITY_SUSPENDED
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
      eventData isEqualTo "Appointments changed 'YES'"
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
    val prisoner: Prisoner = mock {
      on { prisonId } doReturn prisonCode
      on { prisonerNumber } doReturn prisonerNum
      on { firstName } doReturn firstname
      on { lastName } doReturn lastname
      on { bookingId } doReturn bookId.toString()
    }

    whenever(prisonerSearchApiClient.findByPrisonerNumber(prisonerNum)) doReturn prisoner
  }
}
