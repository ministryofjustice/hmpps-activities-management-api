package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.InmateDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.trackEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activeAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.pentonvillePrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonPayBandsLowMediumHigh
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.schedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.waitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.PrisonerAllocatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerAllocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerDeallocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.EarliestReleaseDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonPayBandRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.WaitingListRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ACTIVITY_ID_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ALLOCATION_START_DATE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISONER_NUMBER_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISON_CODE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.USER_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.FakeCaseLoad
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.FakeSecurityContext
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelAllocations
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Optional

@ExtendWith(FakeSecurityContext::class, FakeCaseLoad::class)
class ActivityScheduleServiceTest {

  private val repository: ActivityScheduleRepository = mock()
  private val prisonApiClient: PrisonApiClient = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val prisonPayBandRepository: PrisonPayBandRepository = mock()
  private val waitingListRepository: WaitingListRepository = mock()
  private val auditService: AuditService = mock()
  private val auditCaptor = argumentCaptor<PrisonerAllocatedEvent>()
  private val telemetryClient: TelemetryClient = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val service =
    ActivityScheduleService(
      repository,
      prisonApiClient,
      prisonerSearchApiClient,
      prisonPayBandRepository,
      waitingListRepository,
      auditService,
      telemetryClient,
      TransactionHandler(),
      outboundEventsService,
    )

  private val caseLoad = pentonvillePrisonCode

  private val prisoner = InmateDetail(
    agencyId = caseLoad,
    offenderNo = "123456",
    inOutStatus = "IN",
    firstName = "Bob",
    lastName = "Bobson",
    activeFlag = true,
    offenderId = 1L,
    rootOffenderId = 1L,
    status = "IN",
    dateOfBirth = LocalDate.of(2001, 10, 1),
    bookingId = 1,
  )

  @Test
  fun `current allocations for a given schedule are returned for current date`() {
    val schedule = schedule(pentonvillePrisonCode).apply {
      allocations().first().startDate = LocalDate.now()
    }

    whenever(repository.getActivityScheduleByIdWithFilters(1)).thenReturn(schedule)

    val allocations = service.getAllocationsBy(1)

    assertThat(allocations).hasSize(1)
    assertThat(allocations).containsExactlyInAnyOrder(
      *schedule.allocations().toModelAllocations().toTypedArray(),
    )
  }

  @Test
  fun `ended allocations for a given schedule are not returned`() {
    val schedule = schedule(pentonvillePrisonCode).apply {
      allocations().first().apply { deallocateNowWithReason(DeallocationReason.ENDED) }
    }

    whenever(repository.getActivityScheduleByIdWithFilters(1)).thenReturn(schedule)

    assertThat(service.getAllocationsBy(1)).isEmpty()
  }

  @Test
  fun `getAllocationsBy - prisoner information is returned`() {
    val schedule = schedule(pentonvillePrisonCode)
    val prisoner: Prisoner = mock {
      on { firstName } doReturn "JOE"
      on { lastName } doReturn "BLOGGS"
      on { cellLocation } doReturn "MDI-1-1-001"
      on { releaseDate } doReturn LocalDate.now()
      on { prisonerNumber } doReturn "A1234AA"
    }

    whenever(repository.getActivityScheduleByIdWithFilters(1)).thenReturn(schedule)
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(listOf("A1234AA"))).thenReturn(Mono.just(listOf(prisoner)))

    val expectedResponse = schedule.allocations().toModelAllocations().apply {
      map {
        it.prisonerName = "JOE BLOGGS"
        it.cellLocation = "MDI-1-1-001"
        it.earliestReleaseDate = EarliestReleaseDate(LocalDate.now())
      }
    }

