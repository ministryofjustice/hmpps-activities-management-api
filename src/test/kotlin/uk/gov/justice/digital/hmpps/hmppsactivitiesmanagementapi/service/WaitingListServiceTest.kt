package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import jakarta.persistence.EntityNotFoundException
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mockito.`when`
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.api.NonAssociationsApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.model.NonAssociation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.earliestReleaseDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.notInWorkCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.waitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.PrisonerDeclinedFromWaitingListEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.PrisonerRemovedFromWaitingListEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerWaitingListApplicationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.WaitingListApplicationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.WaitingListApplicationUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.WaitingListSearchRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.WaitingListRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.WaitingListSearchSpecification
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.NUMBER_OF_RESULTS_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISONER_NUMBER_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.CaseloadAccessException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.DEFAULT_CASELOAD_PENTONVILLE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.FakeCaseLoad
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.determineEarliestReleaseDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModel
import java.time.LocalDate
import java.util.*

@ExtendWith(FakeCaseLoad::class)
class WaitingListServiceTest {

  private val activity = activityEntity()
  private val schedule = activity.schedules().first()
  private val scheduleRepository: ActivityScheduleRepository =
    mock { on { findBy(schedule.activityScheduleId, DEFAULT_CASELOAD_PENTONVILLE) } doReturn schedule }
  private val waitingListRepository: WaitingListRepository = mock {}
  private val waitingListSearchSpecification: WaitingListSearchSpecification = spy()
  private val activityRepository: ActivityRepository = mock {}
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val auditService: AuditService = mock()
  private val nonAssociationsApiClient: NonAssociationsApiClient = mockk()
  private val declinedEventCaptor = argumentCaptor<PrisonerDeclinedFromWaitingListEvent>()
  private val removedEventCaptor = argumentCaptor<PrisonerRemovedFromWaitingListEvent>()
  private val service = WaitingListService(
    scheduleRepository,
    waitingListRepository,
    waitingListSearchSpecification,
    activityRepository,
    prisonerSearchApiClient,
    telemetryClient,
    auditService,
    nonAssociationsApiClient,
  )
  private val waitingListCaptor = argumentCaptor<WaitingList>()

  private val prisonCode = PENTONVILLE_PRISON_CODE
  private val prisonerNumber = "AB1234C"
  private val createdBy = "Creator"

  private fun createPrisonerWaitingListRequest(size: Int): List<PrisonerWaitingListApplicationRequest> = List(size) { index ->
    PrisonerWaitingListApplicationRequest(
      activityScheduleId = index + 1L,
      applicationDate = TimeSource.today(),
      requestedBy = "Requester",
      comments = "Testing",
      status = WaitingListStatus.PENDING,
    )
  }

  @BeforeEach
  fun setUp() {
    clearAllMocks()
  }

  @Test
  fun `add prisoner fails if does not have case load access`() {
    assertThatThrownBy {
      service.addPrisoner("WRONG_CASELOAD", mock(), "test")
    }.isInstanceOf(CaseloadAccessException::class.java)
  }

