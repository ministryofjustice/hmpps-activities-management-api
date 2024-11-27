package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityNotFoundException
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.ArgumentMatchers.eq
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNoteSubType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNoteType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNotesApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.model.CaseNote
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.api.NonAssociationsApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.model.NonAssociation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.trackEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activeAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.pentonvilleActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonPayBandsLowMediumHigh
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.schedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.waitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.PrisonerAllocatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AddCaseNoteRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerAllocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerDeallocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.EarliestReleaseDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.WaitingListRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.PrisonPayBandRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ACTIVITY_ID_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ALLOCATION_START_DATE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISONER_NUMBER_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISON_CODE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.USER_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.CaseloadAccessException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.FakeCaseLoad
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.FakeSecurityContext
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelAllocations
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelSchedule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Optional

@ExtendWith(FakeSecurityContext::class, FakeCaseLoad::class)
class ActivityScheduleServiceTest {

  private val repository: ActivityScheduleRepository = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val prisonerSearchAdminApiClient: PrisonerSearchApiApplicationClient = mock()
  private val caseNotesApiClient: CaseNotesApiClient = mock()
  private val prisonPayBandRepository: PrisonPayBandRepository = mock()
  private val waitingListRepository: WaitingListRepository = mock()
  private val auditService: AuditService = mock()
  private val auditCaptor = argumentCaptor<PrisonerAllocatedEvent>()
  private val telemetryClient: TelemetryClient = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val manageAttendancesService: ManageAttendancesService = mock()
  private val nonAssociationsApiClient: NonAssociationsApiClient = mock()
  private val service =
    ActivityScheduleService(
      repository,
      prisonerSearchApiClient,
      prisonerSearchAdminApiClient,
      caseNotesApiClient,
      prisonPayBandRepository,
      waitingListRepository,
      auditService,
      telemetryClient,
      TransactionHandler(),
      outboundEventsService,
      manageAttendancesService,
      nonAssociationsApiClient,
    )

  private val caseLoad = PENTONVILLE_PRISON_CODE

  private val prisoner = activeInPentonvillePrisoner

  @Test
  fun `current allocations for a given schedule are returned for current date`() {
    val schedule = schedule(PENTONVILLE_PRISON_CODE).apply {
      allocations().first().startDate = LocalDate.now()
    }

    whenever(repository.getActivityScheduleByIdWithFilters(1)).thenReturn(schedule)

    val allocations = service.getAllocationsBy(1)

    assertThat(allocations).hasSize(2)
    assertThat(allocations).containsExactlyInAnyOrder(*schedule.allocations().toModelAllocations().toTypedArray())
  }

  @Test
  fun `ended allocations for a given schedule are not returned`() {
    val schedule = schedule(PENTONVILLE_PRISON_CODE).apply {
      allocations().forEach { it.apply { deallocateNowWithReason(DeallocationReason.ENDED) } }
    }

    whenever(repository.getActivityScheduleByIdWithFilters(1)) doReturn schedule

    assertThat(service.getAllocationsBy(1)).isEmpty()
  }

