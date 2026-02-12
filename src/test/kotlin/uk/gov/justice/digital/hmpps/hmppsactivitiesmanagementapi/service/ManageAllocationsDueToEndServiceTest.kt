package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Job
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.rolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.JobEventMessage
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.JobsSqsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.PrisonCodeJobEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageScheduledInstancesServiceTest.Companion.rolledOutPrisons
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService
import java.time.LocalDate

class ManageAllocationsDueToEndServiceTest {
  private val rolloutPrisonService: RolloutPrisonService = mock {
    on { getRolloutPrisons() } doReturn listOf(rolloutPrison(PENTONVILLE_PRISON_CODE), rolloutPrison(MOORLAND_PRISON_CODE))
  }
  private val activityScheduleRepository: ActivityScheduleRepository = mock()
  private val waitingListService: WaitingListService = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val jobsSqsService: JobsSqsService = mock()
  private val jobService: JobService = mock()
  private val allocationRepository: AllocationRepository = mock()

  private val service =
    ManageAllocationsDueToEndService(
      rolloutPrisonService,
      activityScheduleRepository,
      waitingListService,
      TransactionHandler(),
      outboundEventsService,
      jobsSqsService,
      jobService,
      allocationRepository,
    )

  private val yesterday = LocalDate.now().minusDays(1)
  private val today = yesterday.plusDays(1)
  private val twoDaysAgo = yesterday.minusDays(1)

  @BeforeEach
  fun setUp() {
    whenever(rolloutPrisonService.isActivitiesRolledOutAt(PENTONVILLE_PRISON_CODE)) doReturn true
    whenever(rolloutPrisonService.isActivitiesRolledOutAt(MOORLAND_PRISON_CODE)) doReturn true
  }

  @Test
  fun `should send events to queue for each prison`() {
    service.sendAllocationsDueToEndEvents(Job(123, JobType.DEALLOCATE_ENDING))

    verify(jobService).initialiseCounts(123, rolledOutPrisons.count { it.prisonLive })

    verify(jobsSqsService).sendJobEvent(JobEventMessage(123, JobType.DEALLOCATE_ENDING, PrisonCodeJobEvent(PENTONVILLE_PRISON_CODE)))
    verify(jobsSqsService).sendJobEvent(JobEventMessage(123, JobType.DEALLOCATE_ENDING, PrisonCodeJobEvent(MOORLAND_PRISON_CODE)))
  }

  @Test
  fun `deallocate offenders from activity ending yesterday without pending deallocation`() {
    val prison = rolloutPrison(prisonCode = MOORLAND_PRISON_CODE)
    val schedule = activitySchedule(activityEntity(startDate = twoDaysAgo, endDate = yesterday))
    val allocation = schedule.allocations().first().also { it.verifyIsActive() }

    whenever(activityScheduleRepository.findAllByActivityPrisonCode(prison.prisonCode)) doReturn listOf(schedule)
    whenever(allocationRepository.findAllocationsByActivitySchedule(schedule, true)) doReturn schedule.allocations()

    service.handleEvent(123, MOORLAND_PRISON_CODE)

    allocation.verifyIsEnded()

    verify(activityScheduleRepository).saveAndFlush(schedule)
  }

  @Test
  fun `deallocate offenders from activity yesterday declines pending or approved waiting lists`() {
    val prison = rolloutPrison(prisonCode = MOORLAND_PRISON_CODE)
    val schedule = activitySchedule(activityEntity(startDate = twoDaysAgo, endDate = yesterday))

    whenever(activityScheduleRepository.findAllByActivityPrisonCode(prison.prisonCode)) doReturn listOf(schedule)

    service.handleEvent(123, MOORLAND_PRISON_CODE)

    verify(waitingListService).declinePendingOrApprovedApplications(
      schedule.activity.activityId,
      "Activity ended",
      "Activities Management Service",
    )
  }

  @Test
  fun `deallocate offenders from activity ending yesterday with pending deallocation`() {
    val prison = rolloutPrison(prisonCode = MOORLAND_PRISON_CODE)
    val schedule = activitySchedule(activityEntity(startDate = twoDaysAgo, endDate = today))
    val allocation = schedule.allocations().first().also { it.verifyIsActive() }
    allocation.deallocateOn(today, DeallocationReason.OTHER, "by test").plannedDeallocation!!.plannedDate = yesterday

    whenever(activityScheduleRepository.findAllByActivityPrisonCode(prison.prisonCode)) doReturn listOf(schedule)
    whenever(allocationRepository.findAllocationsByActivitySchedule(schedule, true)) doReturn schedule.allocations()

    service.handleEvent(123, MOORLAND_PRISON_CODE)

    allocation.verifyIsEnded(DeallocationReason.OTHER, "by test")

    verify(activityScheduleRepository).saveAndFlush(schedule)
  }

  @Test
  fun `deallocate offenders from activity with no end date and allocation ends yesterday`() {
    val prison = rolloutPrison(prisonCode = MOORLAND_PRISON_CODE)
    val schedule = activitySchedule(activityEntity(startDate = twoDaysAgo, endDate = null))
    val allocation = schedule.allocations().first().apply { endDate = yesterday }.also { it.verifyIsActive() }

    whenever(activityScheduleRepository.findAllByActivityPrisonCode(prison.prisonCode)) doReturn listOf(schedule)
    whenever(allocationRepository.findAllocationsByActivitySchedule(schedule, true)) doReturn schedule.allocations()

    service.handleEvent(123, MOORLAND_PRISON_CODE)

    allocation.verifyIsEnded()

    verify(activityScheduleRepository).saveAndFlush(schedule)
    verifyNoInteractions(waitingListService)
  }

  @Test
  fun `offenders not deallocated from activity with no end date and allocation does not end yesterday`() {
    val prison = rolloutPrison(prisonCode = MOORLAND_PRISON_CODE)
    val schedule = activitySchedule(activityEntity(startDate = yesterday, endDate = null))
    val allocation = schedule.allocations().first().also { it.verifyIsActive() }

    whenever(activityScheduleRepository.findAllByActivityPrisonCode(prison.prisonCode)) doReturn listOf(schedule)
    whenever(allocationRepository.findAllocationsByActivitySchedule(schedule, true)) doReturn schedule.allocations()

    service.handleEvent(123, MOORLAND_PRISON_CODE)

    allocation.verifyIsActive()

    verify(activityScheduleRepository, never()).saveAndFlush(any())
    verifyNoInteractions(waitingListService)
  }

  @Test
  fun `throws an exception if prison is not rolled out`() {
    assertThrows<IllegalArgumentException> {
      service.handleEvent(123, RISLEY_PRISON_CODE)
    }.message isEqualTo "Supplied prison RSI is not rolled out."

    verifyNoInteractions(activityScheduleRepository)
    verifyNoInteractions(outboundEventsService)
    verifyNoInteractions(jobsSqsService)
    verifyNoInteractions(jobService)
    verifyNoInteractions(waitingListService)
  }

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
}