    assertThat(service.getAllocationsBy(1, activeOnly = false, includePrisonerSummary = true))
      .isEqualTo(expectedResponse)
  }

  @Test
  fun `can get allocations for given date`() {
    val schedule = schedule(pentonvillePrisonCode)

    whenever(
      repository.getActivityScheduleByIdWithFilters(
        1,
        allocationsActiveOnDate = LocalDate.now(),
      ),
    )
      .thenReturn(schedule)

    assertThat(service.getAllocationsBy(1, activeOn = LocalDate.now())).isEqualTo(
      schedule.allocations().toModelAllocations(),
    )
  }

  @Test
  fun `all current, future and ended allocations for a given schedule are returned`() {
    val active = activeAllocation.copy(allocationId = 1)
    val suspended =
      activeAllocation.copy(allocationId = 1).apply { prisonerStatus = PrisonerStatus.SUSPENDED }
    val ended =
      active.copy(allocationId = 2, startDate = active.startDate.minusDays(2))
        .apply { endDate = LocalDate.now().minusDays(1) }
    val future = active.copy(allocationId = 3, startDate = active.startDate.plusDays(1))
    val schedule = mock<ActivitySchedule>()
    val activity = mock<Activity>()

    whenever(schedule.activity).thenReturn(activity)
    whenever(activity.prisonCode).thenReturn(caseLoad)
    whenever(schedule.allocations()).thenReturn(listOf(active, suspended, ended, future))
    whenever(repository.getActivityScheduleByIdWithFilters(1)).thenReturn(schedule)

    val allocations = service.getAllocationsBy(1, false)
    assertThat(allocations).hasSize(4)
    assertThat(allocations).containsExactlyInAnyOrder(
      *listOf(active, suspended, ended, future).toModelAllocations().toTypedArray(),
    )
  }

  @Test
  fun `throws entity not found exception for unknown activity schedule`() {
    assertThatThrownBy { service.getAllocationsBy(-99) }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Activity Schedule -99 not found")
  }

  @Test
  fun `can deallocate a prisoner from activity schedule`() {
    val schedule = mock<ActivitySchedule>().stub {
      on { deallocatePrisonerOn("1", TimeSource.tomorrow(), DeallocationReason.OTHER, "by test") } doReturn allocation().copy(prisonerNumber = "1")
    }

    whenever(repository.findById(schedule.activityScheduleId)).doReturn(Optional.of(schedule))

    service.deallocatePrisoners(
      schedule.activityScheduleId,
      PrisonerDeallocationRequest(
        listOf("1"),
        DeallocationReason.OTHER.name,
        TimeSource.tomorrow(),
      ),
      "by test",
    )

    verify(schedule).deallocatePrisonerOn(
      "1",
      TimeSource.tomorrow(),
      DeallocationReason.OTHER,
      "by test",
    )
    verify(repository).saveAndFlush(schedule)
    verify(telemetryClient).trackEvent(
      TelemetryEvent.PRISONER_DEALLOCATED.value,
      mapOf(PRISONER_NUMBER_PROPERTY_KEY to "1"),
    )
  }

  @Test
  fun `can deallocate multiple prisoners from activity schedule`() {
    val schedule = mock<ActivitySchedule>().stub {
      on { deallocatePrisonerOn("1", TimeSource.tomorrow(), DeallocationReason.OTHER, "by test") } doReturn allocation()
      on { deallocatePrisonerOn("2", TimeSource.tomorrow(), DeallocationReason.OTHER, "by test") } doReturn allocation()
    }

    whenever(repository.findById(schedule.activityScheduleId)).doReturn(Optional.of(schedule))

    service.deallocatePrisoners(
      schedule.activityScheduleId,
      PrisonerDeallocationRequest(
        listOf("1", "2"),
        DeallocationReason.OTHER.name,
        TimeSource.tomorrow(),
      ),
      "by test",
    )

    verify(schedule).deallocatePrisonerOn(
      "1",
      TimeSource.tomorrow(),
      DeallocationReason.OTHER,
      "by test",
    )
    verify(schedule).deallocatePrisonerOn(
      "2",
      TimeSource.tomorrow(),
      DeallocationReason.OTHER,
      "by test",
    )
    verify(repository).saveAndFlush(schedule)
  }

  @Test
  fun `throws entity not found exception for unknown activity schedule when try and deallocate`() {
    val schedule = activitySchedule(activityEntity())
    val allocation = schedule.allocations().first()

    whenever(repository.findById(schedule.activityScheduleId)).doReturn(Optional.empty())

    assertThatThrownBy {
      service.deallocatePrisoners(
        schedule.activityScheduleId,
        PrisonerDeallocationRequest(
          listOf(allocation.prisonerNumber),
          DeallocationReason.RELEASED.name,
          TimeSource.tomorrow(),
        ),
        "by test",
      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Activity Schedule ${schedule.activityScheduleId} not found")
  }

  @Test
  fun `throws exception for invalid reason codes on attempted deallocation`() {
    val schedule = mock<ActivitySchedule>()

    whenever(repository.findById(schedule.activityScheduleId)).doReturn(Optional.of(schedule))

    DeallocationReason.entries.filter { !it.displayed }.map { it.name }.forEach { reasonCode ->
      assertThatThrownBy {
        service.deallocatePrisoners(
          schedule.activityScheduleId,
          PrisonerDeallocationRequest(
            listOf("123456"),
            reasonCode,
            TimeSource.tomorrow(),
          ),
          "by test",
        )
      }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Invalid deallocation reason specified '$reasonCode'")
    }
  }

  @Test
  fun `allocate throws exception for start date before activity start date`() {
    val schedule = activitySchedule(activityEntity(prisonCode = pentonvillePrisonCode))
    schedule.activity.startDate = LocalDate.now().plusDays(2)

    whenever(repository.findById(schedule.activityScheduleId)).doReturn(Optional.of(schedule))
    whenever(prisonPayBandRepository.findByPrisonCode(caseLoad)).thenReturn(
      prisonPayBandsLowMediumHigh(caseLoad),
    )
    whenever(prisonApiClient.getPrisonerDetails("123456", fullInfo = false)).doReturn(
      Mono.just(
        prisoner,
      ),
    )

    assertThatThrownBy {
      service.allocatePrisoner(
        schedule.activityScheduleId,
        PrisonerAllocationRequest(
          "123456",
          1,
          TimeSource.tomorrow(),
        ),
        "by test",
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Allocation start date cannot be before activity start date")
  }

  @Test
  fun `allocate throws exception for end date after activity end date`() {
    val schedule = activitySchedule(activityEntity(prisonCode = pentonvillePrisonCode))
    schedule.activity.endDate = TimeSource.tomorrow()

    whenever(repository.findById(schedule.activityScheduleId)).doReturn(Optional.of(schedule))
    whenever(prisonPayBandRepository.findByPrisonCode(caseLoad)).thenReturn(
      prisonPayBandsLowMediumHigh(caseLoad),
    )
    whenever(prisonApiClient.getPrisonerDetails("123456", fullInfo = false)).doReturn(
      Mono.just(
        prisoner,
      ),
    )

    assertThatThrownBy {
      service.allocatePrisoner(
        schedule.activityScheduleId,
        PrisonerAllocationRequest(
          "123456",
          1,
          TimeSource.tomorrow(),
          TimeSource.tomorrow().plusDays(1),
        ),
        "by test",
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Allocation end date cannot be after activity end date")
  }

  @Test
  fun `allocate throws exception for end date before activity start date`() {
    val schedule = activitySchedule(activityEntity(prisonCode = pentonvillePrisonCode))

    whenever(repository.findById(schedule.activityScheduleId)).doReturn(Optional.of(schedule))
    whenever(prisonPayBandRepository.findByPrisonCode(caseLoad)).thenReturn(
      prisonPayBandsLowMediumHigh(caseLoad),
    )
    whenever(prisonApiClient.getPrisonerDetails("123456", fullInfo = false)).doReturn(
      Mono.just(
        prisoner,
      ),
    )

    assertThatThrownBy {
      service.allocatePrisoner(
        schedule.activityScheduleId,
        PrisonerAllocationRequest(
          "123456",
          1,
          TimeSource.tomorrow(),
          TimeSource.today(),
        ),
        "by test",
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Allocation end date cannot be before allocation start date")
  }

  @Test
  fun `allocate throws exception for start date not in future`() {
    val schedule = activitySchedule(activityEntity())

    whenever(repository.findById(schedule.activityScheduleId)).doReturn(Optional.of(schedule))
    whenever(prisonPayBandRepository.findByPrisonCode(caseLoad)).thenReturn(
      prisonPayBandsLowMediumHigh(caseLoad),
    )
    whenever(prisonApiClient.getPrisonerDetails("123456", fullInfo = false)).doReturn(
      Mono.just(
        prisoner,
      ),
    )

    assertThatThrownBy {
      service.allocatePrisoner(
        schedule.activityScheduleId,
        PrisonerAllocationRequest(
          "123456",
          1,
          TimeSource.today(),
        ),
        "by test",
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Allocation start date must be in the future")
  }

  @Test
  fun `successful allocation is audited`() {
    val schedule = activitySchedule(
      activity = activityEntity(activityId = 100, prisonCode = pentonvillePrisonCode),
      activityScheduleId = 200,
      noAllocations = true,
    )
    schedule.allocations() hasSize 0

    whenever(repository.findById(schedule.activityScheduleId)).doReturn(Optional.of(schedule))
    whenever(prisonPayBandRepository.findByPrisonCode(caseLoad)).thenReturn(
      prisonPayBandsLowMediumHigh(caseLoad),
    )
    whenever(prisonApiClient.getPrisonerDetails("654321", fullInfo = false)).doReturn(
      Mono.just(
        prisoner,
      ),
    )
    whenever(prisonApiClient.getPrisonerDetails("654321", fullInfo = false)).doReturn(
      Mono.just(
        prisoner,
      ),
    )
    whenever(repository.saveAndFlush(any())).doReturn(schedule)
    whenever(
      waitingListRepository.findByPrisonCodeAndPrisonerNumberAndActivitySchedule(
        caseLoad,
        "654321",
        schedule,
      ),
    ).thenReturn(emptyList())

    service.allocatePrisoner(
      schedule.activityScheduleId,
      PrisonerAllocationRequest(
        "654321",
        1,
        TimeSource.tomorrow(),
      ),
      "by test",
    )

    with(schedule.allocations().single()) {
      prisonerNumber isEqualTo "654321"
    }

    verify(auditService).logEvent(auditCaptor.capture())

    with(auditCaptor.firstValue) {
      activityId isEqualTo 100
      activityName isEqualTo schedule.activity.summary
      prisonCode isEqualTo pentonvillePrisonCode
      prisonerNumber isEqualTo "654321"
      scheduleId isEqualTo 200
      scheduleDescription isEqualTo schedule.description
      waitingListId isEqualTo null
      createdAt isCloseTo LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
    }

    verify(telemetryClient).trackEvent(
      TelemetryEvent.CREATE_ALLOCATION.value,
      mapOf(
        USER_PROPERTY_KEY to "by test",
        PRISON_CODE_PROPERTY_KEY to "PVI",
        PRISONER_NUMBER_PROPERTY_KEY to "654321",
        ACTIVITY_ID_PROPERTY_KEY to "200",
        ALLOCATION_START_DATE_PROPERTY_KEY to TimeSource.tomorrow().toString(),
      ),
      emptyMap(),
    )
  }

  @Test
  fun `allocation updates APPROVED waiting list application to ALLOCATED status when present and is audited`() {
    val schedule = activitySchedule(
      activity = activityEntity(activityId = 100, prisonCode = pentonvillePrisonCode),
      activityScheduleId = 200,
      noAllocations = true,
    )
    schedule.allocations() hasSize 0

    val waitingListEntity = waitingList(
      prisonCode = schedule.activity.prisonCode,
      initialStatus = WaitingListStatus.APPROVED,
      waitingListId = 300,
    )

    whenever(repository.findById(schedule.activityScheduleId)).doReturn(Optional.of(schedule))
    whenever(prisonPayBandRepository.findByPrisonCode(caseLoad)).thenReturn(
      prisonPayBandsLowMediumHigh(caseLoad),
    )
    whenever(prisonApiClient.getPrisonerDetails("123456", fullInfo = false)).doReturn(
      Mono.just(
        prisoner,
      ),
    )
    whenever(prisonApiClient.getPrisonerDetails("123456", fullInfo = false)).doReturn(
      Mono.just(
        prisoner,
      ),
    )
    whenever(repository.saveAndFlush(any())).doReturn(schedule)
    whenever(
      waitingListRepository.findByPrisonCodeAndPrisonerNumberAndActivitySchedule(
        caseLoad,
        "123456",
        schedule,
      ),
    ).thenReturn(listOf(waitingListEntity))

    service.allocatePrisoner(
      schedule.activityScheduleId,
      PrisonerAllocationRequest(
        "123456",
        1,
        TimeSource.tomorrow(),
      ),
      "by test",
    )

    with(schedule.allocations().single()) {
      prisonerNumber isEqualTo "123456"
    }

    with(waitingListEntity) {
      assertThat(status).isEqualTo(WaitingListStatus.ALLOCATED)
      assertThat(allocation?.allocatedBy).isEqualTo("by test")
      assertThat(allocation?.prisonerNumber).isEqualTo("123456")
    }

    verify(auditService).logEvent(auditCaptor.capture())

    with(auditCaptor.firstValue) {
      activityId isEqualTo 100
      activityName isEqualTo schedule.activity.summary
      prisonCode isEqualTo pentonvillePrisonCode
      prisonerNumber isEqualTo "123456"
      scheduleId isEqualTo 200
      scheduleDescription isEqualTo schedule.description
      waitingListId isEqualTo 300
      createdAt isCloseTo LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
    }
  }

  @Test
  fun `allocation fails if more than one approved waiting list`() {
    val schedule = activitySchedule(activity = activityEntity(prisonCode = pentonvillePrisonCode))
    val waitingLists = listOf(
      waitingList(
        prisonCode = schedule.activity.prisonCode,
        initialStatus = WaitingListStatus.APPROVED,
        waitingListId = 1,
      ),
      waitingList(
        prisonCode = schedule.activity.prisonCode,
        initialStatus = WaitingListStatus.APPROVED,
        waitingListId = 2,
      ),
    )

    whenever(repository.findById(schedule.activityScheduleId)).doReturn(Optional.of(schedule))
    whenever(prisonPayBandRepository.findByPrisonCode(caseLoad)).thenReturn(
      prisonPayBandsLowMediumHigh(caseLoad),
    )
    whenever(prisonApiClient.getPrisonerDetails("123456", fullInfo = false)).doReturn(
      Mono.just(
        prisoner,
      ),
    )
    whenever(prisonApiClient.getPrisonerDetails("123456", fullInfo = false)).doReturn(
      Mono.just(
        prisoner,
      ),
    )
    whenever(repository.saveAndFlush(any())).doReturn(schedule)
    whenever(
      waitingListRepository.findByPrisonCodeAndPrisonerNumberAndActivitySchedule(
        caseLoad,
        "123456",
        schedule,
      ),
    ).thenReturn(waitingLists)

    assertThatThrownBy {
      service.allocatePrisoner(
        schedule.activityScheduleId,
        PrisonerAllocationRequest(
          "123456",
          1,
          TimeSource.tomorrow(),
        ),
        "by test",
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Prisoner has more than one APPROVED waiting list. A prisoner can only have one approved waiting list")
  }
}