  @CsvSource("true", "false")
  @ParameterizedTest
  fun `getAllocationsBy - prisoner information is returned`(hasNonAssociation: Boolean) {
    val schedule = schedule(PENTONVILLE_PRISON_CODE)
    val prisoner1: Prisoner = mock {
      on { firstName } doReturn "JOE"
      on { lastName } doReturn "BLOGGS"
      on { cellLocation } doReturn "MDI-1-1-001"
      on { releaseDate } doReturn LocalDate.now()
      on { prisonerNumber } doReturn "A1234AA"
      on { prisonId } doReturn "MDI"
      on { status } doReturn "ACTIVE IN"
    }
    val prisoner2: Prisoner = mock {
      on { firstName } doReturn "JOE"
      on { lastName } doReturn "BLOGGS"
      on { cellLocation } doReturn "MDI-1-1-001"
      on { releaseDate } doReturn LocalDate.now()
      on { prisonerNumber } doReturn "A1111BB"
      on { prisonId } doReturn "MDI"
      on { status } doReturn "ACTIVE IN"
    }

    whenever(repository.getActivityScheduleByIdWithFilters(1)) doReturn schedule

    val nonAssociation1: NonAssociation = mock {
      on { firstPrisonerNumber } doReturn (if (hasNonAssociation) "A1234AA" else "UNKNOWN")
    }

    val nonAssociation2: NonAssociation = mock {
      on { secondPrisonerNumber } doReturn (if (hasNonAssociation) "A1111BB" else "UNKNOWN")
    }

    prisonerSearchApiClient.stub {
      on {
        runBlocking {
          prisonerSearchApiClient.findByPrisonerNumbersAsync(listOf("A1234AA", "A1111BB"))
        }
      } doReturn listOf(prisoner1, prisoner2)
    }

    nonAssociationsApiClient.stub {
      on {
        runBlocking {
          nonAssociationsApiClient.getNonAssociationsInvolving("PVI", listOf("A1234AA", "A1111BB"))
        }
      } doReturn listOf(nonAssociation1, nonAssociation2)
    }

    val expectedResponse = schedule.allocations().toModelAllocations().apply {
      map {
        it.prisonerName = "JOE BLOGGS"
        it.prisonerFirstName = "JOE"
        it.prisonerLastName = "BLOGGS"
        it.cellLocation = "MDI-1-1-001"
        it.earliestReleaseDate = EarliestReleaseDate(LocalDate.now())
        it.prisonerStatus = "ACTIVE IN"
        it.prisonerPrisonCode = "MDI"
        it.nonAssociations = hasNonAssociation
      }
    }

    assertThat(service.getAllocationsBy(1, activeOnly = false, includePrisonerSummary = true))
      .isEqualTo(expectedResponse)
  }

  @Test
  fun `getAllocationsBy - empty non-associations is handles correctly`() {
    val schedule = schedule(PENTONVILLE_PRISON_CODE)
    val prisoner1: Prisoner = mock {
      on { firstName } doReturn "JOE"
      on { lastName } doReturn "BLOGGS"
      on { cellLocation } doReturn "MDI-1-1-001"
      on { releaseDate } doReturn LocalDate.now()
      on { prisonerNumber } doReturn "A1234AA"
      on { prisonId } doReturn "MDI"
      on { status } doReturn "ACTIVE IN"
    }
    val prisoner2: Prisoner = mock {
      on { firstName } doReturn "JOE"
      on { lastName } doReturn "BLOGGS"
      on { cellLocation } doReturn "MDI-1-1-001"
      on { releaseDate } doReturn LocalDate.now()
      on { prisonerNumber } doReturn "A1111BB"
      on { prisonId } doReturn "MDI"
      on { status } doReturn "ACTIVE IN"
    }

    whenever(repository.getActivityScheduleByIdWithFilters(1)) doReturn schedule

    prisonerSearchApiClient.stub {
      on {
        runBlocking {
          prisonerSearchApiClient.findByPrisonerNumbersAsync(listOf("A1234AA", "A1111BB"))
        }
      } doReturn listOf(prisoner1, prisoner2)
    }

    nonAssociationsApiClient.stub {
      on {
        runBlocking {
          nonAssociationsApiClient.getNonAssociationsInvolving("PVI", listOf("A1234AA", "A1111BB"))
        }
      } doReturn emptyList()
    }

    val expectedResponse = schedule.allocations().toModelAllocations().apply {
      map {
        it.prisonerName = "JOE BLOGGS"
        it.prisonerFirstName = "JOE"
        it.prisonerLastName = "BLOGGS"
        it.cellLocation = "MDI-1-1-001"
        it.earliestReleaseDate = EarliestReleaseDate(LocalDate.now())
        it.prisonerStatus = "ACTIVE IN"
        it.prisonerPrisonCode = "MDI"
        it.nonAssociations = false
      }
    }

    assertThat(service.getAllocationsBy(1, activeOnly = false, includePrisonerSummary = true))
      .isEqualTo(expectedResponse)
  }

  @Test
  fun `getAllocationsBy - no prisoner information throws a null pointer exception`() {
    val schedule = schedule(PENTONVILLE_PRISON_CODE)

    whenever(repository.getActivityScheduleByIdWithFilters(1)) doReturn schedule

    prisonerSearchApiClient.stub {
      on {
        runBlocking {
          prisonerSearchApiClient.findByPrisonerNumbersAsync(listOf("A1234AA", "A1111BB"))
        }
      } doReturn emptyList()
    }

    val exception = assertThrows<NullPointerException> {
      service.getAllocationsBy(1, activeOnly = false, includePrisonerSummary = true)
    }
    exception.message isEqualTo "Prisoner A1234AA not found for allocation id 0"
  }

