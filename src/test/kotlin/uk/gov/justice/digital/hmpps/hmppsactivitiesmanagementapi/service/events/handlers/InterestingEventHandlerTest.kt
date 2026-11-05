package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.BedAssignment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventReviewRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.Action
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.InboundEventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.activitiesChangedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.alertsUpdatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.alertsUpdatedEventAfterMerge
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.appointmentsChangedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.iepReviewDeletedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.iepReviewInsertedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.iepReviewUpdatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.nonAssociationsChangedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.offenderMergedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.prisonerReceivedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.prisonerReleasedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.prisonerUpdatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService
import java.nio.charset.StandardCharsets

class InterestingEventHandlerTest {
  private val rolloutPrisonService = RolloutPrisonService("PVI,MDI", "PVI,MDI", "", "PVI,MDI")
  private val allocationRepository: AllocationRepository = mock()
  private val eventReviewRepository: EventReviewRepository = mock()
  private val prisonApiClient: PrisonApiClient = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val eventReviewCaptor = argumentCaptor<EventReview>()

  private val handler =
    InterestingEventHandler(
      rolloutPrisonService,
      allocationRepository,
      eventReviewRepository,
      prisonApiClient,
      prisonerSearchApiClient,
    )

  private fun webClientResponseException(statusCode: Int): WebClientResponseException = WebClientResponseException.create(
    statusCode,
    "Error",
    HttpHeaders(),
    byteArrayOf(),
    StandardCharsets.UTF_8,
  )

  @BeforeEach
  fun beforeTests() {
    whenever(eventReviewRepository.saveAndFlush(any<EventReview>())) doReturn EventReview(eventReviewId = 1)
  }

