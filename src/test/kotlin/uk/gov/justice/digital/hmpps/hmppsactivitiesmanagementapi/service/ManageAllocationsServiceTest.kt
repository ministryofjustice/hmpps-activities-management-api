package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.movement
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.rolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonRegimeRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import java.time.LocalDate
import java.time.LocalDateTime

class ManageAllocationsServiceTest {

  private val rolloutPrisonRepo: RolloutPrisonRepository = mock()
  private val activityRepo: ActivityRepository = mock()
  private val activityScheduleRepo: ActivityScheduleRepository = mock()
  private val allocationRepository: AllocationRepository = mock()
  private val prisonRegimeRepository: PrisonRegimeRepository = mock()
  private val searchApiClient: PrisonerSearchApiApplicationClient = mock()
  private val waitingListService: WaitingListService = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val prisonApi: PrisonApiApplicationClient = mock()

  private val service =
    ManageAllocationsService(
      rolloutPrisonRepo,
      activityRepo,
      activityScheduleRepo,
      allocationRepository,
      prisonRegimeRepository,
      searchApiClient,
      waitingListService,
      TransactionHandler(),
      outboundEventsService,
      prisonApi,
    )
  private val yesterday = LocalDate.now().minusDays(1)
  private val today = yesterday.plusDays(1)

  @BeforeEach
  fun setup() {
    whenever(searchApiClient.findByPrisonerNumbers(any())) doReturn Mono.just(emptyList())
  }

  @Test
  fun `deallocate offenders from activity ending today without pending deallocation`() {
    val prison = rolloutPrison()
    val activity = activityEntity(startDate = yesterday, endDate = today)
    val schedule = activity.schedules().first()
    val allocation = schedule.allocations().first().also { it.verifyIsActive() }

    whenever(rolloutPrisonRepo.findAll()).thenReturn(listOf(prison))
    whenever(activityRepo.getAllForPrisonAndDate(prison.code, LocalDate.now())).thenReturn(listOf(activity))

    service.allocations(AllocationOperation.ENDING_TODAY)

    allocation.verifyIsEnded()

    verify(activityScheduleRepo).saveAndFlush(schedule)
  }

  @Test
  fun `deallocate offenders from activity ending today declines pending or approved waiting lists`() {
    val prison = rolloutPrison()
    val activity = activityEntity(startDate = yesterday, endDate = today)

    whenever(rolloutPrisonRepo.findAll()).thenReturn(listOf(prison))
    whenever(activityRepo.getAllForPrisonAndDate(prison.code, LocalDate.now())).thenReturn(listOf(activity))

    service.allocations(AllocationOperation.ENDING_TODAY)

    verify(waitingListService).declinePendingOrApprovedApplications(
      activity.activityId,
      "Activity ended",
      "Activities Management Service",
    )
  }

  @Test
  fun `deallocate offenders from activity ending today with pending deallocation`() {
    val prison = rolloutPrison()
    val activity = activityEntity(startDate = yesterday, endDate = today)
    val schedule = activity.schedules().first()
    val allocation = schedule.allocations().first().also { it.verifyIsActive() }
    allocation.deallocateOn(today, DeallocationReason.OTHER, "by test")

    whenever(rolloutPrisonRepo.findAll()).thenReturn(listOf(prison))
    whenever(activityRepo.getAllForPrisonAndDate(prison.code, LocalDate.now())).thenReturn(listOf(activity))

    service.allocations(AllocationOperation.ENDING_TODAY)

    allocation.verifyIsEnded(DeallocationReason.OTHER, "by test")

    verify(activityScheduleRepo).saveAndFlush(schedule)
  }

  @Test
  fun `deallocate offenders from activity with no end date and allocation ends today`() {
    val prison = rolloutPrison()
    val activity = activityEntity(startDate = yesterday, endDate = null)
    val schedule = activity.schedules().first()
    val allocation = schedule.allocations().first().apply { endDate = today }.also { it.verifyIsActive() }

    whenever(rolloutPrisonRepo.findAll()).thenReturn(listOf(prison))
    whenever(activityRepo.getAllForPrisonAndDate(prison.code, LocalDate.now())).thenReturn(listOf(activity))

    service.allocations(AllocationOperation.ENDING_TODAY)

    allocation.verifyIsEnded()

    verify(activityScheduleRepo).saveAndFlush(schedule)
    verifyNoInteractions(waitingListService)
  }