  @Test
  fun `can get allocations for given date`() {
    val schedule = schedule(PENTONVILLE_PRISON_CODE)

    whenever(
      repository.getActivityScheduleByIdWithFilters(
        1,
        allocationsActiveOnDate = LocalDate.now(),
      ),
    ) doReturn schedule

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

    whenever(schedule.activity) doReturn activity
    whenever(activity.prisonCode) doReturn caseLoad
    whenever(schedule.allocations()) doReturn listOf(active, suspended, ended, future)
    whenever(repository.getActivityScheduleByIdWithFilters(1)) doReturn schedule

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
    val allocation = allocation().copy(prisonerNumber = "1")

    val schedule = mock<ActivitySchedule>().stub {
      on { deallocatePrisonerOn("1", TimeSource.tomorrow(), DeallocationReason.OTHER, "by test") } doReturn allocation
    }

    whenever(repository.findById(schedule.activityScheduleId)) doReturn Optional.of(schedule)

    service.deallocatePrisoners(
      schedule.activityScheduleId,
      PrisonerDeallocationRequest(
        listOf("1"),
        DeallocationReason.OTHER.name,
        TimeSource.tomorrow(),
        null,
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
    verify(caseNotesApiClient, never()).postCaseNote(any(), any(), any(), any(), any(), any())
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 0)
    verify(manageAttendancesService).deleteAnyAttendancesForToday(null, allocation)
    verify(manageAttendancesService, never()).sendDeletedEvent(any(), any())
    verify(telemetryClient).trackEvent(
      TelemetryEvent.PRISONER_DEALLOCATED.value,
      mapOf(PRISONER_NUMBER_PROPERTY_KEY to "1"),
    )
  }

  @Test
  fun `can add a case note when deallocating a prisoner from activity schedule`() {
    val allocation = allocation().copy(prisonerNumber = "1")

    val schedule = mock<ActivitySchedule>().stub {
      on { deallocatePrisonerOn("1", TimeSource.tomorrow(), DeallocationReason.OTHER, "by test", 10001) } doReturn
        allocation.apply { deallocateOn(LocalDate.now(), DeallocationReason.SECURITY, "test") }
      on { activity } doReturn activityEntity()
    }

    whenever(caseNotesApiClient.postCaseNote(any(), any(), any(), any(), any(), any())) doReturn CaseNote(
      caseNoteId = "10001",
      offenderIdentifier = "1",
      type = "NEG",
      typeDescription = "Negative Behaviour",
      subType = "NEG_GEN",
      subTypeDescription = "General Entry",
      source = "INST",
      creationDateTime = LocalDateTime.now(),
      occurrenceDateTime = LocalDateTime.now(),
      authorName = "Test",
      authorUserId = "1",
      text = "Test case note",
      eventId = 1,
      sensitive = false,
    )

    whenever(repository.findById(schedule.activityScheduleId)) doReturn Optional.of(schedule)

    service.deallocatePrisoners(
      schedule.activityScheduleId,
      PrisonerDeallocationRequest(
        listOf("1"),
        DeallocationReason.OTHER.name,
        TimeSource.tomorrow(),
        AddCaseNoteRequest(type = CaseNoteType.GEN, text = "Test case note"),
      ),
      "by test",
    )

    verify(caseNotesApiClient, times(1)).postCaseNote("MDI", "1", "Test case note", CaseNoteType.GEN, CaseNoteSubType.HIS, "Deallocated from activity - Other - Maths")
    verify(schedule).deallocatePrisonerOn(
      "1",
      TimeSource.tomorrow(),
      DeallocationReason.OTHER,
      "by test",
      10001,
    )
    verify(repository).saveAndFlush(schedule)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 0)
    verify(manageAttendancesService).deleteAnyAttendancesForToday(null, allocation)
    verify(manageAttendancesService, never()).sendDeletedEvent(any(), any())
    verify(telemetryClient).trackEvent(
      TelemetryEvent.PRISONER_DEALLOCATED.value,
      mapOf(PRISONER_NUMBER_PROPERTY_KEY to "1"),
    )
  }