  @Test
  fun `does not store an interesting event when active allocations exist but cell move data is unavailable`() {
    mockPrisoner()
    runBlocking {
      whenever(prisonApiClient.getCellHistoryForPrisoner(1L)).thenReturn(emptyList())
    }
    val activeAllocations =
      listOf(allocation().copy(allocationId = 1, prisonerNumber = "123456", prisonerStatus = PrisonerStatus.ACTIVE))
    mockAllocations(PENTONVILLE_PRISON_CODE, "123456", activeAllocations)

    val inboundEvent = prisonerUpdatedEvent("123456")

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(allocationRepository).findByPrisonCodePrisonerNumberPrisonerStatus(PENTONVILLE_PRISON_CODE, "123456", PrisonerStatus.ACTIVE, PrisonerStatus.PENDING)
    verify(eventReviewRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `does not store an interesting event when pending allocations exist but cell move data is unavailable`() {
    mockPrisoner(bookId = 2)

    runBlocking {
      whenever(prisonApiClient.getCellHistoryForPrisoner(any())).thenReturn(emptyList())
    }

    val activeAllocations =
      listOf(allocation().copy(allocationId = 1, prisonerNumber = "123456", prisonerStatus = PrisonerStatus.PENDING))
    mockAllocations(PENTONVILLE_PRISON_CODE, "123456", activeAllocations)

    val inboundEvent = prisonerUpdatedEvent("123456", listOf("LOCATION", "SENTENCE"))

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(allocationRepository).findByPrisonCodePrisonerNumberPrisonerStatus(PENTONVILLE_PRISON_CODE, "123456", PrisonerStatus.ACTIVE, PrisonerStatus.PENDING)
    verify(eventReviewRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `does not store an interesting event when cell history is empty`() {
    mockPrisoner(bookId = 2)
    mockAllocations(
      PENTONVILLE_PRISON_CODE,
      "123456",
      listOf(allocation().copy(allocationId = 1, prisonerNumber = "123456", prisonerStatus = PrisonerStatus.ACTIVE)),
    )

    runBlocking {
      whenever(prisonApiClient.getCellHistoryForPrisoner(2L))
        .thenReturn(emptyList())
    }

    handler.handle(prisonerUpdatedEvent("123456")).also { it.isSuccess() isBool true }

    verify(eventReviewRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `fails interesting event when cell move data cannot be retrieved from prison api`() {
    mockPrisoner(bookId = 2)
    mockAllocations(
      PENTONVILLE_PRISON_CODE,
      "123456",
      listOf(allocation().copy(allocationId = 1, prisonerNumber = "123456", prisonerStatus = PrisonerStatus.ACTIVE)),
    )

    runBlocking {
      whenever(prisonApiClient.getCellHistoryForPrisoner(2L))
        .thenThrow(webClientResponseException(500))
    }

    handler.handle(prisonerUpdatedEvent("123456")).also { it.isSuccess() isBool false }

    verify(eventReviewRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `stores none and current cell locations for cell move events if no previous record exists`() {
    mockPrisoner(
      prisonCode = MOORLAND_PRISON_CODE,
      prisonerNum = "676767",
      bookId = 2,
    )
    mockAllocations(
      MOORLAND_PRISON_CODE,
      "676767",
      listOf(allocation().copy(allocationId = 1, prisonerNumber = "676767", prisonerStatus = PrisonerStatus.ACTIVE)),
    )
    val currentAssignment = mock<BedAssignment> {
      on { description } doReturn "MDI-1-1-001"
      on { assignmentDateTime } doReturn "2026-10-12T12:40:13"
      on { assignmentEndDateTime } doReturn null
      on { bedAssignmentHistorySequence } doReturn 1
    }

    val activeAllocations =
      listOf(allocation().copy(allocationId = 1, prisonerNumber = "676767", prisonerStatus = PrisonerStatus.PENDING))
    mockAllocations(MOORLAND_PRISON_CODE, "676767", activeAllocations)

    runBlocking {
      whenever(prisonApiClient.getCellHistoryForPrisoner(2L))
        .thenReturn(listOf(currentAssignment))
    }

    handler.handle(prisonerUpdatedEvent("676767")).also { it.isSuccess() isBool true }

    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      eventData isEqualTo "None;MDI-1-1-001"
    }
  }

  @Test
  fun `stores previous and current cell locations for cell move events`() {
    mockPrisoner(bookId = 2)
    mockAllocations(
      PENTONVILLE_PRISON_CODE,
      "123456",
      listOf(allocation().copy(allocationId = 1, prisonerNumber = "123456", prisonerStatus = PrisonerStatus.ACTIVE)),
    )
    val currentAssignment = mock<BedAssignment> {
      on { description } doReturn "PVI-1-1-002"
      on { assignmentDateTime } doReturn "2026-10-12T12:40:13"
      on { assignmentEndDateTime } doReturn null
      on { bedAssignmentHistorySequence } doReturn 2
    }
    val previousAssignment = mock<BedAssignment> {
      on { description } doReturn "PVI-1-1-001"
      on { assignmentDateTime } doReturn "2026-10-12T12:40:13"
      on { assignmentEndDateTime } doReturn "2026-10-12T12:40:13"
      on { bedAssignmentHistorySequence } doReturn 1
    }
    runBlocking {
      whenever(prisonApiClient.getCellHistoryForPrisoner(2L))
        .thenReturn(listOf(currentAssignment, previousAssignment))
    }

    handler.handle(prisonerUpdatedEvent("123456")).also { it.isSuccess() isBool true }

    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      eventData isEqualTo "PVI-1-1-001;PVI-1-1-002"
    }
  }

  @Test
  fun `stores previous and current cell locations for out of order cell history`() {
    mockPrisoner(bookId = 2)
    mockAllocations(
      PENTONVILLE_PRISON_CODE,
      "123456",
      listOf(allocation().copy(allocationId = 1, prisonerNumber = "123456", prisonerStatus = PrisonerStatus.ACTIVE)),
    )
    val earlierAssignment = mock<BedAssignment> {
      on { description } doReturn "RSI-0-0-OLD"
      on { assignmentDateTime } doReturn "2026-10-12T12:39:52"
      on { assignmentEndDateTime } doReturn "2026-10-12T12:40:13"
      on { bedAssignmentHistorySequence } doReturn 1
    }
    val currentAssignment = mock<BedAssignment> {
      on { description } doReturn "RSI-9-9-NEW"
      on { assignmentDateTime } doReturn "2026-10-12T12:40:13"
      on { assignmentEndDateTime } doReturn null
      on { bedAssignmentHistorySequence } doReturn 2
    }
    val oldAssignment = mock<BedAssignment> {
      on { description } doReturn "RSI-A-N-2-18S"
      on { assignmentDateTime } doReturn "2026-10-09T17:57:30"
      on { assignmentEndDateTime } doReturn "2026-10-09T17:57:50"
      on { bedAssignmentHistorySequence } doReturn 0
    }

    runBlocking {
      whenever(prisonApiClient.getCellHistoryForPrisoner(2L))
        .thenReturn(listOf(earlierAssignment, currentAssignment, oldAssignment))
    }

    handler.handle(prisonerUpdatedEvent("123456")).also { it.isSuccess() isBool true }

    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      eventData isEqualTo "RSI-0-0-OLD;RSI-9-9-NEW"
    }
  }

  @Test
  fun `stores an iep-review-inserted event despite the reason and prisonId being null`() {
    mockPrisoner(firstname = "Bobby")

    val activeAllocations = listOf(allocation().copy(allocationId = 1, prisonerNumber = "123456"))
    mockAllocations(PENTONVILLE_PRISON_CODE, "123456", activeAllocations)

    val inboundEvent = iepReviewInsertedEvent("123456", incentiveLevelChanged = true, incentiveLevel = "STD", previousIncentiveLevel = "BAS")

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(allocationRepository).findByPrisonCodePrisonerNumberPrisonerStatus(PENTONVILLE_PRISON_CODE, "123456", PrisonerStatus.ACTIVE, PrisonerStatus.PENDING)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo "STD;BAS"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.INCENTIVES_INSERTED.eventType
      eventDescription isEqualTo EventReviewDescription.INCENTIVE_LEVEL_CHANGED
      prisonCode isEqualTo PENTONVILLE_PRISON_CODE
      prisonerNumber isEqualTo "123456"
    }
  }

  @ParameterizedTest
  @ValueSource(strings = ["NEW_ADMISSION", "READMISSION", "READMISSION_SWITCH_BOOKING", "TRANSFERRED", "POST_MERGE_ADMISSION", "TEMPORARY_ABSENCE_RETURN", "RETURN_FROM_COURT"])
  fun `stores a prisoner received event with an ARRIVAL_OR_RETURN description and the reason as event data`(reason: String) {
    mockPrisoner(lastname = "Geldof")

    val activeAllocations = listOf(allocation().copy(allocationId = 1, prisonerNumber = "123456"))
    mockAllocations(PENTONVILLE_PRISON_CODE, "123456", activeAllocations)

    val inboundEvent = prisonerReceivedEvent(PENTONVILLE_PRISON_CODE, "123456", reason = reason)

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(allocationRepository).findByPrisonCodePrisonerNumberPrisonerStatus(PENTONVILLE_PRISON_CODE, "123456", PrisonerStatus.ACTIVE, PrisonerStatus.PENDING)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo reason
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.PRISONER_RECEIVED.eventType
      prisonCode isEqualTo PENTONVILLE_PRISON_CODE
      prisonerNumber isEqualTo "123456"
      eventDescription isEqualTo EventReviewDescription.ARRIVAL_OR_RETURN
    }
  }

  @ParameterizedTest
  @ValueSource(strings = ["TEMPORARY_ABSENCE_RELEASE", "SENT_TO_COURT", "RELEASED", "RELEASED_TO_HOSPITAL"])
  fun `stores a prisoner released event with a RELEASED description and the reason as event data`(reason: String) {
    // Note prison code is different to that of the event because they have been release to Pentonville
    mockPrisoner(prisonCode = PENTONVILLE_PRISON_CODE)
    val inboundEvent = prisonerReleasedEvent(MOORLAND_PRISON_CODE, "123456", reason = reason)

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo reason
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.PRISONER_RELEASED.eventType
      prisonCode isEqualTo MOORLAND_PRISON_CODE
      prisonerNumber isEqualTo "123456"
      eventDescription isEqualTo EventReviewDescription.RELEASED
    }
  }

  @Test
  fun `stores a transferred prisoner released event with a TRANSFER_OUT description and the reason as event data`() {
    // Note prison code is different to that of the event because they have been release to Pentonville
    mockPrisoner(prisonCode = PENTONVILLE_PRISON_CODE)
    val inboundEvent = prisonerReleasedEvent(MOORLAND_PRISON_CODE, "123456", reason = "TRANSFERRED")

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo "TRANSFERRED"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.PRISONER_RELEASED.eventType
      prisonCode isEqualTo MOORLAND_PRISON_CODE
      prisonerNumber isEqualTo "123456"
      eventDescription isEqualTo EventReviewDescription.TRANSFER_OUT
    }
  }

  @Test
  fun `stores a prisoner released event which is not handled`() {
    // Note prison code is different to that of the event because they have been release to Pentonville
    mockPrisoner(prisonCode = PENTONVILLE_PRISON_CODE)
    val inboundEvent = prisonerReleasedEvent(MOORLAND_PRISON_CODE, "123456", reason = "UNEXPECTED")

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo "UNEXPECTED"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.PRISONER_RELEASED.eventType
      prisonCode isEqualTo MOORLAND_PRISON_CODE
      prisonerNumber isEqualTo "123456"
      eventDescription isEqualTo null
    }
  }

