package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Job
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.enumeration.ServiceName
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.movement
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.rolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.waitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.JobEventMessage
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.JobsSqsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.PrisonCodeJobEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageScheduledInstancesServiceTest.Companion.rolledOutPrisons
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService

class ManageAllocationsDueToExpireServiceTest {

  private val rolloutPrisonService: RolloutPrisonService = mock {
    on { getRolloutPrisons() } doReturn listOf(rolloutPrison(PENTONVILLE_PRISON_CODE), rolloutPrison(MOORLAND_PRISON_CODE))
  }
  private val activityScheduleRepository: ActivityScheduleRepository = mock()
  private val allocationRepository: AllocationRepository = mock()
  private val waitingListService: WaitingListService = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val prisonApiClient: PrisonApiClient = mock()
  private val monitoringService: MonitoringService = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val jobsSqsService: JobsSqsService = mock()
  private val jobService: JobService = mock()

  private val service =
    ManageAllocationsDueToExpireService(
      rolloutPrisonService,
      activityScheduleRepository,
      waitingListService,
      TransactionHandler(),
      prisonApiClient,
      prisonerSearchApiClient,
      jobsSqsService,
      jobService,
      allocationRepository,
      outboundEventsService,
      monitoringService,
    )

  @BeforeEach
  fun setUp() {
    whenever(rolloutPrisonService.isActivitiesRolledOutAt(PENTONVILLE_PRISON_CODE)) doReturn true
    whenever(rolloutPrisonService.isActivitiesRolledOutAt(MOORLAND_PRISON_CODE)) doReturn true
  }

  @Test
  fun `prisoners are deallocated from allocations pending due to expire`() {
    val prison = rolloutPrison()
    val activity = activityEntity(startDate = TimeSource.tomorrow())
    val schedule = activity.schedules().first()
    val allocation = schedule.allocations().first().also { it.prisonerStatus isEqualTo PrisonerStatus.PENDING }
    val prisonerInAtOtherPrison: Prisoner = mock {
      on { inOutStatus } doReturn Prisoner.InOutStatus.IN
      on { prisonerNumber } doReturn allocation.prisonerNumber
      on { prisonId } doReturn prison.prisonCode.plus("-other")
    }

    whenever(rolloutPrisonService.getRolloutPrisons()) doReturn listOf(prison)
    whenever(rolloutPrisonService.getByPrisonCode(prison.prisonCode)) doReturn prison
    whenever(allocationRepository.findByPrisonCodePrisonerStatus(prison.prisonCode, listOf(PrisonerStatus.PENDING))) doReturn listOf(
      allocation,
    )
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(listOf(prisonerInAtOtherPrison.prisonerNumber))) doReturn listOf(prisonerInAtOtherPrison)

    whenever(prisonApiClient.getMovementsForPrisonersFromPrison(prison.prisonCode, setOf(allocation.prisonerNumber))) doReturn
      listOf(movement(prisonerNumber = allocation.prisonerNumber, movementDate = TimeSource.yesterday()))

    service.deallocateAllocationsDueToExpire()

    allocation.verifyIsExpired()