  @Test
  fun `offenders not deallocated from activity with no end date and allocation does not end today`() {
    val prison = rolloutPrison()
    val activity = activityEntity(startDate = yesterday, endDate = null)
    val schedule = activity.schedules().first()
    val allocation = schedule.allocations().first().also { it.verifyIsActive() }

    whenever(rolloutPrisonRepo.findAll()).thenReturn(listOf(prison))
    whenever(activityRepo.getAllForPrisonAndDate(prison.code, LocalDate.now())).thenReturn(listOf(activity))

    service.allocations(AllocationOperation.ENDING_TODAY)

    allocation.verifyIsActive()

    verify(activityScheduleRepo, never()).saveAndFlush(any())
    verifyNoInteractions(waitingListService)
  }

  @Test
  fun `deallocate offenders from activities across multiple prisons`() {
    val moorland = rolloutPrison().copy(code = moorlandPrisonCode)
    val pentonville = rolloutPrison()
    whenever(rolloutPrisonRepo.findAll()).thenReturn(listOf(pentonville, moorland))

    val pentonvilleActivity = activityEntity(activityId = 1, startDate = yesterday, endDate = today)
    val moorlandActivity = activityEntity(activityId = 2, startDate = yesterday, endDate = today)
    whenever(activityRepo.getAllForPrisonAndDate(pentonville.code, today)).thenReturn(listOf(pentonvilleActivity))
    whenever(activityRepo.getAllForPrisonAndDate(moorland.code, today)).thenReturn(listOf(moorlandActivity))

    service.allocations(AllocationOperation.ENDING_TODAY)

    listOf(pentonvilleActivity, moorlandActivity).onEach { activity ->
      with(activity) {
        this.schedules().first().allocations().forEach { assertThat(it.status(PrisonerStatus.ENDED)).isTrue() }
        verify(activityScheduleRepo).saveAndFlush(this.schedules().first())
      }
    }
  }

  @Test
  fun `deallocate offenders from activities ending today declines pending or approved waiting lists`() {
    val moorland = rolloutPrison().copy(code = moorlandPrisonCode)
    val pentonville = rolloutPrison()
    whenever(rolloutPrisonRepo.findAll()).thenReturn(listOf(pentonville, moorland))

    val pentonvilleActivity = activityEntity(activityId = 1, startDate = yesterday, endDate = today)
    val moorlandActivity = activityEntity(activityId = 2, startDate = yesterday, endDate = today)
    whenever(activityRepo.getAllForPrisonAndDate(pentonville.code, today)).thenReturn(listOf(pentonvilleActivity))
    whenever(activityRepo.getAllForPrisonAndDate(moorland.code, today)).thenReturn(listOf(moorlandActivity))

    service.allocations(AllocationOperation.ENDING_TODAY)

    verify(waitingListService).declinePendingOrApprovedApplications(
      pentonvilleActivity.activityId,
      "Activity ended",
      "Activities Management Service",
    )
    verify(waitingListService).declinePendingOrApprovedApplications(
      moorlandActivity.activityId,
      "Activity ended",
      "Activities Management Service",
    )
  }

  @Test
  fun `prisoners are deallocated from allocations pending due to expire`() {
    val prison = rolloutPrison()
    val activity = activityEntity(startDate = TimeSource.tomorrow())
    val schedule = activity.schedules().first()
    val allocation = schedule.allocations().first().also { it.prisonerStatus isEqualTo PrisonerStatus.PENDING }
    val prisoner: Prisoner = mock {
      on { inOutStatus } doReturn Prisoner.InOutStatus.OUT
      on { prisonerNumber } doReturn allocation.prisonerNumber
    }

    whenever(rolloutPrisonRepo.findAll()) doReturn listOf(prison)
    whenever(prisonRegimeRepository.findByPrisonCode(prison.code)) doReturn prisonRegime()
    whenever(allocationRepository.findByPrisonCodePrisonerStatus(prison.code, PrisonerStatus.PENDING)) doReturn listOf(
      allocation,
    )
    whenever(searchApiClient.findByPrisonerNumbers(listOf(prisoner.prisonerNumber))) doReturn Mono.just(listOf(prisoner))

    whenever(prisonApi.getMovementsForPrisonersFromPrison(prison.code, setOf(allocation.prisonerNumber))) doReturn
      listOf(movement(prisonerNumber = allocation.prisonerNumber, movementDate = TimeSource.yesterday()))

    service.allocations(AllocationOperation.EXPIRING_TODAY)

    verify(waitingListService).declinePendingOrApprovedApplications(
      prison.code,
      allocation.prisonerNumber,
      "Released",
      "Activities Management Service",
    )

    allocation.verifyIsExpired()

    verify(activityScheduleRepo).saveAndFlush(schedule)
  }