  @Test
  fun `stores an alerts updated event when alerts have been added and closed`() {
    mockPrisoner(prisonerNum = "ABC1234")

    val activeAllocations = listOf(allocation().copy(allocationId = 1, prisonerNumber = "ABC1234"))
    mockAllocations(PENTONVILLE_PRISON_CODE, "ABC1234", activeAllocations)

    val inboundEvent = alertsUpdatedEvent(prisonerNumber = "ABC1234")

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(allocationRepository).findByPrisonCodePrisonerNumberPrisonerStatus(PENTONVILLE_PRISON_CODE, "ABC1234", PrisonerStatus.ACTIVE, PrisonerStatus.PENDING)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo "A1,A2;R1,R2"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.ALERTS_UPDATED.eventType
      eventDescription isEqualTo EventReviewDescription.ALERTS_ADDED_AND_CLOSED
      prisonCode isEqualTo PENTONVILLE_PRISON_CODE
      prisonerNumber isEqualTo "ABC1234"
    }
  }

  @Test
  fun `uses found prisoner path when prisoner lookup succeeds`() {
    mockPrisoner(prisonerNum = "ABC1234")

    val activeAllocations = listOf(allocation().copy(allocationId = 1, prisonerNumber = "ABC1234"))
    mockAllocations(PENTONVILLE_PRISON_CODE, "ABC1234", activeAllocations)

    val inboundEvent = alertsUpdatedEvent(prisonerNumber = "ABC1234")

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(eventReviewRepository).saveAndFlush(any())
  }

  @Test
  fun `ignores stale alerts update after attempting prisoner search after alerts have been removed from a merged prisoner`() {
    whenever(prisonerSearchApiClient.findByPrisonerNumber("123OLD")) doThrow webClientResponseException(404)

    val inboundEvent = alertsUpdatedEventAfterMerge(prisonerNumber = "123OLD")

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(eventReviewRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `fails stale alerts update when prisoner lookup returns server error`() {
    whenever(prisonerSearchApiClient.findByPrisonerNumber("123OLD")) doThrow webClientResponseException(500)

    val inboundEvent = alertsUpdatedEventAfterMerge(prisonerNumber = "123OLD")

    handler.handle(inboundEvent).also { it.isSuccess() isBool false }

    verify(eventReviewRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `fails when prisoner lookup returns null for a normal alerts update`() {
    whenever(prisonerSearchApiClient.findByPrisonerNumber("123OLD")) doReturn null

    val inboundEvent = alertsUpdatedEvent(prisonerNumber = "123OLD")

    handler.handle(inboundEvent).also { it.isSuccess() isBool false }

    verify(eventReviewRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `fails when prisoner lookup returns a non-404 or 5xx client error`() {
    whenever(prisonerSearchApiClient.findByPrisonerNumber("123OLD")) doThrow webClientResponseException(400)

    val inboundEvent = alertsUpdatedEvent(prisonerNumber = "123OLD")

    handler.handle(inboundEvent).also { it.isSuccess() isBool false }

    verify(eventReviewRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `stores an alerts updated event when an alert has been added`() {
    mockPrisoner(prisonerNum = "ABC1234")

    val activeAllocations = listOf(allocation().copy(allocationId = 1, prisonerNumber = "ABC1234"))
    mockAllocations(PENTONVILLE_PRISON_CODE, "ABC1234", activeAllocations)

    val inboundEvent = alertsUpdatedEvent(prisonerNumber = "ABC1234", alertsAdded = setOf("A1", "A2"), alertsRemoved = emptySet())

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      eventData isEqualTo "A1,A2;"
      eventDescription isEqualTo EventReviewDescription.ALERT_ADDED
    }
  }

  @Test
  fun `stores an alerts updated event when an alert has been closed`() {
    mockPrisoner(prisonerNum = "ABC1234")

    val activeAllocations = listOf(allocation().copy(allocationId = 1, prisonerNumber = "ABC1234"))
    mockAllocations(PENTONVILLE_PRISON_CODE, "ABC1234", activeAllocations)

    val inboundEvent = alertsUpdatedEvent(prisonerNumber = "ABC1234", alertsAdded = emptySet(), alertsRemoved = setOf("R1", "R2"))

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      eventData isEqualTo ";R1,R2"
      eventDescription isEqualTo EventReviewDescription.ALERT_CLOSED
    }
  }

  @Test
  fun `does not store an alerts updated event when no alerts added or closed`() {
    val inboundEvent = alertsUpdatedEvent(prisonerNumber = "ABC1234", alertsAdded = emptySet(), alertsRemoved = emptySet())

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verifyNoInteractions(eventReviewRepository)
  }

  @Test
  fun `stores a Non Associations updated event`() {
    mockPrisoner(prisonerNum = "ABC1234")

    val activeAllocations = listOf(allocation().copy(allocationId = 1, prisonerNumber = "ABC1234"))
    mockAllocations(PENTONVILLE_PRISON_CODE, "ABC1234", activeAllocations)

    val inboundEvent = nonAssociationsChangedEvent(prisonerNumber = "ABC1234")

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(allocationRepository).findByPrisonCodePrisonerNumberPrisonerStatus(PENTONVILLE_PRISON_CODE, "ABC1234", PrisonerStatus.ACTIVE, PrisonerStatus.PENDING)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo "New non-association"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.NON_ASSOCIATIONS.eventType
      prisonCode isEqualTo PENTONVILLE_PRISON_CODE
      prisonerNumber isEqualTo "ABC1234"
      eventDescription isEqualTo EventReviewDescription.NON_ASSOCIATION
    }
  }

  @Test
  fun `stores an incentives inserted event when the incentive level changed`() {
    mockPrisoner(prisonerNum = "ABC1234")

    val activeAllocations = listOf(allocation().copy(allocationId = 1, prisonerNumber = "ABC1234"))
    mockAllocations(PENTONVILLE_PRISON_CODE, "ABC1234", activeAllocations)

    val inboundEvent = iepReviewInsertedEvent(
      prisonerNumber = "ABC1234",
      incentiveLevel = "STD",
      previousIncentiveLevel = "BAS",
      incentiveLevelChanged = true,
    )

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(allocationRepository).findByPrisonCodePrisonerNumberPrisonerStatus(PENTONVILLE_PRISON_CODE, "ABC1234", PrisonerStatus.ACTIVE, PrisonerStatus.PENDING)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo "STD;BAS"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.INCENTIVES_INSERTED.eventType
      eventDescription isEqualTo EventReviewDescription.INCENTIVE_LEVEL_CHANGED
      prisonCode isEqualTo PENTONVILLE_PRISON_CODE
      prisonerNumber isEqualTo "ABC1234"
    }
  }

  @Test
  fun `does not store an incentives inserted event when the incentive level did not change`() {
    val inboundEvent = iepReviewInsertedEvent(prisonerNumber = "ABC1234", incentiveLevelChanged = false)

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(eventReviewRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `stores an incentives updated event when the incentive level changed`() {
    mockPrisoner(prisonerNum = "ABC1234")

    val activeAllocations = listOf(allocation().copy(allocationId = 1, prisonerNumber = "ABC1234"))
    mockAllocations(PENTONVILLE_PRISON_CODE, "ABC1234", activeAllocations)

    val inboundEvent = iepReviewUpdatedEvent(
      prisonerNumber = "ABC1234",
      incentiveLevel = "BAS",
      previousIncentiveLevel = "STD",
      incentiveLevelChanged = true,
    )

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(allocationRepository).findByPrisonCodePrisonerNumberPrisonerStatus(PENTONVILLE_PRISON_CODE, "ABC1234", PrisonerStatus.ACTIVE, PrisonerStatus.PENDING)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo "BAS;STD"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.INCENTIVES_UPDATED.eventType
      eventDescription isEqualTo EventReviewDescription.INCENTIVE_LEVEL_CHANGED
      prisonCode isEqualTo PENTONVILLE_PRISON_CODE
      prisonerNumber isEqualTo "ABC1234"
    }
  }

  @Test
  fun `does not store an incentives updated event when the incentive level did not change`() {
    val inboundEvent = iepReviewUpdatedEvent(prisonerNumber = "ABC1234", incentiveLevelChanged = false)

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(eventReviewRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `does not store an incentives deleted event`() {
    val inboundEvent = iepReviewDeletedEvent(prisonerNumber = "ABC1234")

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(eventReviewRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `stores an offender merged event`() {
    mockPrisoner(prisonerNum = "ABC1234")

    val inboundEvent = offenderMergedEvent(prisonerNumber = "ABC1234", removedPrisonerNumber = "DEF9876")

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo "DEF9876;ABC1234"
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.OFFENDER_MERGED.eventType
      prisonCode isEqualTo PENTONVILLE_PRISON_CODE
      prisonerNumber isEqualTo "ABC1234"
      eventDescription isEqualTo EventReviewDescription.PRISONER_MERGED
    }
  }

  @Test
  fun `stores an activities changed event with action END`() {
    // Note prison code is different to that of the event because they have been release to Moorland
    mockPrisoner(prisonerNum = "ABC1234", prisonCode = MOORLAND_PRISON_CODE)
    val inboundEvent =
      activitiesChangedEvent(prisonId = PENTONVILLE_PRISON_CODE, prisonerNumber = "ABC1234", action = Action.END)

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verifyNoInteractions(allocationRepository)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo null
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

    verifyNoInteractions(allocationRepository)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo null
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

    verifyNoInteractions(allocationRepository)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo null
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.APPOINTMENTS_CHANGED.eventType
      prisonCode isEqualTo PENTONVILLE_PRISON_CODE
      prisonerNumber isEqualTo "ABC1234"
      eventDescription isEqualTo EventReviewDescription.APPOINTMENTS_CANCELLED
    }
  }

  @Test
  fun `stores an appointments changed event with action NO`() {
    mockPrisoner(prisonerNum = "ABC1234")
    val inboundEvent =
      appointmentsChangedEvent(prisonId = PENTONVILLE_PRISON_CODE, prisonerNumber = "ABC1234", action = "NO")

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verifyNoInteractions(allocationRepository)
    verify(eventReviewRepository).saveAndFlush(eventReviewCaptor.capture())

    with(eventReviewCaptor.firstValue) {
      bookingId isEqualTo 1
      eventData isEqualTo null
      eventTime isCloseTo TimeSource.now()
      eventType isEqualTo InboundEventType.APPOINTMENTS_CHANGED.eventType
      prisonCode isEqualTo PENTONVILLE_PRISON_CODE
      prisonerNumber isEqualTo "ABC1234"
      eventDescription isEqualTo EventReviewDescription.APPOINTMENTS_KEPT
    }
  }

  @Test
  fun `ignores prisoners in prisons which are not rolled out`() {
    mockPrisoner(prisonCode = "RSI")
    val inboundEvent = prisonerUpdatedEvent("123456")

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verifyNoInteractions(allocationRepository)
    verifyNoInteractions(eventReviewRepository)
  }

  @Test
  fun `ignores events for a prisoner with no active allocations`() {
    mockPrisoner()
    mockAllocations(PENTONVILLE_PRISON_CODE, "123456", emptyList())
    val inboundEvent = prisonerUpdatedEvent("123456", listOf("LOCATION", "SENTENCE"))

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(allocationRepository).findByPrisonCodePrisonerNumberPrisonerStatus(PENTONVILLE_PRISON_CODE, "123456", PrisonerStatus.ACTIVE, PrisonerStatus.PENDING)
    verifyNoInteractions(eventReviewRepository)
  }

  @Test
  fun `ignores prisoner update event that is not meaningful`() {
    val inboundEvent = prisonerUpdatedEvent("123456", listOf("SENTENCE", "STATUS"))

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(eventReviewRepository, never()).saveAndFlush(any())
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