  @Test
  fun `can deallocate multiple prisoners from activity schedule`() {
    val schedule = mock<ActivitySchedule>().stub {
      on { deallocatePrisonerOn("G0007AB", TimeSource.tomorrow(), DeallocationReason.OTHER, "by test") } doReturn allocation()
      on { deallocatePrisonerOn("A1234CD", TimeSource.tomorrow(), DeallocationReason.OTHER, "by test") } doReturn allocation()
    }

    whenever(repository.findById(schedule.activityScheduleId)).doReturn(Optional.of(schedule))

    service.deallocatePrisoners(
      schedule.activityScheduleId,
      PrisonerDeallocationRequest(
        listOf("G0007AB", "A1234CD"),
        DeallocationReason.OTHER.name,
        TimeSource.tomorrow(),
        null,
      ),
      "by test",
    )

    verify(schedule).deallocatePrisonerOn(
      "G0007AB",
      TimeSource.tomorrow(),
      DeallocationReason.OTHER,
      "by test",
    )
    verify(schedule).deallocatePrisonerOn(
      "A1234CD",
      TimeSource.tomorrow(),
      DeallocationReason.OTHER,
      "by test",
    )
    verify(manageAttendancesService, times(2)).deleteAnyAttendancesForToday(eq(null), any())
    verify(repository).saveAndFlush(schedule)
    verify(manageAttendancesService, never()).sendDeletedEvent(any(), any())
  }