  @Test
  fun `prisoners are deallocated from allocations pending due to expire when at a different prison`() {
    val prison = rolloutPrison()
    val activity = activityEntity(startDate = TimeSource.tomorrow())
    val schedule = activity.schedules().first()
    val allocation = schedule.allocations().first().also { it.prisonerStatus isEqualTo PrisonerStatus.PENDING }
    val prisonerInAtOtherPrison: Prisoner = mock {
      on { inOutStatus } doReturn Prisoner.InOutStatus.IN
      on { prisonerNumber } doReturn allocation.prisonerNumber
      on { prisonId } doReturn prison.code.plus("-other")
    }

    whenever(rolloutPrisonRepo.findAll()) doReturn listOf(prison)
    whenever(prisonRegimeRepository.findByPrisonCode(prison.code)) doReturn prisonRegime()
    whenever(allocationRepository.findByPrisonCodePrisonerStatus(prison.code, PrisonerStatus.PENDING)) doReturn listOf(
      allocation,
    )
    whenever(searchApiClient.findByPrisonerNumbers(listOf(prisonerInAtOtherPrison.prisonerNumber))) doReturn Mono.just(listOf(prisonerInAtOtherPrison))

    whenever(prisonApi.getMovementsForPrisonersFromPrison(prison.code, setOf(allocation.prisonerNumber))) doReturn
      listOf(movement(prisonerNumber = allocation.prisonerNumber, fromPrisonCode = prison.code, movementDate = TimeSource.yesterday()))

    service.allocations(AllocationOperation.EXPIRING_TODAY)

    verify(waitingListService).declinePendingOrApprovedApplications(
      prison.code,
      allocation.prisonerNumber,
      "Released",
      "Activities Management Service",
    )

    allocation.verifyIsExpired()

    verify(activityScheduleRepo).saveAndFlush(schedule)
  }

  @Test
  fun `prisoners are are not deallocated from allocations pending as not due to expire`() {
    val prison = rolloutPrison()
    val activity = activityEntity(startDate = TimeSource.tomorrow())
    val schedule = activity.schedules().first()
    val allocation = schedule.allocations().first().also { it.prisonerStatus isEqualTo PrisonerStatus.PENDING }
    val prisoner: Prisoner = mock {
      on { inOutStatus } doReturn Prisoner.InOutStatus.OUT
      on { prisonerNumber } doReturn allocation.prisonerNumber
      on { prisonId } doReturn prison.code
    }

    whenever(rolloutPrisonRepo.findAll()) doReturn listOf(prison)
    whenever(prisonRegimeRepository.findByPrisonCode(prison.code)) doReturn prisonRegime()
    whenever(allocationRepository.findByPrisonCodePrisonerStatus(prison.code, PrisonerStatus.PENDING)) doReturn listOf(
      allocation,
    )
    whenever(searchApiClient.findByPrisonerNumbers(listOf(prisoner.prisonerNumber))) doReturn Mono.just(listOf(prisoner))

    // Multiple moves to demonstrate takes the latest move for an offender
    whenever(prisonApi.getMovementsForPrisonersFromPrison(prison.code, setOf(allocation.prisonerNumber))) doReturn
      listOf(
//        movement(prisonerNumber = allocation.prisonerNumber, movementDate = TimeSource.yesterday()),
        movement(prisonerNumber = allocation.prisonerNumber, fromPrisonCode = prison.code, movementDate = TimeSource.today()),
      )

    service.allocations(AllocationOperation.EXPIRING_TODAY)

    verify(waitingListService, never()).declinePendingOrApprovedApplications(any(), any(), any(), any())

    allocation.prisonerStatus isEqualTo PrisonerStatus.PENDING

    verify(activityScheduleRepo, never()).saveAndFlush(schedule)
  }