  @Test
  fun `fails if prisoner is at different prison to the activities prison`() {
    prisonerSearchApiClient.stub {
      on { findByPrisonerNumber(prisonerNumber = "123456") } doReturn activeInMoorlandPrisoner
    }

    val request = WaitingListApplicationRequest(
      prisonerNumber = "123456",
      activityScheduleId = schedule.activityScheduleId,
      applicationDate = TimeSource.today(),
      requestedBy = "Test",
      status = WaitingListStatus.PENDING,
    )

    assertThatThrownBy { service.addPrisoner(DEFAULT_CASELOAD_PENTONVILLE, request, "test") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Tim Harrison is not resident at this prison")
  }

  @Test
  fun `fails if status not PENDING, APPROVED or DECLINED`() {
    prisonerSearchApiClient.stub {
      on { findByPrisonerNumber(prisonerNumber = "123456") } doReturn activeOutPentonvillePrisoner
    }

    val request = WaitingListApplicationRequest(
      prisonerNumber = "123456",
      activityScheduleId = 1L,
      applicationDate = TimeSource.today(),
      requestedBy = "Bob",
      comments = "Bob's comments",
      status = WaitingListStatus.PENDING,
    )

    assertThatThrownBy {
      service.addPrisoner(
        DEFAULT_CASELOAD_PENTONVILLE,
        request.copy(status = WaitingListStatus.ALLOCATED),
        "test",
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Only statuses of PENDING, APPROVED and DECLINED are allowed when adding a prisoner to a waiting list")

    assertThatThrownBy {
      service.addPrisoner(
        DEFAULT_CASELOAD_PENTONVILLE,
        request.copy(status = WaitingListStatus.REMOVED),
        "test",
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Only statuses of PENDING, APPROVED and DECLINED are allowed when adding a prisoner to a waiting list")
  }

  @Test
  fun `fails if prisoner has no booking id`() {
    prisonerSearchApiClient.stub {
      on { findByPrisonerNumber(prisonerNumber = "123456") } doReturn activeInPentonvillePrisoner.copy(bookingId = null)
    }

    val request = WaitingListApplicationRequest(
      prisonerNumber = "123456",
      activityScheduleId = schedule.activityScheduleId,
      applicationDate = TimeSource.today(),
      requestedBy = "Test",
      status = WaitingListStatus.PENDING,
    )

    assertThatThrownBy { service.addPrisoner(DEFAULT_CASELOAD_PENTONVILLE, request, "test") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Prisoner 123456 has no booking id at prison $DEFAULT_CASELOAD_PENTONVILLE")
  }

  @Test
  fun `fails if activity category is 'not in work'`() {
    val request = WaitingListApplicationRequest(
      prisonerNumber = "123456",
      activityScheduleId = schedule.activityScheduleId,
      applicationDate = TimeSource.today(),
      requestedBy = "Test",
      status = WaitingListStatus.PENDING,
    )

    activity.activityCategory = notInWorkCategory

    assertThatThrownBy {
      service.addPrisoner(DEFAULT_CASELOAD_PENTONVILLE, request, "test")
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot add prisoner to the waiting list because the activity category is 'not in work'")
  }

  @Test
  fun `fails if activity has ended or ends today`() {
    activity.endDate = LocalDate.now()

    val request = WaitingListApplicationRequest(
      prisonerNumber = "123456",
      activityScheduleId = schedule.activityScheduleId,
      applicationDate = TimeSource.today(),
      requestedBy = "Test",
      status = WaitingListStatus.PENDING,
    )

    assertThatThrownBy { service.addPrisoner(DEFAULT_CASELOAD_PENTONVILLE, request, "test") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot add prisoner to the waiting list for an activity ending on or before today")
  }

  @Test
  fun `fails if application date is in the future`() {
    val request = WaitingListApplicationRequest(
      prisonerNumber = "123456",
      activityScheduleId = schedule.activityScheduleId,
      applicationDate = TimeSource.tomorrow(),
      requestedBy = "Test",
      status = WaitingListStatus.PENDING,
    )

    assertThatThrownBy { service.addPrisoner(DEFAULT_CASELOAD_PENTONVILLE, request, "test") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Application date cannot be not be in the future")
  }

  @Test
  fun `fails if pending application already exists for prisoner`() {
    val request = WaitingListApplicationRequest(
      prisonerNumber = "123456",
      activityScheduleId = schedule.activityScheduleId,
      applicationDate = TimeSource.today(),
      requestedBy = "Test",
      status = WaitingListStatus.PENDING,
    )

    mock<WaitingList> { on { status } doReturn WaitingListStatus.PENDING }.also { pending ->
      waitingListRepository.stub {
        on {
          findByPrisonCodeAndPrisonerNumberAndActivitySchedule(
            DEFAULT_CASELOAD_PENTONVILLE,
            "123456",
            schedule,
          )
        } doReturn listOf(pending)
      }
    }

    assertThatThrownBy { service.addPrisoner(DEFAULT_CASELOAD_PENTONVILLE, request, "test") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot add prisoner to the waiting list because a pending application already exists")
  }

  @Test
  fun `fails if approved application already exists for prisoner`() {
    val request = WaitingListApplicationRequest(
      prisonerNumber = "123456",
      activityScheduleId = schedule.activityScheduleId,
      applicationDate = TimeSource.today(),
      requestedBy = "Test",
      status = WaitingListStatus.PENDING,
    )

    mock<WaitingList> { on { status } doReturn WaitingListStatus.APPROVED }.also { approved ->
      waitingListRepository.stub {
        on {
          findByPrisonCodeAndPrisonerNumberAndActivitySchedule(
            DEFAULT_CASELOAD_PENTONVILLE,
            "123456",
            schedule,
          )
        } doReturn listOf(approved)
      }
    }

    assertThatThrownBy { service.addPrisoner(DEFAULT_CASELOAD_PENTONVILLE, request, "test") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot add prisoner to the waiting list because an approved application already exists")
  }

  @Test
  fun `fails if prisoner already allocated (in future)`() {
    val request = WaitingListApplicationRequest(
      prisonerNumber = "123456",
      activityScheduleId = schedule.activityScheduleId,
      applicationDate = TimeSource.today(),
      requestedBy = "Test",
      status = WaitingListStatus.PENDING,
    )

    schedule.allocatePrisoner(
      prisonerNumber = "123456".toPrisonerNumber(),
      payBand = mock(),
      bookingId = 1L,
      startDate = TimeSource.tomorrow(),
      allocatedBy = "Test",
    )

    waitingListRepository.stub {
      on {
        findByPrisonCodeAndPrisonerNumberAndActivitySchedule(
          DEFAULT_CASELOAD_PENTONVILLE,
          "123456",
          schedule,
        )
      } doReturn emptyList()
    }

    assertThatThrownBy { service.addPrisoner(DEFAULT_CASELOAD_PENTONVILLE, request, "test") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot add prisoner to the waiting list because they are already allocated")
  }

  @Test
  fun `add prisoner to waiting list succeeds`() {
    prisonerSearchApiClient.stub {
      on { findByPrisonerNumber(prisonerNumber = "123456") } doReturn activeInPentonvillePrisoner
    }

    val request = WaitingListApplicationRequest(
      prisonerNumber = "123456",
      activityScheduleId = 1L,
      applicationDate = TimeSource.today(),
      requestedBy = "Bob",
      comments = "Bob's comments",
      status = WaitingListStatus.PENDING,
    )

    service.addPrisoner(DEFAULT_CASELOAD_PENTONVILLE, request, "Test user")

    verify(waitingListRepository).saveAndFlush(waitingListCaptor.capture())
    verify(telemetryClient).trackEvent(
      TelemetryEvent.PRISONER_ADDED_TO_WAITLIST.value,
      mapOf(PRISONER_NUMBER_PROPERTY_KEY to "123456"),
      mapOf(NUMBER_OF_RESULTS_METRIC_KEY to 1.0),
    )

    with(waitingListCaptor.firstValue) {
      prisonerNumber isEqualTo "123456"
      activitySchedule isEqualTo schedule
      applicationDate isEqualTo TimeSource.today()
      requestedBy isEqualTo "Bob"
      comments isEqualTo "Bob's comments"
      status isEqualTo WaitingListStatus.PENDING
    }
  }

  @Test
  fun `can add multiple waiting list records for the same prisoner`() {
    prisonerSearchApiClient.stub {
      on { findByPrisonerNumber(prisonerNumber = "654321") } doReturn activeInPentonvillePrisoner
    }

    val request = WaitingListApplicationRequest(
      prisonerNumber = "654321",
      activityScheduleId = 1L,
      applicationDate = TimeSource.today(),
      requestedBy = "Bob",
      comments = "Bob's comments",
      status = WaitingListStatus.DECLINED,
    )

    service.addPrisoner(DEFAULT_CASELOAD_PENTONVILLE, request, "Test user")
    service.addPrisoner(
      DEFAULT_CASELOAD_PENTONVILLE,
      request.copy(
        applicationDate = TimeSource.yesterday(),
        requestedBy = "Fred",
        comments = "Fred's comments",
        status = WaitingListStatus.PENDING,
      ),
      "Test user",
    )

    verify(waitingListRepository, times(2)).saveAndFlush(waitingListCaptor.capture())

    with(waitingListCaptor.firstValue) {
      prisonerNumber isEqualTo "654321"
      activitySchedule isEqualTo schedule
      applicationDate isEqualTo TimeSource.today()
      requestedBy isEqualTo "Bob"
      comments isEqualTo "Bob's comments"
      status isEqualTo WaitingListStatus.DECLINED
    }

    with(waitingListCaptor.secondValue) {
      prisonerNumber isEqualTo "654321"
      activitySchedule isEqualTo schedule
      applicationDate isEqualTo TimeSource.yesterday()
      requestedBy isEqualTo "Fred"
      comments isEqualTo "Fred's comments"
      status isEqualTo WaitingListStatus.PENDING
    }
  }

  @Test
  fun `get waiting lists by the schedule identifier throws exception when no prisoner details`() {
    val schedule = activityEntity(prisonCode = PENTONVILLE_PRISON_CODE).schedules().first()
    val allocation = schedule.allocations().first()

    val waitingList = WaitingList(
      waitingListId = 99,
      prisonCode = PENTONVILLE_PRISON_CODE,
      activitySchedule = schedule,
      prisonerNumber = "123456",
      bookingId = 100L,
      applicationDate = TimeSource.today(),
      requestedBy = "Fred",
      comments = "Some random test comments",
      createdBy = "Bob",
      initialStatus = WaitingListStatus.DECLINED,
    ).apply {
      this.allocation = allocation
      this.updatedBy = "Test"
      this.updatedTime = TimeSource.now()
      this.declinedReason = "Needs to attend level one activity first"
    }

    scheduleRepository.stub {
      on { scheduleRepository.findById(1L) } doReturn Optional.of(schedule)
    }

    waitingListRepository.stub {
      on { findByActivitySchedule(schedule) } doReturn listOf(waitingList)
    }

    prisonerSearchApiClient.stub {
      on {
        runBlocking {
          prisonerSearchApiClient.findByPrisonerNumbersAsync(listOf("123456"))
        }
      } doReturn emptyList()
    }

    val nonAssociation: NonAssociation = mock {
      on { firstPrisonerNumber } doReturn "123456"
    }

    coEvery { nonAssociationsApiClient.getNonAssociationsInvolving(PENTONVILLE_PRISON_CODE, listOf("123456")) } returns listOf(nonAssociation)

    val exception = assertThrows<NullPointerException> {
      service.getWaitingListsBySchedule(1L)
    }
    exception.message isEqualTo "Prisoner 123456 not found for waiting list id 1"
  }

  @Test
  fun `get waiting lists by the schedule identifier ignores prisoners who have status of REMOVED`() {
    val schedule = activityEntity(prisonCode = PENTONVILLE_PRISON_CODE).schedules().first()

    val removedWaitingList = waitingList(prisonCode = PENTONVILLE_PRISON_CODE, prisonerNumber = "AAAAAAA", initialStatus = WaitingListStatus.REMOVED)
    val validWaitingList = waitingList(prisonCode = PENTONVILLE_PRISON_CODE, prisonerNumber = "CCCCCC", initialStatus = WaitingListStatus.PENDING)

    val validPrisoner = PrisonerSearchPrisonerFixture.instance(prisonerNumber = validWaitingList.prisonerNumber)

    scheduleRepository.stub {
      on { scheduleRepository.findById(1L) } doReturn Optional.of(schedule)
    }

    waitingListRepository.stub {
      on { findByActivitySchedule(schedule) } doReturn listOf(removedWaitingList, validWaitingList)
    }

    prisonerSearchApiClient.stub {
      on {
        runBlocking {
          prisonerSearchApiClient.findByPrisonerNumbersAsync(listOf("CCCCCC"))
        }
      } doReturn listOf(validPrisoner)
    }

    val nonAssociation: NonAssociation = mock {
      on { firstPrisonerNumber } doReturn "CCCCCC"
    }

    coEvery { nonAssociationsApiClient.getNonAssociationsInvolving(PENTONVILLE_PRISON_CODE, listOf("CCCCCC")) } returns listOf(nonAssociation)

    service.getWaitingListsBySchedule(1L, true) isEqualTo listOf(validWaitingList.toModel(determineEarliestReleaseDate(validPrisoner), true))

    coVerify { nonAssociationsApiClient.getNonAssociationsInvolving(PENTONVILLE_PRISON_CODE, listOf("CCCCCC")) }
  }

  @Test
  fun `get waiting lists by the schedule identifier with includeNonAssociationsCheck set to false does not call non-associations api`() {
    val schedule = activityEntity(prisonCode = PENTONVILLE_PRISON_CODE).schedules().first()

    val removedWaitingList = waitingList(prisonCode = PENTONVILLE_PRISON_CODE, prisonerNumber = "AAAAAAA", initialStatus = WaitingListStatus.REMOVED)
    val validWaitingList = waitingList(prisonCode = PENTONVILLE_PRISON_CODE, prisonerNumber = "CCCCCC", initialStatus = WaitingListStatus.PENDING)

    val validPrisoner = PrisonerSearchPrisonerFixture.instance(prisonerNumber = validWaitingList.prisonerNumber)

    scheduleRepository.stub {
      on { scheduleRepository.findById(1L) } doReturn Optional.of(schedule)
    }

    waitingListRepository.stub {
      on { findByActivitySchedule(schedule) } doReturn listOf(removedWaitingList, validWaitingList)
    }

    prisonerSearchApiClient.stub {
      on {
        runBlocking {
          prisonerSearchApiClient.findByPrisonerNumbersAsync(listOf("CCCCCC"))
        }
      } doReturn listOf(validPrisoner)
    }

    service.getWaitingListsBySchedule(1L, false) isEqualTo listOf(validWaitingList.toModel(determineEarliestReleaseDate(validPrisoner), null))

    coVerify(exactly = 0) { nonAssociationsApiClient.getNonAssociationsInvolving(any(), any()) }
  }

  @Test
  fun `get waiting lists by the schedule identifier succeeds when the waitlist is empty`() {
    val schedule = activityEntity(prisonCode = PENTONVILLE_PRISON_CODE).schedules().first()

    scheduleRepository.stub {
      on { scheduleRepository.findById(1L) } doReturn Optional.of(schedule)
    }

    waitingListRepository.stub {
      on { findByActivitySchedule(schedule) } doReturn emptyList()
    }

    prisonerSearchApiClient.stub {
      on { prisonerSearchApiClient.findByPrisonerNumbers(emptyList()) } doReturn emptyList()
    }

    assertThat(service.getWaitingListsBySchedule(1L)).isEmpty()

    coVerify(exactly = 0) { nonAssociationsApiClient.getNonAssociationsInvolving(any(), any()) }
  }

  @Test
  fun `get waiting lists by the schedule identifier succeeds when non-associations api returns a null`() {
    val schedule = activityEntity(prisonCode = PENTONVILLE_PRISON_CODE).schedules().first()
    val removedWaitingList = waitingList(prisonCode = PENTONVILLE_PRISON_CODE, prisonerNumber = "AAAAAAA", initialStatus = WaitingListStatus.REMOVED)
    val validWaitingList = waitingList(prisonCode = PENTONVILLE_PRISON_CODE, prisonerNumber = "CCCCCC", initialStatus = WaitingListStatus.PENDING)

    val validPrisoner = PrisonerSearchPrisonerFixture.instance(prisonerNumber = validWaitingList.prisonerNumber)

    scheduleRepository.stub {
      on { scheduleRepository.findById(1L) } doReturn Optional.of(schedule)
    }

    waitingListRepository.stub {
      on { findByActivitySchedule(schedule) } doReturn listOf(removedWaitingList, validWaitingList)
    }

    prisonerSearchApiClient.stub {
      on {
        runBlocking {
          prisonerSearchApiClient.findByPrisonerNumbersAsync(listOf("CCCCCC"))
        }
      } doReturn listOf(validPrisoner)
    }

    coEvery { nonAssociationsApiClient.getNonAssociationsInvolving(PENTONVILLE_PRISON_CODE, listOf("CCCCCC")) } returns null

    service.getWaitingListsBySchedule(1L, true) isEqualTo listOf(validWaitingList.toModel(determineEarliestReleaseDate(validPrisoner), null))

    coVerify { nonAssociationsApiClient.getNonAssociationsInvolving(PENTONVILLE_PRISON_CODE, listOf("CCCCCC")) }
  }

  @Test
  fun `get waiting lists by the schedule identifier with earliest release date`() {
    val schedule = activityEntity(prisonCode = PENTONVILLE_PRISON_CODE).schedules().first()
    val allocation = schedule.allocations().first()

    val waitingList = WaitingList(
      waitingListId = 99,
      prisonCode = PENTONVILLE_PRISON_CODE,
      activitySchedule = schedule,
      prisonerNumber = "G4793VF",
      bookingId = 100L,
      applicationDate = TimeSource.today(),
      requestedBy = "Fred",
      comments = "Some random test comments",
      createdBy = "Bob",
      initialStatus = WaitingListStatus.DECLINED,
    ).apply {
      this.allocation = allocation
      this.updatedBy = "Test"
      this.updatedTime = TimeSource.now()
      this.declinedReason = "Needs to attend level one activity first"
    }

    scheduleRepository.stub {
      on { scheduleRepository.findById(1L) } doReturn Optional.of(schedule)
    }

    waitingListRepository.stub {
      on { findByActivitySchedule(schedule) } doReturn listOf(waitingList)
    }

    val releaseDate = LocalDate.of(2030, 4, 20)
    val prisoner = PrisonerSearchPrisonerFixture.instance().copy(releaseDate = releaseDate)

    prisonerSearchApiClient.stub {
      on {
        runBlocking {
          prisonerSearchApiClient.findByPrisonerNumbersAsync(listOf("G4793VF"))
        }
      } doReturn listOf(prisoner)
    }

    val nonAssociation: NonAssociation = mock {
      on { firstPrisonerNumber } doReturn "G4793VF"
    }

    coEvery { nonAssociationsApiClient.getNonAssociationsInvolving(PENTONVILLE_PRISON_CODE, listOf("G4793VF")) } returns listOf(nonAssociation)

    val earliestReleaseDate = earliestReleaseDate().copy(releaseDate = releaseDate)

    with(service.getWaitingListsBySchedule(1L)) {
      assertThat(size).isEqualTo(1)

      with(first()) {
        id isEqualTo 99
        scheduleId isEqualTo schedule.activityScheduleId
        allocationId isEqualTo allocation.allocationId
        prisonCode isEqualTo PENTONVILLE_PRISON_CODE
        prisonerNumber isEqualTo "G4793VF"
        bookingId isEqualTo 100L
        status isEqualTo WaitingListStatus.DECLINED
        requestedDate isEqualTo TimeSource.today()
        requestedBy isEqualTo "Fred"
        comments isEqualTo "Some random test comments"
        createdBy isEqualTo "Bob"
        creationTime isCloseTo TimeSource.now()
        updatedBy isEqualTo "Test"
        updatedTime!! isCloseTo TimeSource.now()
        declinedReason isEqualTo "Needs to attend level one activity first"
        earliestReleaseDate.releaseDate isEqualTo earliestReleaseDate.releaseDate
        nonAssociations isEqualTo true
      }
    }

    coVerify { nonAssociationsApiClient.getNonAssociationsInvolving(PENTONVILLE_PRISON_CODE, listOf("G4793VF")) }
  }

  @Test
  fun `get waiting lists by the schedule identifier fails when not found`() {
    whenever(scheduleRepository.findById(any())) doReturn Optional.empty()

    assertThatThrownBy { service.getWaitingListsBySchedule(99) }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Activity Schedule 99 not found")
  }

  @Test
  fun `get waiting list by id`() {
    val waitingList = waitingList(waitingListId = 99, prisonCode = PENTONVILLE_PRISON_CODE)

    val releaseDate = LocalDate.of(2030, 4, 20)
    val prisoner = PrisonerSearchPrisonerFixture.instance().copy(releaseDate = releaseDate)

    prisonerSearchApiClient.stub {
      on { findByPrisonerNumber("123456") } doReturn prisoner
    }

    whenever(waitingListRepository.findById(waitingList.waitingListId)) doReturn Optional.of(waitingList)

    service.getWaitingListBy(99) isEqualTo waitingList.toModel(determineEarliestReleaseDate(prisoner))
  }

  @Test
  fun `get waiting list by id fails if does not have case load access`() {
    whenever(waitingListRepository.findById(99)) doReturn Optional.of(
      waitingList(
        waitingListId = 99,
        prisonCode = "WRONG_CASELOAD",
      ),
    )

    assertThatThrownBy { service.getWaitingListBy(99) }.isInstanceOf(CaseloadAccessException::class.java)
  }

  @Test
  fun `get waiting list by id fails if not found`() {
    whenever(waitingListRepository.findById(any())) doReturn Optional.empty()

    assertThatThrownBy { service.getWaitingListBy(99) }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Waiting List 99 not found")
  }

  @Test
  fun `update the application date`() {
    val waitingList = waitingList(prisonCode = PENTONVILLE_PRISON_CODE, applicationDate = TimeSource.today()).also {
      whenever(waitingListRepository.findById(it.waitingListId)) doReturn Optional.of(it)
    }

    val releaseDate = LocalDate.of(2030, 4, 20)
    val prisoner = PrisonerSearchPrisonerFixture.instance().copy(releaseDate = releaseDate)

    prisonerSearchApiClient.stub {
      on { findByPrisonerNumber("123456") } doReturn prisoner
    }

    waitingList.applicationDate isEqualTo TimeSource.today()
    waitingList.updatedTime isEqualTo null
    waitingList.updatedBy isEqualTo null

    service.updateWaitingList(
      waitingList.waitingListId,
      WaitingListApplicationUpdateRequest(applicationDate = TimeSource.yesterday()),
      "Jon",
    )

    waitingList.applicationDate isEqualTo TimeSource.yesterday()
    waitingList.updatedTime!! isCloseTo TimeSource.now()
    waitingList.updatedBy!! isEqualTo "Jon"
  }

  @Test
  fun `update the application date no-op if the same`() {
    val waitingList = waitingList(prisonCode = PENTONVILLE_PRISON_CODE, applicationDate = TimeSource.today()).also {
      whenever(waitingListRepository.findById(it.waitingListId)) doReturn Optional.of(it)
    }

    val prisoner = PrisonerSearchPrisonerFixture.instance().copy(releaseDate = LocalDate.now())
    prisonerSearchApiClient.stub {
      on { findByPrisonerNumber("123456") } doReturn prisoner
    }

    waitingList.applicationDate isEqualTo TimeSource.today()
    waitingList.updatedTime isEqualTo null
    waitingList.updatedBy isEqualTo null

    service.updateWaitingList(
      waitingList.waitingListId,
      WaitingListApplicationUpdateRequest(applicationDate = TimeSource.today()),
      "Jon",
    )

    waitingList.applicationDate isEqualTo TimeSource.today()
    waitingList.updatedTime isEqualTo null
    waitingList.updatedBy isEqualTo null
  }

  @Test
  fun `update the application date fails if after creation date`() {
    val waitingList = waitingList(prisonCode = PENTONVILLE_PRISON_CODE).also {
      whenever(waitingListRepository.findById(it.waitingListId)) doReturn Optional.of(it)
    }

    waitingList.applicationDate isEqualTo TimeSource.today()

    assertThatThrownBy {
      service.updateWaitingList(
        waitingList.waitingListId,
        WaitingListApplicationUpdateRequest(applicationDate = TimeSource.tomorrow()),
        "Fred",
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The application date cannot be after the date the application was initially created ${TimeSource.today()}")
  }

  @Test
  fun `update requested by`() {
    val waitingList = waitingList(prisonCode = PENTONVILLE_PRISON_CODE, requestedBy = "Fred").also {
      whenever(waitingListRepository.findById(it.waitingListId)) doReturn Optional.of(it)
    }

    val prisoner = PrisonerSearchPrisonerFixture.instance().copy(releaseDate = LocalDate.now())
    prisonerSearchApiClient.stub {
      on { findByPrisonerNumber("123456") } doReturn prisoner
    }

    waitingList.requestedBy isEqualTo "Fred"
    waitingList.updatedTime isEqualTo null
    waitingList.updatedBy isEqualTo null

    service.updateWaitingList(
      waitingList.waitingListId,
      WaitingListApplicationUpdateRequest(requestedBy = "Bob"),
      "Fred",
    )

    waitingList.requestedBy isEqualTo "Bob"
    waitingList.updatedTime!! isCloseTo TimeSource.now()
    waitingList.updatedBy isEqualTo "Fred"
  }

  @Test
  fun `update requested by no-op if the same`() {
    val waitingList = waitingList(prisonCode = PENTONVILLE_PRISON_CODE, requestedBy = "Fred").also {
      whenever(waitingListRepository.findById(it.waitingListId)) doReturn Optional.of(it)
    }

    val prisoner = PrisonerSearchPrisonerFixture.instance().copy(releaseDate = LocalDate.now())
    prisonerSearchApiClient.stub {
      on { findByPrisonerNumber("123456") } doReturn prisoner
    }

    waitingList.requestedBy isEqualTo "Fred"
    waitingList.updatedTime isEqualTo null
    waitingList.updatedBy isEqualTo null

    service.updateWaitingList(
      waitingList.waitingListId,
      WaitingListApplicationUpdateRequest(requestedBy = "Fred"),
      "Fred",
    )

    waitingList.requestedBy isEqualTo "Fred"
    waitingList.updatedTime isEqualTo null
    waitingList.updatedBy isEqualTo null
  }

  @Test
  fun `update requested fails if blank or empty`() {
    val waitingList = waitingList(prisonCode = PENTONVILLE_PRISON_CODE, requestedBy = "Fred").also {
      whenever(waitingListRepository.findById(it.waitingListId)) doReturn Optional.of(it)
    }

    waitingList.requestedBy isEqualTo "Fred"

    assertThatThrownBy {
      service.updateWaitingList(
        waitingList.waitingListId,
        WaitingListApplicationUpdateRequest(requestedBy = " "),
        "Fred",
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Requested by cannot be blank or empty")

    assertThatThrownBy {
      service.updateWaitingList(
        waitingList.waitingListId,
        WaitingListApplicationUpdateRequest(requestedBy = ""),
        "Fred",
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Requested by cannot be blank or empty")
  }

  @Test
  fun `update comments`() {
    val waitingList = waitingList(prisonCode = PENTONVILLE_PRISON_CODE, comments = "Initial comments").also {
      whenever(waitingListRepository.findById(it.waitingListId)) doReturn Optional.of(it)
    }

    val prisoner = PrisonerSearchPrisonerFixture.instance().copy(releaseDate = LocalDate.now())
    prisonerSearchApiClient.stub {
      on { findByPrisonerNumber("123456") } doReturn prisoner
    }

    waitingList.comments isEqualTo "Initial comments"
    waitingList.updatedTime isEqualTo null
    waitingList.updatedBy isEqualTo null

    service.updateWaitingList(
      waitingList.waitingListId,
      WaitingListApplicationUpdateRequest(comments = "Updated comments"),
      "Lucy",
    )

    waitingList.comments isEqualTo "Updated comments"
    waitingList.updatedTime!! isCloseTo TimeSource.now()
    waitingList.updatedBy isEqualTo "Lucy"
  }

  @Test
  fun `update comments no-op if the same`() {
    val waitingList = waitingList(prisonCode = PENTONVILLE_PRISON_CODE, comments = "Initial comments").also {
      whenever(waitingListRepository.findById(it.waitingListId)) doReturn Optional.of(it)
    }

    val prisoner = PrisonerSearchPrisonerFixture.instance().copy(releaseDate = LocalDate.now())
    prisonerSearchApiClient.stub {
      on { findByPrisonerNumber("123456") } doReturn prisoner
    }

    waitingList.comments isEqualTo "Initial comments"
    waitingList.updatedTime isEqualTo null
    waitingList.updatedBy isEqualTo null

    service.updateWaitingList(
      waitingList.waitingListId,
      WaitingListApplicationUpdateRequest(comments = "Initial comments"),
      "Lucy",
    )

    waitingList.comments isEqualTo "Initial comments"
    waitingList.updatedTime isEqualTo null
    waitingList.updatedBy isEqualTo null
  }

  @Test
  fun `update status to APPROVED`() {
    val waitingList = waitingList(prisonCode = PENTONVILLE_PRISON_CODE, initialStatus = WaitingListStatus.PENDING).also {
      whenever(waitingListRepository.findById(it.waitingListId)) doReturn Optional.of(it)
    }

    val prisoner = PrisonerSearchPrisonerFixture.instance().copy(releaseDate = LocalDate.now())
    prisonerSearchApiClient.stub {
      on { findByPrisonerNumber("123456") } doReturn prisoner
    }

    waitingList.status isEqualTo WaitingListStatus.PENDING
    waitingList.updatedTime isEqualTo null
    waitingList.updatedBy isEqualTo null
    waitingList.statusUpdatedTime isEqualTo null

    service.updateWaitingList(
      waitingList.waitingListId,
      WaitingListApplicationUpdateRequest(status = WaitingListStatus.APPROVED),
      "Frank",
    )

    verify(telemetryClient).trackEvent(
      TelemetryEvent.PRISONER_APPROVED_ON_WAITLIST.value,
      mapOf(PRISONER_NUMBER_PROPERTY_KEY to "123456"),
      mapOf(NUMBER_OF_RESULTS_METRIC_KEY to 1.0),
    )

    waitingList.status isEqualTo WaitingListStatus.APPROVED
    waitingList.updatedTime!! isCloseTo TimeSource.now()
    waitingList.updatedBy isEqualTo "Frank"
    waitingList.statusUpdatedTime isCloseTo TimeSource.now()
  }

  @Test
  fun `update status to DECLINED`() {
    val waitingList = waitingList(prisonCode = PENTONVILLE_PRISON_CODE, initialStatus = WaitingListStatus.PENDING).also {
      whenever(waitingListRepository.findById(it.waitingListId)) doReturn Optional.of(it)
    }

    val prisoner = PrisonerSearchPrisonerFixture.instance().copy(releaseDate = LocalDate.now())
    prisonerSearchApiClient.stub {
      on { findByPrisonerNumber("123456") } doReturn prisoner
    }

    waitingList.status isEqualTo WaitingListStatus.PENDING
    waitingList.updatedTime isEqualTo null
    waitingList.updatedBy isEqualTo null
    waitingList.statusUpdatedTime isEqualTo null

    service.updateWaitingList(
      waitingList.waitingListId,
      WaitingListApplicationUpdateRequest(status = WaitingListStatus.DECLINED),
      "Frank",
    )

    verify(telemetryClient).trackEvent(
      TelemetryEvent.PRISONER_DECLINED_FROM_WAITLIST.value,
      mapOf(PRISONER_NUMBER_PROPERTY_KEY to "123456"),
      mapOf(NUMBER_OF_RESULTS_METRIC_KEY to 1.0),
    )

    waitingList.status isEqualTo WaitingListStatus.DECLINED
    waitingList.updatedTime!! isCloseTo TimeSource.now()
    waitingList.updatedBy isEqualTo "Frank"
    waitingList.statusUpdatedTime isCloseTo TimeSource.now()
  }

  @Test
  fun `update status to WITHDRAWN`() {
    val waitingList = waitingList(prisonCode = PENTONVILLE_PRISON_CODE, initialStatus = WaitingListStatus.PENDING).also {
      whenever(waitingListRepository.findById(it.waitingListId)) doReturn Optional.of(it)
    }

    val prisoner = PrisonerSearchPrisonerFixture.instance().copy(releaseDate = LocalDate.now())
    prisonerSearchApiClient.stub {
      on { findByPrisonerNumber("123456") } doReturn prisoner
    }

    waitingList.status isEqualTo WaitingListStatus.PENDING
    waitingList.updatedTime isEqualTo null
    waitingList.updatedBy isEqualTo null
    waitingList.statusUpdatedTime isEqualTo null

    service.updateWaitingList(
      waitingList.waitingListId,
      WaitingListApplicationUpdateRequest(status = WaitingListStatus.WITHDRAWN),
      "Frank",
    )

    verify(telemetryClient).trackEvent(
      TelemetryEvent.PRISONER_WITHDRAWN_FROM_WAITLIST.value,
      mapOf(PRISONER_NUMBER_PROPERTY_KEY to "123456"),
      mapOf(NUMBER_OF_RESULTS_METRIC_KEY to 1.0),
    )

    waitingList.status isEqualTo WaitingListStatus.WITHDRAWN
    waitingList.updatedTime!! isCloseTo TimeSource.now()
    waitingList.updatedBy isEqualTo "Frank"
    waitingList.statusUpdatedTime isCloseTo TimeSource.now()
  }

  @Test
  fun `Re-instate a withdrawn application`() {
    val waitingList = waitingList(prisonCode = PENTONVILLE_PRISON_CODE, initialStatus = WaitingListStatus.WITHDRAWN).also {
      whenever(waitingListRepository.findById(it.waitingListId)) doReturn Optional.of(it)
    }

    val prisoner = PrisonerSearchPrisonerFixture.instance().copy(releaseDate = LocalDate.now())
    prisonerSearchApiClient.stub {
      on { findByPrisonerNumber("123456") } doReturn prisoner
    }

    waitingList.status isEqualTo WaitingListStatus.WITHDRAWN
    waitingList.updatedTime isEqualTo null
    waitingList.updatedBy isEqualTo null
    waitingList.statusUpdatedTime isEqualTo null

    service.updateWaitingList(
      waitingList.waitingListId,
      WaitingListApplicationUpdateRequest(status = WaitingListStatus.PENDING),
      "Frank",
    )

    verifyNoInteractions(telemetryClient)

    waitingList.status isEqualTo WaitingListStatus.PENDING
    waitingList.updatedTime!! isCloseTo TimeSource.now()
    waitingList.updatedBy isEqualTo "Frank"
    waitingList.statusUpdatedTime isCloseTo TimeSource.now()
  }

  @Test
  fun `update status no-op if the same`() {
    val waitingList = waitingList(prisonCode = PENTONVILLE_PRISON_CODE, initialStatus = WaitingListStatus.PENDING).also {
      whenever(waitingListRepository.findById(it.waitingListId)) doReturn Optional.of(it)
    }

    val prisoner = PrisonerSearchPrisonerFixture.instance().copy(releaseDate = LocalDate.now())
    prisonerSearchApiClient.stub {
      on { findByPrisonerNumber("123456") } doReturn prisoner
    }

    waitingList.status isEqualTo WaitingListStatus.PENDING
    waitingList.updatedTime isEqualTo null
    waitingList.updatedBy isEqualTo null
    waitingList.statusUpdatedTime isEqualTo null

    service.updateWaitingList(
      waitingList.waitingListId,
      WaitingListApplicationUpdateRequest(status = WaitingListStatus.PENDING),
      "Frank",
    )

    waitingList.status isEqualTo WaitingListStatus.PENDING
    waitingList.updatedTime isEqualTo null
    waitingList.updatedBy isEqualTo null
    waitingList.statusUpdatedTime isEqualTo null
  }

  @Test
  fun `update status fails if already allocated`() {
    val waitingList =
      waitingList(prisonCode = PENTONVILLE_PRISON_CODE, initialStatus = WaitingListStatus.ALLOCATED).also {
        whenever(waitingListRepository.findById(it.waitingListId)) doReturn Optional.of(it)
      }

    assertThatThrownBy {
      service.updateWaitingList(
        waitingList.waitingListId,
        WaitingListApplicationUpdateRequest(status = WaitingListStatus.DECLINED),
        "Frank",
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The waiting list ${waitingList.waitingListId} can no longer be updated")

    waitingList.status isEqualTo WaitingListStatus.ALLOCATED
    waitingList.updatedTime isEqualTo null
    waitingList.updatedBy isEqualTo null
    waitingList.statusUpdatedTime isEqualTo null
  }

  @EnumSource(WaitingListStatus::class, names = ["PENDING", "WITHDRAWN"], mode = EnumSource.Mode.EXCLUDE)
  @ParameterizedTest(name = "WITHDRAWN cannot be changed from to {0}")
  fun `withdrawn can only be changed to pending`(newStatus: WaitingListStatus) {
    val waitingList =
      waitingList(prisonCode = PENTONVILLE_PRISON_CODE, initialStatus = WaitingListStatus.WITHDRAWN).also {
        whenever(waitingListRepository.findById(it.waitingListId)) doReturn Optional.of(it)
      }

    assertThatThrownBy {
      service.updateWaitingList(
        waitingList.waitingListId,
        WaitingListApplicationUpdateRequest(status = newStatus),
        "Frank",
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The withdrawn waiting list can only be changed to pending")

    waitingList.status isEqualTo WaitingListStatus.WITHDRAWN
    waitingList.updatedTime isEqualTo null
    waitingList.updatedBy isEqualTo null
    waitingList.statusUpdatedTime isEqualTo null
  }

  @Test
  fun `update status fails if already removed`() {
    val waitingList = waitingList(prisonCode = PENTONVILLE_PRISON_CODE, initialStatus = WaitingListStatus.REMOVED).also {
      whenever(waitingListRepository.findById(it.waitingListId)) doReturn Optional.of(it)
    }

    assertThatThrownBy {
      service.updateWaitingList(
        waitingList.waitingListId,
        WaitingListApplicationUpdateRequest(status = WaitingListStatus.APPROVED),
        "Frank",
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The waiting list ${waitingList.waitingListId} can no longer be updated")

    waitingList.status isEqualTo WaitingListStatus.REMOVED
    waitingList.updatedTime isEqualTo null
    waitingList.updatedBy isEqualTo null
    waitingList.statusUpdatedTime isEqualTo null
  }

  @Test
  fun `only status changes update 'statusUpdatedTime'`() {
    val waitingList = waitingList(
      prisonCode = PENTONVILLE_PRISON_CODE,
      initialStatus = WaitingListStatus.PENDING,
    ).also {
      whenever(waitingListRepository.findById(it.waitingListId)) doReturn Optional.of(it)
    }

    val prisoner = PrisonerSearchPrisonerFixture.instance().copy(releaseDate = LocalDate.now())
    prisonerSearchApiClient.stub {
      on { findByPrisonerNumber("123456") } doReturn prisoner
    }

    service.updateWaitingList(
      waitingList.waitingListId,
      WaitingListApplicationUpdateRequest(
        applicationDate = LocalDate.now().minusDays(1),
        comments = "test",
        requestedBy = "test",
      ),
      "Frank",
    )

    waitingList.statusUpdatedTime isEqualTo null

    service.updateWaitingList(
      waitingList.waitingListId,
      WaitingListApplicationUpdateRequest(status = WaitingListStatus.APPROVED),
      "Frank",
    )

    waitingList.statusUpdatedTime isCloseTo TimeSource.now()
  }

  @Test
  fun `update application fails if person is already allocated to the activity`() {
    val waitingList =
      waitingList(prisonCode = PENTONVILLE_PRISON_CODE, initialStatus = WaitingListStatus.PENDING, allocated = true)
        .also {
          whenever(waitingListRepository.findById(it.waitingListId)) doReturn Optional.of(it)
        }

    assertThatThrownBy {
      service.updateWaitingList(
        waitingList.waitingListId,
        WaitingListApplicationUpdateRequest(status = WaitingListStatus.APPROVED),
        "Frank",
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The waiting list ${waitingList.waitingListId} can no longer be updated because the prisoner has already been allocated to the activity")

    waitingList.status isEqualTo WaitingListStatus.PENDING
    waitingList.updatedTime isEqualTo null
    waitingList.updatedBy isEqualTo null
  }

  @Test
  fun `update application fails if person has a more recent application for the same activity`() {
    val waitingList = waitingList(prisonCode = PENTONVILLE_PRISON_CODE, initialStatus = WaitingListStatus.PENDING)
      .also {
        whenever(waitingListRepository.findById(it.waitingListId)) doReturn Optional.of(it)

        val moreRecentApplication =
          waitingList(waitingListId = 2, prisonCode = PENTONVILLE_PRISON_CODE, initialStatus = WaitingListStatus.PENDING)
        whenever(
          waitingListRepository.findByPrisonCodeAndPrisonerNumberAndActivitySchedule(
            it.prisonCode,
            it.prisonerNumber,
            it.activitySchedule,
          ),
        ) doReturn listOf(it.copy(), moreRecentApplication)
      }

    assertThatThrownBy {
      service.updateWaitingList(
        waitingList.waitingListId,
        WaitingListApplicationUpdateRequest(status = WaitingListStatus.APPROVED),
        "Frank",
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The waiting list ${waitingList.waitingListId} can no longer be updated because there is a more recent application for this prisoner")

    waitingList.status isEqualTo WaitingListStatus.PENDING
    waitingList.updatedTime isEqualTo null
    waitingList.updatedBy isEqualTo null
  }

  @Test
  fun `update status fails if not PENDING, APPROVED or DECLINED`() {
    val waitingList = waitingList(prisonCode = PENTONVILLE_PRISON_CODE, initialStatus = WaitingListStatus.PENDING).also {
      whenever(waitingListRepository.findById(it.waitingListId)) doReturn Optional.of(it)
    }

    assertThatThrownBy {
      service.updateWaitingList(
        waitingList.waitingListId,
        WaitingListApplicationUpdateRequest(status = WaitingListStatus.ALLOCATED),
        "Frank",
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Only PENDING, APPROVED, DECLINED or WITHDRAWN are allowed for the status change")

    waitingList.status isEqualTo WaitingListStatus.PENDING
    waitingList.updatedTime isEqualTo null
    waitingList.updatedBy isEqualTo null
  }

  @Test
  fun `update fails if waiting list not found`() {
    whenever(waitingListRepository.findById(any())) doReturn Optional.empty()

    assertThatThrownBy {
      service.updateWaitingList(99, WaitingListApplicationUpdateRequest(status = WaitingListStatus.ALLOCATED), "Frank")
    }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Waiting List 99 not found")
  }

  @Test
  fun `update fails if incorrect caseload`() {
    val waitingList = waitingList(prisonCode = MOORLAND_PRISON_CODE, initialStatus = WaitingListStatus.PENDING).also {
      whenever(waitingListRepository.findById(it.waitingListId)) doReturn Optional.of(it)
    }

    assertThatThrownBy {
      service.updateWaitingList(
        waitingList.waitingListId,
        WaitingListApplicationUpdateRequest(status = WaitingListStatus.ALLOCATED),
        "Frank",
      )
    }
      .isInstanceOf(CaseloadAccessException::class.java)
  }

  @Test
  fun `removes pending, approved and declined applications for prisoner and is audited`() {
    val pending = waitingList(
      waitingListId = 1,
      prisonCode = MOORLAND_PRISON_CODE,
      prisonerNumber = "A",
      initialStatus = WaitingListStatus.PENDING,
    )
    val approved = waitingList(
      waitingListId = 2,
      prisonCode = MOORLAND_PRISON_CODE,
      prisonerNumber = "A",
      initialStatus = WaitingListStatus.APPROVED,
    )
    val declined = waitingList(
      waitingListId = 3,
      prisonCode = MOORLAND_PRISON_CODE,
      prisonerNumber = "A",
      initialStatus = WaitingListStatus.DECLINED,
    )

    whenever(
      waitingListRepository.findByPrisonCodeAndPrisonerNumberAndStatusIn(
        prisonCode = MOORLAND_PRISON_CODE,
        prisonerNumber = "A",
        statuses = setOf(WaitingListStatus.PENDING, WaitingListStatus.APPROVED, WaitingListStatus.DECLINED, WaitingListStatus.WITHDRAWN),
      ),
    ) doReturn listOf(pending, approved, declined)

    service.removeOpenApplications(MOORLAND_PRISON_CODE, "A", "Fred")

    verify(waitingListRepository).findByPrisonCodeAndPrisonerNumberAndStatusIn(
      MOORLAND_PRISON_CODE,
      "A",
      setOf(WaitingListStatus.PENDING, WaitingListStatus.APPROVED, WaitingListStatus.DECLINED, WaitingListStatus.WITHDRAWN),
    )

    with(pending) {
      status isEqualTo WaitingListStatus.REMOVED
      updatedTime!! isCloseTo TimeSource.now()
      updatedBy isEqualTo "Fred"
    }

    with(approved) {
      status isEqualTo WaitingListStatus.REMOVED
      updatedTime!! isCloseTo TimeSource.now()
      updatedBy isEqualTo "Fred"
    }

    with(declined) {
      status isEqualTo WaitingListStatus.REMOVED
      updatedTime!! isCloseTo TimeSource.now()
      updatedBy isEqualTo "Fred"
    }

    verify(auditService, times(3)).logEvent(removedEventCaptor.capture())

    with(removedEventCaptor.firstValue) {
      waitingListId isEqualTo 1
      activityId isEqualTo 1
      scheduleId isEqualTo 1
      activityName isEqualTo schedule.activity.summary
      prisonCode isEqualTo MOORLAND_PRISON_CODE
      prisonerNumber isEqualTo "A"
      createdAt isCloseTo TimeSource.now()
    }

    with(removedEventCaptor.secondValue) {
      waitingListId isEqualTo 2
      activityId isEqualTo 2
      scheduleId isEqualTo 2
      activityName isEqualTo schedule.activity.summary
      prisonCode isEqualTo MOORLAND_PRISON_CODE
      prisonerNumber isEqualTo "A"
      createdAt isCloseTo TimeSource.now()
    }

    with(removedEventCaptor.thirdValue) {
      waitingListId isEqualTo 3
      activityId isEqualTo 3
      scheduleId isEqualTo 3
      activityName isEqualTo schedule.activity.summary
      prisonCode isEqualTo MOORLAND_PRISON_CODE
      prisonerNumber isEqualTo "A"
      createdAt isCloseTo TimeSource.now()
    }
  }

  @Test
  fun `declines pending and approved applications for activity and is audited`() {
    val activity = activityEntity()
    val pendingA = waitingList(
      waitingListId = 1,
      prisonCode = MOORLAND_PRISON_CODE,
      prisonerNumber = "A",
      initialStatus = WaitingListStatus.PENDING,
    )
    val approvedA = waitingList(
      waitingListId = 2,
      prisonCode = MOORLAND_PRISON_CODE,
      prisonerNumber = "A",
      initialStatus = WaitingListStatus.APPROVED,
    )
    val pendingB = waitingList(
      waitingListId = 3,
      prisonCode = MOORLAND_PRISON_CODE,
      prisonerNumber = "B",
      initialStatus = WaitingListStatus.PENDING,
    )
    val approvedB = waitingList(
      waitingListId = 4,
      prisonCode = MOORLAND_PRISON_CODE,
      prisonerNumber = "B",
      initialStatus = WaitingListStatus.APPROVED,
    )

    whenever(activityRepository.findById(activity.activityId)) doReturn Optional.of(activity)

    whenever(
      waitingListRepository.findByActivityAndStatusIn(
        activity,
        setOf(WaitingListStatus.PENDING, WaitingListStatus.APPROVED),
      ),
    ) doReturn listOf(pendingA, approvedA, pendingB, approvedB)

    service.declinePendingOrApprovedApplications(activity.activityId, "reason", "Bob")

    verify(waitingListRepository).findByActivityAndStatusIn(
      activity,
      setOf(WaitingListStatus.PENDING, WaitingListStatus.APPROVED),
    )

    listOf(pendingA, approvedA, pendingB, approvedB).forEach {
      with(it) {
        status isEqualTo WaitingListStatus.DECLINED
        declinedReason isEqualTo "reason"
        updatedTime!! isCloseTo TimeSource.now()
        updatedBy isEqualTo "Bob"
      }
    }

    verify(auditService, times(4)).logEvent(declinedEventCaptor.capture())

    with(declinedEventCaptor.firstValue) {
      waitingListId isEqualTo 1
      activityId isEqualTo 1
      scheduleId isEqualTo 1
      activityName isEqualTo schedule.activity.summary
      prisonCode isEqualTo MOORLAND_PRISON_CODE
      prisonerNumber isEqualTo "A"
      createdAt isCloseTo TimeSource.now()
    }

    with(declinedEventCaptor.secondValue) {
      waitingListId isEqualTo 2
      activityId isEqualTo 2
      scheduleId isEqualTo 2
      activityName isEqualTo schedule.activity.summary
      prisonCode isEqualTo MOORLAND_PRISON_CODE
      prisonerNumber isEqualTo "A"
      createdAt isCloseTo TimeSource.now()
    }

    with(declinedEventCaptor.thirdValue) {
      waitingListId isEqualTo 3
      activityId isEqualTo 3
      scheduleId isEqualTo 3
      activityName isEqualTo schedule.activity.summary
      prisonCode isEqualTo MOORLAND_PRISON_CODE
      prisonerNumber isEqualTo "B"
      createdAt isCloseTo TimeSource.now()
    }

    with(declinedEventCaptor.lastValue) {
      waitingListId isEqualTo 4
      activityId isEqualTo 4
      scheduleId isEqualTo 4
      activityName isEqualTo schedule.activity.summary
      prisonCode isEqualTo MOORLAND_PRISON_CODE
      prisonerNumber isEqualTo "B"
      createdAt isCloseTo TimeSource.now()
    }
  }

  @Test
  fun `Searching waiting list applications fail if user does not have case load access`() {
    assertThatThrownBy {
      service.searchWaitingLists("WRONG_CASELOAD", mock(), 0, 50)
    }.isInstanceOf(CaseloadAccessException::class.java)
  }

  @Test
  fun `Returns waiting list applications`() {
    val request = WaitingListSearchRequest()
    val waitingListApplication = waitingList()
    val pagedResult = PageImpl(listOf(waitingListApplication))

    val pageable: Pageable = PageRequest.of(0, 50, Sort.by("applicationDate"))
    whenever(waitingListRepository.findAll(any(), eq(pageable))).thenReturn(pagedResult)

    val releaseDate = LocalDate.of(2030, 4, 20)
    val prisoner = PrisonerSearchPrisonerFixture.instance().copy(releaseDate = releaseDate)
    val earliestReleaseDate = earliestReleaseDate().copy(releaseDate = releaseDate)

    prisonerSearchApiClient.stub {
      on { findByPrisonerNumber("123456") } doReturn prisoner
    }

    val result = service.searchWaitingLists(DEFAULT_CASELOAD_PENTONVILLE, request, 0, 50)

    result isEqualTo pagedResult.map { it.toModel(earliestReleaseDate) }
  }

  @Test
  fun `Filters waiting list applications`() {
    val request = WaitingListSearchRequest(
      applicationDateFrom = LocalDate.parse("2023-01-01"),
      applicationDateTo = LocalDate.parse("2023-01-31"),
      activityId = 2,
      prisonerNumbers = listOf("ABC1234"),
      status = listOf(WaitingListStatus.APPROVED),
    )
    val waitingListApplication = waitingList()
    val pagedResult = PageImpl(listOf(waitingListApplication))

    val pageable: Pageable = PageRequest.of(0, 50, Sort.by("applicationDate"))
    whenever(waitingListRepository.findAll(any(), eq(pageable))).thenReturn(pagedResult)

    val releaseDate = LocalDate.of(2030, 4, 20)
    val prisoner = PrisonerSearchPrisonerFixture.instance().copy(releaseDate = releaseDate)
    val earliestReleaseDate = earliestReleaseDate().copy(releaseDate = releaseDate)

    prisonerSearchApiClient.stub {
      on { findByPrisonerNumber("123456") } doReturn prisoner
    }

    val result = service.searchWaitingLists(DEFAULT_CASELOAD_PENTONVILLE, request, 0, 50)

    verify(waitingListSearchSpecification).prisonCodeEquals(DEFAULT_CASELOAD_PENTONVILLE)
    verify(waitingListSearchSpecification).applicationDateFrom(LocalDate.parse("2023-01-01"))
    verify(waitingListSearchSpecification).applicationDateTo(LocalDate.parse("2023-01-31"))
    verify(waitingListSearchSpecification).prisonerNumberIn(listOf("ABC1234"))
    verify(waitingListSearchSpecification).statusIn(listOf(WaitingListStatus.APPROVED))

    result isEqualTo pagedResult.map { it.toModel(earliestReleaseDate) }
  }

  @Test
  fun `fetch open applications for the prison`() {
    val waitingList = listOf(waitingList(prisonCode = MOORLAND_PRISON_CODE, prisonerNumber = "ABC1234"))

    whenever(
      waitingListRepository.findByPrisonCodeAndStatusIn(
        MOORLAND_PRISON_CODE,
        setOf(WaitingListStatus.PENDING, WaitingListStatus.APPROVED, WaitingListStatus.DECLINED, WaitingListStatus.WITHDRAWN),
      ),
    ) doReturn waitingList

    val result = service.fetchOpenApplicationsForPrison(MOORLAND_PRISON_CODE)

    assertThat(result).isEqualTo(waitingList)
  }

  @Test
  fun `should throw exception if application request is empty`() {
    assertThrows<IllegalArgumentException> {
      service.addPrisonerToMultipleActivities(prisonCode, prisonerNumber, emptyList(), createdBy)
    }.also {
      assertThat(it.message).isEqualTo("At least one waiting list application request must be provided")
    }
  }

  @Test
  fun `should throw exception if more than 5 application requests are provided`() {
    val requestList = createPrisonerWaitingListRequest(6)
    assertThrows<IllegalArgumentException> {
      service.addPrisonerToMultipleActivities(prisonCode, prisonerNumber, requestList, createdBy)
    }.also {
      assertThat(it.message).isEqualTo("A maximum of 5 waiting list application requests can be submitted at once")
    }
  }

  @Test
  fun `should throw exception if activity schedule is not found`() {
    val requestList = createPrisonerWaitingListRequest(3)
    `when`(scheduleRepository.findBy(1L, prisonCode)).thenReturn(null)
    assertThrows<EntityNotFoundException> {
      service.addPrisonerToMultipleActivities(prisonCode, prisonerNumber, requestList, createdBy)
    }.also {
      assertThat(it.message).isEqualTo("Activity schedule 1 not found")
    }
  }

  @Test
  fun `should save applications successfully to waiting list`() {
    prisonerSearchApiClient.stub {
      on { findByPrisonerNumber(prisonerNumber = "AB1234C") } doReturn activeInPentonvillePrisoner
    }

    val requestList = createPrisonerWaitingListRequest(5)

    whenever(scheduleRepository.findBy(any(), any())).thenReturn(schedule)
    whenever(waitingListRepository.saveAllAndFlush(any<List<WaitingList>>()))
      .thenAnswer { invocation: InvocationOnMock ->
        invocation.getArgument<List<WaitingList>>(0)
      }

    assertDoesNotThrow {
      service.addPrisonerToMultipleActivities(prisonCode, prisonerNumber, requestList, createdBy)
    }

    // Verifying the number of applications saved
    val captor = argumentCaptor<List<WaitingList>>()
    verify(waitingListRepository).saveAllAndFlush(captor.capture())

    val applicationsSaved = captor.firstValue
    assert(applicationsSaved.size == 5) {
      "Expected 5 applications in waiting list, got ${applicationsSaved.size}"
    }

    // Checking field values for every application in the waiting list
    applicationsSaved.forEachIndexed { index, app ->
      assert(app.prisonCode == prisonCode) { "Prison code does not match for application $index" }
      assert(app.activitySchedule == schedule) { "Activity schedule does not match for application $index" }
      assert(app.status == WaitingListStatus.PENDING) { "Status does not match for application $index" }
      assert(app.createdBy == "Creator") { "Created by does not match for application $index" }
      assert(app.comments == "Testing") { "Comment does not match for application $index" }
      assert(app.applicationDate == TimeSource.today()) { "Application date does not match for application $index" }
      assert(app.requestedBy == "Requester") { "Requested by does not match for application $index" }
    }

    verify(telemetryClient, times(5)).trackEvent(
      TelemetryEvent.PRISONER_ADDED_TO_WAITLIST.value,
      mapOf(PRISONER_NUMBER_PROPERTY_KEY to "AB1234C"),
      mapOf(NUMBER_OF_RESULTS_METRIC_KEY to 1.0),
    )
  }
}