  @Test
  fun `throws entity not found exception for unknown activity schedule when try and deallocate`() {
    val schedule = activitySchedule(activityEntity())
    val allocation = schedule.allocations().first()

    whenever(repository.findById(schedule.activityScheduleId)) doReturn Optional.empty()

    assertThatThrownBy {
      service.deallocatePrisoners(
        schedule.activityScheduleId,
        PrisonerDeallocationRequest(
          listOf(allocation.prisonerNumber),
          DeallocationReason.RELEASED.name,
          TimeSource.tomorrow(),
          null,
        ),
        "by test",
      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Activity Schedule ${schedule.activityScheduleId} not found")
    verifyNoInteractions(manageAttendancesService)
  }

  @Test
  fun `throws exception for invalid reason codes on attempted deallocation`() {
    val schedule = mock<ActivitySchedule>()

    whenever(repository.findById(schedule.activityScheduleId)) doReturn Optional.of(schedule)

    DeallocationReason.entries.filter { !it.displayed }.map { it.name }.forEach { reasonCode ->
      assertThatThrownBy {
        service.deallocatePrisoners(
          schedule.activityScheduleId,
          PrisonerDeallocationRequest(
            listOf("123456"),
            reasonCode,
            TimeSource.tomorrow(),
            null,
          ),
          "by test",
        )
      }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Invalid deallocation reason specified '$reasonCode'")
      verifyNoInteractions(manageAttendancesService)
    }
  }

  @Test
  fun `can deallocate and delete attendances later today`() {
    val allocation = allocation().copy(prisonerNumber = "1")

    val schedule = mock<ActivitySchedule>().stub {
      on { deallocatePrisonerOn("123456", TimeSource.tomorrow(), DeallocationReason.OTHER, "by test") } doReturn allocation
    }

    whenever(repository.findById(0)) doReturn Optional.of(schedule)

    val attendances: List<Attendance> = listOf(mock(), mock())

    whenever(manageAttendancesService.deleteAnyAttendancesForToday(123, allocation)).doReturn(attendances)

    service.deallocatePrisoners(
      schedule.activityScheduleId,
      PrisonerDeallocationRequest(
        listOf("123456"),
        DeallocationReason.OTHER.name,
        TimeSource.tomorrow(),
        null,
        123,
      ),
      "by test",
    )

    verify(schedule).deallocatePrisonerOn(
      "123456",
      TimeSource.tomorrow(),
      DeallocationReason.OTHER,
      "by test",
    )
    verify(repository).saveAndFlush(schedule)
    verify(caseNotesApiClient, never()).postCaseNote(any(), any(), any(), any(), any(), any())
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 0)
    verify(manageAttendancesService).deleteAnyAttendancesForToday(123L, allocation)
    verify(manageAttendancesService).sendDeletedEvent(attendances[0], allocation)
    verify(manageAttendancesService).sendDeletedEvent(attendances[1], allocation)
    verify(telemetryClient).trackEvent(
      TelemetryEvent.PRISONER_DEALLOCATED.value,
      mapOf(PRISONER_NUMBER_PROPERTY_KEY to "1"),
    )
  }

  @Test
  fun `throws exception if trying to deallocate and delete attendances for multiple prisoners`() {
    val schedule = mock<ActivitySchedule>()

    whenever(repository.findById(schedule.activityScheduleId)) doReturn Optional.of(schedule)

    DeallocationReason.entries.filter { !it.displayed }.map { it.name }.forEach { reasonCode ->
      assertThatThrownBy {
        service.deallocatePrisoners(
          schedule.activityScheduleId,
          PrisonerDeallocationRequest(
            listOf("123456", "333333"),
            reasonCode,
            TimeSource.tomorrow(),
            null,
            123L,
          ),
          "by test",
        )
      }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Cannot deallocate sessions later today for multiple prisoners")
      verifyNoInteractions(manageAttendancesService)
    }
  }

  @Test
  fun `allocate throws exception for start date before activity start date`() {
    val schedule = activitySchedule(activityEntity(prisonCode = PENTONVILLE_PRISON_CODE))
    schedule.activity.startDate = LocalDate.now().plusDays(2)

    whenever(repository.findById(schedule.activityScheduleId)) doReturn Optional.of(schedule)
    whenever(prisonPayBandRepository.findByPrisonCode(caseLoad)) doReturn prisonPayBandsLowMediumHigh(caseLoad)
    whenever(prisonerSearchApiClient.findByPrisonerNumber("123456")) doReturn prisoner

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
    val schedule = activitySchedule(activityEntity(prisonCode = PENTONVILLE_PRISON_CODE))
    schedule.activity.endDate = TimeSource.tomorrow()

    whenever(repository.findById(schedule.activityScheduleId)) doReturn Optional.of(schedule)
    whenever(prisonPayBandRepository.findByPrisonCode(caseLoad)) doReturn prisonPayBandsLowMediumHigh(caseLoad)
    whenever(prisonerSearchApiClient.findByPrisonerNumber("123456")) doReturn prisoner

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
    val schedule = activitySchedule(activityEntity(prisonCode = PENTONVILLE_PRISON_CODE))

    whenever(repository.findById(schedule.activityScheduleId)) doReturn Optional.of(schedule)
    whenever(prisonPayBandRepository.findByPrisonCode(caseLoad)) doReturn prisonPayBandsLowMediumHigh(caseLoad)
    whenever(prisonerSearchApiClient.findByPrisonerNumber("123456")) doReturn prisoner

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
  fun `allocate throws exception for start date in the past`() {
    val schedule = activitySchedule(activityEntity())

    assertThatThrownBy {
      service.allocatePrisoner(
        schedule.activityScheduleId,
        PrisonerAllocationRequest(
          "123456",
          1,
          TimeSource.yesterday(),
        ),
        "by test",
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Allocation start date must not be in the past")
  }

  @Test
  fun `schedule instance id should be provided if allocation starts today`() {
    val schedule = activitySchedule(activityEntity())

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
      .hasMessage("The next session must be provided when allocation start date is today")
  }

  @Test
  fun `allocate without pay band throws exception for paid activity`() {
    val schedule = activitySchedule(activityEntity(paid = true))

    whenever(repository.findById(schedule.activityScheduleId)) doReturn Optional.of(schedule)

    assertThatThrownBy {
      service.allocatePrisoner(
        schedule.activityScheduleId,
        PrisonerAllocationRequest(
          prisonerNumber = "123456",
          payBandId = null,
          TimeSource.tomorrow(),
        ),
        "by test",
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Allocation must have a pay band when the activity '1' is paid")
  }

  @Test
  fun `allocate with pay band throws exception for unpaid activity`() {
    val schedule = activitySchedule(activityEntity(paid = false, noPayBands = true), noAllocations = true)

    whenever(repository.findById(schedule.activityScheduleId)) doReturn Optional.of(schedule)

    assertThatThrownBy {
      service.allocatePrisoner(
        schedule.activityScheduleId,
        PrisonerAllocationRequest(
          prisonerNumber = "123456",
          payBandId = 1,
          TimeSource.tomorrow(),
        ),
        "by test",
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Allocation cannot have a pay band when the activity '1' is unpaid")
  }

  @Test
  fun `allocation fails if prisoner is not found`() {
    val schedule = activitySchedule(activity = pentonvilleActivity, noAllocations = true)

    whenever(repository.findById(schedule.activityScheduleId)) doReturn Optional.of(schedule)
    whenever(prisonPayBandRepository.findByPrisonCode(caseLoad)) doReturn prisonPayBandsLowMediumHigh(caseLoad)
    whenever(prisonerSearchApiClient.findByPrisonerNumber("654321")) doReturn null

    assertThatThrownBy {
      service.allocatePrisoner(
        schedule.activityScheduleId,
        PrisonerAllocationRequest(
          "654321",
          1,
          TimeSource.tomorrow(),
        ),
        "by test",
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Unable to allocate prisoner with prisoner number 654321 to schedule ${schedule.activityScheduleId}, prisoner not found.")
  }

  @Test
  fun `allocation fails if prisoner is not active at prison`() {
    val schedule = activitySchedule(activity = pentonvilleActivity, noAllocations = true)

    whenever(repository.findById(schedule.activityScheduleId)) doReturn Optional.of(schedule)
    whenever(prisonPayBandRepository.findByPrisonCode(caseLoad)) doReturn prisonPayBandsLowMediumHigh(caseLoad)
    whenever(prisonerSearchApiClient.findByPrisonerNumber("654321")) doReturn prisoner.copy(prisonId = MOORLAND_PRISON_CODE)

    assertThatThrownBy {
      service.allocatePrisoner(
        schedule.activityScheduleId,
        PrisonerAllocationRequest(
          "654321",
          1,
          TimeSource.tomorrow(),
        ),
        "by test",
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Unable to allocate prisoner with prisoner number 654321, prisoner is not active at prison $PENTONVILLE_PRISON_CODE.")
  }

  @Test
  fun `allocation fails if prisoner does not have a booking id`() {
    val schedule = activitySchedule(activity = pentonvilleActivity, noAllocations = true)

    whenever(repository.findById(schedule.activityScheduleId)) doReturn Optional.of(schedule)
    whenever(prisonPayBandRepository.findByPrisonCode(caseLoad)) doReturn prisonPayBandsLowMediumHigh(caseLoad)
    whenever(prisonerSearchApiClient.findByPrisonerNumber("654321")) doReturn prisoner.copy(bookingId = null)

    assertThatThrownBy {
      service.allocatePrisoner(
        schedule.activityScheduleId,
        PrisonerAllocationRequest(
          "654321",
          1,
          TimeSource.tomorrow(),
        ),
        "by test",
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Unable to allocate prisoner with prisoner number 654321, prisoner does not have a booking id.")
  }

  @Test
  fun `should successfully allocate an active in prison prisoner`() {
    val schedule = activitySchedule(activity = pentonvilleActivity, noAllocations = true)

    whenever(repository.findById(schedule.activityScheduleId)) doReturn Optional.of(schedule)
    whenever(prisonPayBandRepository.findByPrisonCode(caseLoad)) doReturn prisonPayBandsLowMediumHigh(caseLoad)
    whenever(prisonerSearchApiClient.findByPrisonerNumber("654321")) doReturn activeInPentonvillePrisoner
    whenever(repository.saveAndFlush(any())) doReturn schedule
    whenever(waitingListRepository.findByPrisonCodeAndPrisonerNumberAndActivitySchedule(any(), any(), any())) doReturn emptyList()

    val attendance1: Attendance = mock()
    val attendance2: Attendance = mock()
    val newAttendances: List<Attendance> = listOf(attendance1, attendance2)
    whenever(manageAttendancesService.createAnyAttendancesForToday(eq(123L), any())) doReturn newAttendances
    whenever(manageAttendancesService.saveAttendances(newAttendances, schedule.description)) doReturn newAttendances

    service.allocatePrisoner(
      schedule.activityScheduleId,
      PrisonerAllocationRequest(
        "654321",
        1,
        TimeSource.tomorrow(),
        scheduleInstanceId = 123L,
      ),
      "by test",
    )

    schedule.allocations().single().prisonerNumber isEqualTo "654321"

    verify(auditService).logEvent(any())
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATED, 0)
    inOrder(manageAttendancesService) {
      verify(manageAttendancesService).createAnyAttendancesForToday(eq(123L), any())
      verify(manageAttendancesService).saveAttendances(newAttendances, schedule.description)
      verify(manageAttendancesService).sendCreatedEvent(attendance1)
      verify(manageAttendancesService).sendCreatedEvent(attendance2)
    }
  }

  @Test
  fun `should successfully allocate an active out prison prisoner`() {
    val schedule = activitySchedule(activity = pentonvilleActivity, noAllocations = true)

    whenever(repository.findById(schedule.activityScheduleId)) doReturn Optional.of(schedule)
    whenever(prisonPayBandRepository.findByPrisonCode(caseLoad)) doReturn prisonPayBandsLowMediumHigh(caseLoad)
    whenever(prisonerSearchApiClient.findByPrisonerNumber("654321")) doReturn activeOutPentonvillePrisoner
    whenever(repository.saveAndFlush(any())) doReturn schedule
    whenever(waitingListRepository.findByPrisonCodeAndPrisonerNumberAndActivitySchedule(any(), any(), any())) doReturn emptyList()

    service.allocatePrisoner(
      schedule.activityScheduleId,
      PrisonerAllocationRequest(
        "654321",
        1,
        TimeSource.tomorrow(),
      ),
      "by test",
    )

    schedule.allocations().single().prisonerNumber isEqualTo "654321"

    verify(auditService).logEvent(any())
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATED, 0)
  }

  @Test
  fun `should audit successful allocation of prisoner`() {
    val schedule = activitySchedule(activity = pentonvilleActivity, noAllocations = true)

    whenever(repository.findById(schedule.activityScheduleId)) doReturn Optional.of(schedule)
    whenever(prisonPayBandRepository.findByPrisonCode(caseLoad)) doReturn prisonPayBandsLowMediumHigh(caseLoad)
    whenever(prisonerSearchApiClient.findByPrisonerNumber("654321")) doReturn activeOutPentonvillePrisoner
    whenever(repository.saveAndFlush(any())) doReturn schedule
    whenever(waitingListRepository.findByPrisonCodeAndPrisonerNumberAndActivitySchedule(any(), any(), any())) doReturn emptyList()

    service.allocatePrisoner(
      schedule.activityScheduleId,
      PrisonerAllocationRequest(
        "654321",
        1,
        TimeSource.tomorrow(),
      ),
      "by test",
    )

    verify(auditService).logEvent(auditCaptor.capture())

    with(auditCaptor.firstValue) {
      activityId isEqualTo pentonvilleActivity.activityId
      activityName isEqualTo pentonvilleActivity.summary
      prisonCode isEqualTo PENTONVILLE_PRISON_CODE
      prisonerNumber isEqualTo "654321"
      scheduleId isEqualTo schedule.activityScheduleId
      scheduleDescription isEqualTo schedule.description
      waitingListId isEqualTo null
      createdAt isCloseTo TimeSource.now().truncatedTo(ChronoUnit.MINUTES)
    }

    verify(telemetryClient).trackEvent(
      TelemetryEvent.CREATE_ALLOCATION.value,
      mapOf(
        USER_PROPERTY_KEY to "by test",
        PRISON_CODE_PROPERTY_KEY to PENTONVILLE_PRISON_CODE,
        PRISONER_NUMBER_PROPERTY_KEY to "654321",
        ACTIVITY_ID_PROPERTY_KEY to "${pentonvilleActivity.activityId}",
        ALLOCATION_START_DATE_PROPERTY_KEY to TimeSource.tomorrow().toString(),
      ),
      emptyMap(),
    )

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATED, 0)
  }

  @Test
  fun `allocation updates APPROVED waiting list application to ALLOCATED status when present`() {
    val schedule = activitySchedule(activity = pentonvilleActivity, noAllocations = true)

    val waitingListEntity = waitingList(
      prisonCode = schedule.activity.prisonCode,
      initialStatus = WaitingListStatus.APPROVED,
      waitingListId = 300,
    )

    whenever(repository.findById(schedule.activityScheduleId)) doReturn Optional.of(schedule)
    whenever(prisonPayBandRepository.findByPrisonCode(caseLoad)) doReturn prisonPayBandsLowMediumHigh(caseLoad)
    whenever(prisonerSearchApiClient.findByPrisonerNumber("123456")) doReturn prisoner
    whenever(repository.saveAndFlush(any())) doReturn schedule
    whenever(
      waitingListRepository.findByPrisonCodeAndPrisonerNumberAndActivitySchedule(
        caseLoad,
        "123456",
        schedule,
      ),
    ) doReturn listOf(waitingListEntity)

    service.allocatePrisoner(
      schedule.activityScheduleId,
      PrisonerAllocationRequest(
        "123456",
        1,
        TimeSource.tomorrow(),
      ),
      "by test",
    )

    schedule.allocations().single().prisonerNumber isEqualTo "123456"

    with(waitingListEntity) {
      assertThat(status).isEqualTo(WaitingListStatus.ALLOCATED)
      assertThat(allocation?.allocatedBy).isEqualTo("by test")
      assertThat(allocation?.prisonerNumber).isEqualTo("123456")
    }
  }

  @Test
  fun `allocation fails if more than one approved waiting list`() {
    val schedule = activitySchedule(activity = activityEntity(prisonCode = PENTONVILLE_PRISON_CODE))
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

    whenever(repository.findById(schedule.activityScheduleId)) doReturn Optional.of(schedule)
    whenever(prisonPayBandRepository.findByPrisonCode(caseLoad)) doReturn prisonPayBandsLowMediumHigh(caseLoad)
    whenever(prisonerSearchApiClient.findByPrisonerNumber("123456")) doReturn prisoner
    whenever(repository.saveAndFlush(any())) doReturn schedule
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
      .hasMessage("Prisoner has more than one APPROVED waiting list application. A prisoner can only have one approved waiting list application.")
  }

  @Test
  fun `allocation fails if pending waiting list`() {
    val schedule = activitySchedule(activity = activityEntity(prisonCode = PENTONVILLE_PRISON_CODE))
    val waitingLists = listOf(
      waitingList(
        prisonCode = schedule.activity.prisonCode,
        initialStatus = WaitingListStatus.PENDING,
        waitingListId = 1,
      ),
    )

    whenever(repository.findById(schedule.activityScheduleId)) doReturn Optional.of(schedule)
    whenever(prisonPayBandRepository.findByPrisonCode(caseLoad)) doReturn prisonPayBandsLowMediumHigh(caseLoad)
    whenever(prisonerSearchApiClient.findByPrisonerNumber("123456")) doReturn prisoner
    whenever(repository.saveAndFlush(any())) doReturn schedule
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
      .hasMessage("Prisoner has a PENDING waiting list application. It must be APPROVED before they can be allocated.")
  }

  @Test
  fun `should retrieve schedule by id and date`() {
    val schedule = pentonvilleActivity.schedule()

    whenever(repository.getActivityScheduleByIdWithFilters(schedule.activityScheduleId, TimeSource.today())) doReturn schedule

    service.getScheduleById(schedule.activityScheduleId, TimeSource.today()) isEqualTo schedule.toModelSchedule()
  }

  @Test
  fun `should fail to retrieve schedule by id and date when invalid case load`() {
    val schedule = moorlandActivity.schedule()

    whenever(repository.getActivityScheduleByIdWithFilters(schedule.activityScheduleId, TimeSource.today())) doReturn schedule

    assertThatThrownBy {
      service.getScheduleById(schedule.activityScheduleId, TimeSource.today()) isEqualTo schedule.toModelSchedule()
    }.isInstanceOf(CaseloadAccessException::class.java)
  }

  @Test
  fun `should fail to retrieve schedule by id and date when schedule not found`() {
    val schedule = pentonvilleActivity.schedule()

    whenever(repository.getActivityScheduleByIdWithFilters(schedule.activityScheduleId, TimeSource.today())) doReturn null

    assertThatThrownBy {
      service.getScheduleById(schedule.activityScheduleId, TimeSource.today()) isEqualTo schedule.toModelSchedule()
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Activity schedule ID ${schedule.activityScheduleId} not found")
  }
}