  @Test
  fun `prisoners due to expire waiting lists are declined`() {
    val prison = rolloutPrison()
    val activity = activityEntity(startDate = yesterday, endDate = today)
    val schedule = activity.schedules().first()
    val allocation = schedule.allocations().first().autoSuspend(LocalDateTime.now().minusDays(5), "reason")
    val prisoner: Prisoner = mock {
      on { inOutStatus } doReturn Prisoner.InOutStatus.OUT
      on { prisonerNumber } doReturn allocation.prisonerNumber
    }

    whenever(rolloutPrisonRepo.findAll()) doReturn listOf(prison)
    whenever(prisonRegimeRepository.findByPrisonCode(prison.code)) doReturn prisonRegime()
    whenever(
      allocationRepository.findByPrisonCodePrisonerStatus(
        prison.code,
        PrisonerStatus.AUTO_SUSPENDED,
      ),
    ) doReturn listOf(allocation)
    whenever(searchApiClient.findByPrisonerNumbers(listOf(prisoner.prisonerNumber))) doReturn Mono.just(listOf(prisoner))
    whenever(prisonApi.getMovementsForPrisonersFromPrison(prison.code, setOf(allocation.prisonerNumber))) doReturn
      listOf(movement(prisonerNumber = allocation.prisonerNumber, movementDate = TimeSource.yesterday()))

    service.allocations(AllocationOperation.EXPIRING_TODAY)

    verify(waitingListService).declinePendingOrApprovedApplications(
      prison.code,
      allocation.prisonerNumber,
      "Released",
      "Activities Management Service",
    )
  }

  @Test
  fun `prison is skipped if regime config is missing`() {
    val prisonWithRegime = rolloutPrison()
    val prisonWithoutRegime = rolloutPrison().copy(rolloutPrisonId = 2, code = moorlandPrisonCode)

    val activity = activityEntity(startDate = yesterday, endDate = today)
    val schedule = activity.schedules().first()
    val allocation = schedule.allocations().first().autoSuspend(LocalDateTime.now().minusDays(5), "reason")
    val prisoner: Prisoner = mock {
      on { inOutStatus } doReturn Prisoner.InOutStatus.OUT
      on { prisonerNumber } doReturn allocation.prisonerNumber
    }

    whenever(rolloutPrisonRepo.findAll()).doReturn(listOf(prisonWithRegime, prisonWithoutRegime))
    whenever(prisonRegimeRepository.findByPrisonCode(prisonWithRegime.code)).doReturn(prisonRegime())
    whenever(prisonRegimeRepository.findByPrisonCode(prisonWithoutRegime.code)).doReturn(null)
    whenever(
      allocationRepository.findByPrisonCodePrisonerStatus(
        prisonWithRegime.code,
        PrisonerStatus.AUTO_SUSPENDED,
      ),
    ).doReturn(
      listOf(allocation),
    )
    whenever(searchApiClient.findByPrisonerNumbers(listOf(prisoner.prisonerNumber))).doReturn(Mono.just(listOf(prisoner)))
    whenever(
      prisonApi.getMovementsForPrisonersFromPrison(
        prisonWithRegime.code,
        setOf(allocation.prisonerNumber),
      ),
    ) doReturn
      listOf(movement(prisonerNumber = allocation.prisonerNumber, movementDate = TimeSource.yesterday()))

    service.allocations(AllocationOperation.EXPIRING_TODAY)

    allocation.verifyIsExpired()

    verify(activityScheduleRepo).saveAndFlush(schedule)
  }

  @Test
  fun `pending allocations on or before today are correctly activated`() {
    val prison = rolloutPrison().also {
      whenever(rolloutPrisonRepo.findAll()) doReturn listOf(it)
    }

    val pendingAllocationYesterday: Allocation = allocation().copy(
      allocationId = 1,
      prisonerNumber = "1",
      startDate = TimeSource.yesterday(),
      prisonerStatus = PrisonerStatus.PENDING,
    ).also {
      whenever(searchApiClient.findByPrisonerNumber(it.prisonerNumber)) doReturn prisoner(it, Prisoner.InOutStatus.IN)
    }

    val pendingAllocationToday: Allocation = allocation().copy(
      allocationId = 2,
      prisonerNumber = "2",
      startDate = TimeSource.today(),
      prisonerStatus = PrisonerStatus.PENDING,
    ).also {
      whenever(searchApiClient.findByPrisonerNumber(it.prisonerNumber)) doReturn prisoner(it, Prisoner.InOutStatus.IN)
    }

    whenever(
      allocationRepository.findByPrisonCodePrisonerStatusStartingOnOrBeforeDate(
        prison.code,
        PrisonerStatus.PENDING,
        TimeSource.today(),
      ),
    ) doReturn listOf(pendingAllocationYesterday, pendingAllocationToday)

    pendingAllocationYesterday.prisonerStatus isEqualTo PrisonerStatus.PENDING
    pendingAllocationToday.prisonerStatus isEqualTo PrisonerStatus.PENDING

    service.allocations(AllocationOperation.STARTING_TODAY)

    pendingAllocationYesterday.prisonerStatus isEqualTo PrisonerStatus.ACTIVE
    pendingAllocationToday.prisonerStatus isEqualTo PrisonerStatus.ACTIVE

    verify(allocationRepository).saveAndFlush(pendingAllocationYesterday)
    verify(allocationRepository).saveAndFlush(pendingAllocationToday)
  }