    verify(activityScheduleRepository).saveAndFlush(schedule)
  }

  @Test
  fun `should capture failures in monitoring service for any exceptions when expiring`() {
    val exception = RuntimeException("Something went wrong")
    doThrow(exception).whenever(activityScheduleRepository).saveAndFlush(any())
    val prison = rolloutPrison()
    val activity = activityEntity(startDate = TimeSource.tomorrow())
    val schedule = activity.schedules().first()
    val allocation = schedule.allocations().first().also { it.prisonerStatus isEqualTo PrisonerStatus.PENDING }
    val prisoner: Prisoner = mock {
      on { inOutStatus } doReturn Prisoner.InOutStatus.OUT
      on { prisonerNumber } doReturn allocation.prisonerNumber
    }

    whenever(rolloutPrisonService.getRolloutPrisons()) doReturn listOf(prison)
    whenever(rolloutPrisonService.getByPrisonCode(prison.prisonCode)) doReturn prison
    whenever(allocationRepository.findByPrisonCodePrisonerStatus(prison.prisonCode, listOf(PrisonerStatus.PENDING))) doReturn listOf(allocation)
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(listOf(prisoner.prisonerNumber))) doReturn listOf(prisoner)
    whenever(prisonApiClient.getMovementsForPrisonersFromPrison(prison.prisonCode, setOf(allocation.prisonerNumber))) doReturn listOf(movement(prisonerNumber = allocation.prisonerNumber, movementDate = TimeSource.yesterday()))

    service.deallocateAllocationsDueToExpire()

    verify(monitoringService).capture("An error occurred deallocating allocations on activity schedule 1", exception)
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
      on { prisonId } doReturn prison.prisonCode.plus("-other")
    }

    whenever(rolloutPrisonService.getRolloutPrisons()) doReturn listOf(prison)
    whenever(rolloutPrisonService.getByPrisonCode(prison.prisonCode)) doReturn prison
    whenever(allocationRepository.findByPrisonCodePrisonerStatus(prison.prisonCode, listOf(PrisonerStatus.PENDING))) doReturn listOf(
      allocation,
    )
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(listOf(prisonerInAtOtherPrison.prisonerNumber))) doReturn listOf(prisonerInAtOtherPrison)

    whenever(prisonApiClient.getMovementsForPrisonersFromPrison(prison.prisonCode, setOf(allocation.prisonerNumber))) doReturn
      listOf(movement(prisonerNumber = allocation.prisonerNumber, fromPrisonCode = prison.prisonCode, movementDate = TimeSource.yesterday()))

    service.deallocateAllocationsDueToExpire()

    allocation.verifyIsExpired()

    verify(activityScheduleRepository).saveAndFlush(schedule)
  }

  @Test
  fun `prisoners are are not deallocated from allocations pending as not due to expire`() {
    val prison = rolloutPrison()
    val activity = activityEntity(startDate = TimeSource.tomorrow())
    val schedule = activity.schedules().first()
    val allocation = schedule.allocations().first().also { it.prisonerStatus isEqualTo PrisonerStatus.PENDING }

    whenever(rolloutPrisonService.getRolloutPrisons()) doReturn listOf(prison)
    whenever(rolloutPrisonService.getByPrisonCode(prison.prisonCode)) doReturn prison
    whenever(allocationRepository.findByPrisonCodePrisonerStatus(prison.prisonCode, listOf(PrisonerStatus.PENDING))) doReturn listOf(
      allocation,
    )

    // Multiple moves to demonstrate takes the latest move for an offender
    whenever(prisonApiClient.getMovementsForPrisonersFromPrison(prison.prisonCode, setOf(allocation.prisonerNumber))) doReturn
      listOf(
        movement(prisonerNumber = allocation.prisonerNumber, movementDate = TimeSource.yesterday()),
        movement(prisonerNumber = allocation.prisonerNumber, fromPrisonCode = prison.prisonCode, movementDate = TimeSource.today()),
      )

    service.deallocateAllocationsDueToExpire()

    verify(waitingListService, never()).removeOpenApplications(any(), any(), any())

    allocation.prisonerStatus isEqualTo PrisonerStatus.PENDING

    verify(activityScheduleRepository, never()).saveAndFlush(schedule)
  }

  @Test
  fun `prisoners due to expire waiting lists are removed`() {
    val prison = rolloutPrison()
    val prisoner: Prisoner = mock {
      on { inOutStatus } doReturn Prisoner.InOutStatus.OUT
      on { prisonerNumber } doReturn "A1234AA"
    }

    whenever(rolloutPrisonService.getRolloutPrisons()) doReturn listOf(prison)
    whenever(rolloutPrisonService.getByPrisonCode(prison.prisonCode)) doReturn prison
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(listOf(prisoner.prisonerNumber))) doReturn listOf(prisoner)
    whenever(waitingListService.fetchOpenApplicationsForPrison(prison.prisonCode)) doReturn listOf(waitingList(prisonerNumber = "A1234AA"))
    whenever(prisonApiClient.getMovementsForPrisonersFromPrison(prison.prisonCode, setOf("A1234AA"))) doReturn
      listOf(movement(prisonerNumber = "A1234AA", movementDate = TimeSource.yesterday()))

    service.deallocateAllocationsDueToExpire()

    verify(waitingListService).removeOpenApplications(
      prison.prisonCode,
      "A1234AA",
      ServiceName.SERVICE_NAME.value,
    )
  }

  @Test
  fun `should send events to queue for each prison`() {
    service.sendAllocationsDueToExpireEvents(Job(123, JobType.DEALLOCATE_EXPIRING))

    verify(jobService).initialiseCounts(123, rolledOutPrisons.count { it.prisonLive })

    verify(jobsSqsService).sendJobEvent(JobEventMessage(123, JobType.DEALLOCATE_EXPIRING, PrisonCodeJobEvent(PENTONVILLE_PRISON_CODE)))
    verify(jobsSqsService).sendJobEvent(JobEventMessage(123, JobType.DEALLOCATE_EXPIRING, PrisonCodeJobEvent(MOORLAND_PRISON_CODE)))
  }

  @Test
  fun `handleEvent - prisoners are deallocated from allocations pending due to expire`() {
    val prison = rolloutPrison()
    val activity = activityEntity(startDate = TimeSource.tomorrow())
    val schedule = activity.schedules().first()
    val allocation = schedule.allocations().first().also { it.prisonerStatus isEqualTo PrisonerStatus.PENDING }
    val prisonerInAtOtherPrison: Prisoner = mock {
      on { inOutStatus } doReturn Prisoner.InOutStatus.IN
      on { prisonerNumber } doReturn allocation.prisonerNumber
      on { prisonId } doReturn prison.prisonCode.plus("-other")
    }

    whenever(rolloutPrisonService.getByPrisonCode(prison.prisonCode)) doReturn prison
    whenever(allocationRepository.findByPrisonCodePrisonerStatus(prison.prisonCode, listOf(PrisonerStatus.PENDING))) doReturn listOf(
      allocation,
    )
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(listOf(prisonerInAtOtherPrison.prisonerNumber))) doReturn listOf(prisonerInAtOtherPrison)

    whenever(prisonApiClient.getMovementsForPrisonersFromPrison(prison.prisonCode, setOf(allocation.prisonerNumber))) doReturn
      listOf(movement(prisonerNumber = allocation.prisonerNumber, movementDate = TimeSource.yesterday()))

    service.handleEvent(123, PENTONVILLE_PRISON_CODE)

    allocation.verifyIsExpired()

    verify(activityScheduleRepository).saveAndFlush(schedule)
    verify(activityScheduleRepository).saveAndFlush(schedule)
  }

  @Test
  fun `handleEvent - should capture failures in monitoring service for any exceptions when expiring`() {
    val exception = RuntimeException("Something went wrong")
    doThrow(exception).whenever(activityScheduleRepository).saveAndFlush(any())
    val prison = rolloutPrison()
    val activity = activityEntity(startDate = TimeSource.tomorrow())
    val schedule = activity.schedules().first()
    val allocation = schedule.allocations().first().also { it.prisonerStatus isEqualTo PrisonerStatus.PENDING }
    val prisoner: Prisoner = mock {
      on { inOutStatus } doReturn Prisoner.InOutStatus.OUT
      on { prisonerNumber } doReturn allocation.prisonerNumber
    }

    whenever(rolloutPrisonService.getByPrisonCode(prison.prisonCode)) doReturn prison
    whenever(allocationRepository.findByPrisonCodePrisonerStatus(prison.prisonCode, listOf(PrisonerStatus.PENDING))) doReturn listOf(allocation)
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(listOf(prisoner.prisonerNumber))) doReturn listOf(prisoner)
    whenever(prisonApiClient.getMovementsForPrisonersFromPrison(prison.prisonCode, setOf(allocation.prisonerNumber))) doReturn listOf(movement(prisonerNumber = allocation.prisonerNumber, movementDate = TimeSource.yesterday()))

    service.handleEvent(123, PENTONVILLE_PRISON_CODE)

    verify(monitoringService).capture("An error occurred deallocating allocations on activity schedule 1", exception)
  }

  @Test
  fun `handleEvent - prisoners are deallocated from allocations pending due to expire when at a different prison`() {
    val prison = rolloutPrison()
    val activity = activityEntity(startDate = TimeSource.tomorrow())
    val schedule = activity.schedules().first()
    val allocation = schedule.allocations().first().also { it.prisonerStatus isEqualTo PrisonerStatus.PENDING }
    val prisonerInAtOtherPrison: Prisoner = mock {
      on { inOutStatus } doReturn Prisoner.InOutStatus.IN
      on { prisonerNumber } doReturn allocation.prisonerNumber
      on { prisonId } doReturn prison.prisonCode.plus("-other")
    }

    whenever(rolloutPrisonService.getByPrisonCode(prison.prisonCode)) doReturn prison
    whenever(allocationRepository.findByPrisonCodePrisonerStatus(prison.prisonCode, listOf(PrisonerStatus.PENDING))) doReturn listOf(
      allocation,
    )
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(listOf(prisonerInAtOtherPrison.prisonerNumber))) doReturn listOf(prisonerInAtOtherPrison)

    whenever(prisonApiClient.getMovementsForPrisonersFromPrison(prison.prisonCode, setOf(allocation.prisonerNumber))) doReturn
      listOf(movement(prisonerNumber = allocation.prisonerNumber, fromPrisonCode = prison.prisonCode, movementDate = TimeSource.yesterday()))

    service.handleEvent(123, PENTONVILLE_PRISON_CODE)

    allocation.verifyIsExpired()

    verify(activityScheduleRepository).saveAndFlush(schedule)
  }

  @Test
  fun `handleEvent - prisoners are are not deallocated from allocations pending as not due to expire`() {
    val prison = rolloutPrison()
    val activity = activityEntity(startDate = TimeSource.tomorrow())
    val schedule = activity.schedules().first()
    val allocation = schedule.allocations().first().also { it.prisonerStatus isEqualTo PrisonerStatus.PENDING }

    whenever(rolloutPrisonService.getByPrisonCode(prison.prisonCode)) doReturn prison
    whenever(allocationRepository.findByPrisonCodePrisonerStatus(prison.prisonCode, listOf(PrisonerStatus.PENDING))) doReturn listOf(
      allocation,
    )

    // Multiple moves to demonstrate takes the latest move for an offender
    whenever(prisonApiClient.getMovementsForPrisonersFromPrison(prison.prisonCode, setOf(allocation.prisonerNumber))) doReturn
      listOf(
        movement(prisonerNumber = allocation.prisonerNumber, movementDate = TimeSource.yesterday()),
        movement(prisonerNumber = allocation.prisonerNumber, fromPrisonCode = prison.prisonCode, movementDate = TimeSource.today()),
      )

    service.handleEvent(123, PENTONVILLE_PRISON_CODE)

    verify(waitingListService, never()).removeOpenApplications(any(), any(), any())

    allocation.prisonerStatus isEqualTo PrisonerStatus.PENDING

    verify(activityScheduleRepository, never()).saveAndFlush(schedule)
  }

  @Test
  fun `handleEvent - prisoners due to expire waiting lists are removed`() {
    val prison = rolloutPrison()
    val prisoner: Prisoner = mock {
      on { inOutStatus } doReturn Prisoner.InOutStatus.OUT
      on { prisonerNumber } doReturn "A1234AA"
    }

    whenever(rolloutPrisonService.getRolloutPrisons()) doReturn listOf(prison)
    whenever(rolloutPrisonService.getByPrisonCode(prison.prisonCode)) doReturn prison
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(listOf(prisoner.prisonerNumber))) doReturn listOf(prisoner)
    whenever(waitingListService.fetchOpenApplicationsForPrison(prison.prisonCode)) doReturn listOf(waitingList(prisonerNumber = "A1234AA"))
    whenever(prisonApiClient.getMovementsForPrisonersFromPrison(prison.prisonCode, setOf("A1234AA"))) doReturn
      listOf(movement(prisonerNumber = "A1234AA", movementDate = TimeSource.yesterday()))

    service.handleEvent(123, PENTONVILLE_PRISON_CODE)

    verify(waitingListService).removeOpenApplications(
      prison.prisonCode,
      "A1234AA",
      ServiceName.SERVICE_NAME.value,
    )
  }

  private fun Allocation.verifyIsExpired() {
    prisonerStatus isEqualTo PrisonerStatus.ENDED
    deallocatedTime!! isCloseTo TimeSource.now()
    deallocatedReason isEqualTo DeallocationReason.TEMPORARILY_RELEASED
    deallocatedBy isEqualTo "Activities Management Service"
  }
}