  @Test
  fun `pending allocations on or before today are auto-suspended when prisoner is out of prison`() {
    val prison = rolloutPrison().also {
      whenever(rolloutPrisonRepo.findAll()) doReturn listOf(it)
    }

    val pendingAllocationYesterday: Allocation = allocation().copy(
      allocationId = 1,
      prisonerNumber = "1",
      startDate = TimeSource.yesterday(),
      prisonerStatus = PrisonerStatus.PENDING,
    ).also {
      whenever(searchApiClient.findByPrisonerNumber(it.prisonerNumber)) doReturn prisoner(it, Prisoner.InOutStatus.OUT)
    }

    val pendingAllocationToday: Allocation = allocation().copy(
      allocationId = 2,
      prisonerNumber = "2",
      startDate = TimeSource.today(),
      prisonerStatus = PrisonerStatus.PENDING,
    ).also {
      whenever(searchApiClient.findByPrisonerNumber(it.prisonerNumber)) doReturn prisoner(it, Prisoner.InOutStatus.OUT)
    }

    whenever(
      allocationRepository.findByPrisonCodePrisonerStatusStartingOnOrBeforeDate(
        prison.code,
        PrisonerStatus.PENDING,
        TimeSource.today(),
      ),
    ) doReturn listOf(pendingAllocationYesterday, pendingAllocationToday)

    pendingAllocationYesterday.prisonerStatus isEqualTo PrisonerStatus.PENDING
    pendingAllocationToday.prisonerStatus isEqualTo PrisonerStatus.PENDING

    service.allocations(AllocationOperation.STARTING_TODAY)

    listOf(pendingAllocationYesterday, pendingAllocationToday).forEach { allocation ->
      allocation.prisonerStatus isEqualTo PrisonerStatus.AUTO_SUSPENDED
      allocation.suspendedReason isEqualTo "Temporarily released or transferred"
      allocation.suspendedTime isCloseTo TimeSource.now()
    }

    verify(allocationRepository).saveAndFlush(pendingAllocationYesterday)
    verify(allocationRepository).saveAndFlush(pendingAllocationToday)
  }

  @Test
  fun `pending allocation not processed when prisoner not found`() {
    val prison = rolloutPrison().also {
      whenever(rolloutPrisonRepo.findAll()) doReturn listOf(it)
    }

    val pendingAllocation: Allocation = allocation().copy(
      allocationId = 1,
      prisonerNumber = "1",
      startDate = TimeSource.yesterday(),
      prisonerStatus = PrisonerStatus.PENDING,
    ).also {
      whenever(searchApiClient.findByPrisonerNumber(it.prisonerNumber)) doReturn null
    }

    whenever(
      allocationRepository.findByPrisonCodePrisonerStatusStartingOnOrBeforeDate(
        prison.code,
        PrisonerStatus.PENDING,
        TimeSource.today(),
      ),
    ) doReturn listOf(pendingAllocation)

    pendingAllocation.prisonerStatus isEqualTo PrisonerStatus.PENDING

    service.allocations(AllocationOperation.STARTING_TODAY)

    pendingAllocation.prisonerStatus isEqualTo PrisonerStatus.PENDING

    verify(allocationRepository, never()).saveAndFlush(pendingAllocation)
  }

  private fun prisoner(allocation: Allocation, status: Prisoner.InOutStatus): Prisoner =
    PrisonerSearchPrisonerFixture.instance(prisonerNumber = allocation.prisonerNumber, inOutStatus = status)

  private fun Allocation.verifyIsActive() {
    prisonerStatus isEqualTo PrisonerStatus.ACTIVE
  }

  private fun Allocation.verifyIsEnded(
    reason: DeallocationReason = DeallocationReason.ENDED,
    endedBy: String? = "Activities Management Service",
  ) {
    prisonerStatus isEqualTo PrisonerStatus.ENDED
    deallocatedTime!! isCloseTo TimeSource.now()
    deallocatedReason isEqualTo reason
    deallocatedBy isEqualTo endedBy
  }

  private fun Allocation.verifyIsExpired() {
    prisonerStatus isEqualTo PrisonerStatus.ENDED
    deallocatedTime!! isCloseTo TimeSource.now()
    deallocatedReason isEqualTo DeallocationReason.TEMPORARILY_RELEASED
    deallocatedBy isEqualTo "Activities Management Service"
  }
}
