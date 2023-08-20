package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.InmateDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.SentenceCalcDates
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.rolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentOccurrenceAllocationService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.WaitingListService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OffenderReleasedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.ReleaseInformation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.offenderReleasedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.offenderTemporaryReleasedEvent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class OffenderReleasedEventHandlerTest {
  private val rolloutPrisonRepository: RolloutPrisonRepository = mock()
  private val allocationRepository: AllocationRepository = mock()
  private val prisonApiClient: PrisonApiApplicationClient = mock()
  private val appointmentOccurrenceAllocationService: AppointmentOccurrenceAllocationService = mock()
  private val attendanceRepository: AttendanceRepository = mock()
  private val waitingListService: WaitingListService = mock()

  private val handler = OffenderReleasedEventHandler(
    rolloutPrisonRepository,
    allocationRepository,
    appointmentOccurrenceAllocationService,
    prisonApiClient,
    attendanceRepository,
    waitingListService,
  )

  private val prisoner: InmateDetail = mock {
    on { status } doReturn "INACTIVE OUT"
  }

  @BeforeEach
  fun beforeTests() {
    reset(rolloutPrisonRepository, allocationRepository, prisonApiClient)
    rolloutPrisonRepository.stub {
      on { findByCode(moorlandPrisonCode) } doReturn
        rolloutPrison().copy(
          activitiesToBeRolledOut = true,
          activitiesRolloutDate = LocalDate.now().plusDays(-1),
        )
    }
  }

  @Test
  fun `inbound released event is not handled for an inactive prison`() {
    val inboundEvent = offenderReleasedEvent(moorlandPrisonCode, "123456")
    reset(rolloutPrisonRepository)
    rolloutPrisonRepository.stub {
      on { findByCode(moorlandPrisonCode) } doReturn
        rolloutPrison().copy(
          activitiesToBeRolledOut = false,
          activitiesRolloutDate = null,
        )
    }

    val outcome = handler.handle(inboundEvent)

    assertThat(outcome.isSuccess()).isTrue
    verify(rolloutPrisonRepository).findByCode(moorlandPrisonCode)
    verifyNoInteractions(allocationRepository)
    verifyNoInteractions(waitingListService)
  }

  @Test
  fun `inbound release event is not processed when no matching prison is found`() {
    reset(rolloutPrisonRepository)
    val inboundEvent = offenderReleasedEvent(moorlandPrisonCode, "123456")
    rolloutPrisonRepository.stub { on { findByCode(moorlandPrisonCode) } doReturn null }

    val outcome = handler.handle(inboundEvent)

    assertThat(outcome.isSuccess()).isTrue
    verify(rolloutPrisonRepository).findByCode(moorlandPrisonCode)
    verifyNoInteractions(allocationRepository)
    verifyNoInteractions(waitingListService)
  }

  @Test
  fun `active allocations are unchanged on temporary release of prisoner`() {
    val previouslyActiveAllocations = listOf(
      allocation().copy(allocationId = 1, prisonerNumber = "123456"),
      allocation().copy(allocationId = 2, prisonerNumber = "123456"),
      allocation().copy(allocationId = 3, prisonerNumber = "123456"),
    ).onEach {
      assertThat(it.status(PrisonerStatus.ACTIVE)).isTrue
      assertThat(it.suspendedBy).isNull()
      assertThat(it.suspendedReason).isNull()
      assertThat(it.suspendedTime).isNull()
    }

    whenever(allocationRepository.findByPrisonCodeAndPrisonerNumber(moorlandPrisonCode, "123456"))
      .doReturn(previouslyActiveAllocations)

    val outcome = handler.handle(offenderTemporaryReleasedEvent(moorlandPrisonCode, "123456"))

    assertThat(outcome.isSuccess()).isTrue

    previouslyActiveAllocations.forEach { assertThat(it.status(PrisonerStatus.ACTIVE)).isTrue }

    verify(allocationRepository, never()).saveAllAndFlush(any<List<Allocation>>())
    verifyNoInteractions(waitingListService)
  }

  @Test
  fun `un-ended allocations are ended on release from remand`() {
    val previouslyActiveAllocations = listOf(
      allocation().copy(allocationId = 1, prisonerNumber = "123456"),
      allocation().copy(allocationId = 2, prisonerNumber = "123456"),
      allocation().copy(allocationId = 3, prisonerNumber = "123456"),
    ).onEach {
      assertThat(it.status(PrisonerStatus.ACTIVE)).isTrue
      assertThat(it.deallocatedBy).isNull()
      assertThat(it.deallocatedReason).isNull()
      assertThat(it.deallocatedTime).isNull()
    }

    val sentenceCalcDatesNoReleaseDateForRemand: SentenceCalcDates = mock { on { releaseDate } doReturn null }

    prisoner.stub {
      on { sentenceDetail } doReturn sentenceCalcDatesNoReleaseDateForRemand
    }

    whenever(prisonApiClient.getPrisonerDetails("123456", fullInfo = true, extraInfo = true))
      .doReturn(Mono.just(prisoner))

    whenever(allocationRepository.findByPrisonCodeAndPrisonerNumber(moorlandPrisonCode, "123456"))
      .doReturn(previouslyActiveAllocations)

    val outcome = handler.handle(offenderReleasedEvent(moorlandPrisonCode, "123456"))

    assertThat(outcome.isSuccess()).isTrue

    previouslyActiveAllocations.forEach {
      assertThat(it.status(PrisonerStatus.ENDED)).isTrue
      assertThat(it.deallocatedBy).isEqualTo("Activities Management Service")
      assertThat(it.deallocatedReason).isEqualTo(DeallocationReason.RELEASED)
      assertThat(it.deallocatedTime)
        .isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
    }

    verify(waitingListService).declinePendingOrApprovedApplications(moorlandPrisonCode, "123456", "Released", "Activities Management Service")
  }

  @Test
  fun `un-ended allocations are ended on release from custodial`() {
    val previouslyActiveAllocations = listOf(
      allocation().copy(allocationId = 1, prisonerNumber = "123456"),
      allocation().copy(allocationId = 2, prisonerNumber = "123456"),
      allocation().copy(allocationId = 3, prisonerNumber = "123456"),
    ).onEach {
      assertThat(it.status(PrisonerStatus.ACTIVE)).isTrue
      assertThat(it.deallocatedBy).isNull()
      assertThat(it.deallocatedReason).isNull()
      assertThat(it.deallocatedTime).isNull()
    }

    val sentenceCalcDatesReleaseDateTodayForCustodialSentence: SentenceCalcDates =
      mock { on { releaseDate } doReturn LocalDate.now() }

    prisoner.stub {
      on { sentenceDetail } doReturn sentenceCalcDatesReleaseDateTodayForCustodialSentence
    }

    whenever(prisonApiClient.getPrisonerDetails("123456", fullInfo = true, extraInfo = true))
      .doReturn(Mono.just(prisoner))

    whenever(allocationRepository.findByPrisonCodeAndPrisonerNumber(moorlandPrisonCode, "123456"))
      .doReturn(previouslyActiveAllocations)

    val outcome = handler.handle(offenderReleasedEvent(moorlandPrisonCode, "123456"))

    assertThat(outcome.isSuccess()).isTrue

    previouslyActiveAllocations.forEach {
      assertThat(it.status(PrisonerStatus.ENDED)).isTrue
      assertThat(it.deallocatedBy).isEqualTo("Activities Management Service")
      assertThat(it.deallocatedReason).isEqualTo(DeallocationReason.RELEASED)
      assertThat(it.deallocatedTime)
        .isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
    }

    verify(waitingListService).declinePendingOrApprovedApplications(moorlandPrisonCode, "123456", "Released", "Activities Management Service")
  }

  @Test
  fun `only un-ended allocations are ended on release of prisoner`() {
    val previouslyEndedAllocation = allocation().copy(allocationId = 1, prisonerNumber = "123456")
      .also { it.deallocateNowWithReason(DeallocationReason.ENDED) }
    val previouslySuspendedAllocation = allocation().copy(allocationId = 2, prisonerNumber = "123456")
      .also { it.autoSuspend(LocalDateTime.now(), "reason") }
    val previouslyActiveAllocation = allocation().copy(allocationId = 3, prisonerNumber = "123456")

    val allocations = listOf(previouslyEndedAllocation, previouslySuspendedAllocation, previouslyActiveAllocation)

    whenever(prisonApiClient.getPrisonerDetails("123456", fullInfo = true, extraInfo = true))
      .doReturn(Mono.just(prisoner))

    whenever(allocationRepository.findByPrisonCodeAndPrisonerNumber(moorlandPrisonCode, "123456"))
      .doReturn(allocations)

    val outcome = handler.handle(offenderReleasedEvent(moorlandPrisonCode, "123456"))

    assertThat(outcome.isSuccess()).isTrue

    with(previouslyEndedAllocation) {
      assertThat(status(PrisonerStatus.ENDED)).isTrue
      assertThat(deallocatedTime).isCloseTo(TimeSource.now(), within(2, ChronoUnit.SECONDS))
    }

    assertThat(previouslySuspendedAllocation.status(PrisonerStatus.ENDED)).isTrue
    assertThat(previouslyActiveAllocation.status(PrisonerStatus.ENDED)).isTrue

    verify(waitingListService).declinePendingOrApprovedApplications(moorlandPrisonCode, "123456", "Released", "Activities Management Service")
  }

  @Test
  fun `allocation is unmodified for unknown release event`() {
    val allocation = allocation().copy(allocationId = 1, prisonerNumber = "123456")
      .also { assertThat(it.status(PrisonerStatus.ACTIVE)).isTrue() }

    whenever(allocationRepository.findByPrisonCodeAndPrisonerNumber(moorlandPrisonCode, "123456")).doReturn(
      listOf(
        allocation,
      ),
    )

    val outcome = handler.handle(
      OffenderReleasedEvent(
        ReleaseInformation(
          nomsNumber = "12345",
          reason = "UNKNOWN",
          prisonId = moorlandPrisonCode,
        ),
      ),
    )

    assertThat(outcome.isSuccess()).isFalse
    assertThat(allocation.status(PrisonerStatus.ACTIVE)).isTrue
    verifyNoInteractions(allocationRepository)
    verifyNoInteractions(waitingListService)
  }

  @Test
  fun `all future allocations are cancelled for a released event`() {
    val prisonerNumber = "12345"
    whenever(prisonApiClient.getPrisonerDetails(prisonerNumber, fullInfo = true, extraInfo = true))
      .doReturn(Mono.just(prisoner))

    val outcome = handler.handle(
      OffenderReleasedEvent(
        ReleaseInformation(
          nomsNumber = prisonerNumber,
          reason = "RELEASED",
          prisonId = moorlandPrisonCode,
        ),
      ),
    )

    assertThat(outcome.isSuccess()).isTrue()
    verify(appointmentOccurrenceAllocationService).cancelFutureOffenderAppointments(moorlandPrisonCode, "12345")
  }

  @Test
  fun `only future attendances are removed on release`() {
    prisonApiClient.stub {
      on {
        prisonApiClient.getPrisonerDetails(
          "123456",
          fullInfo = true,
          extraInfo = true,
        )
      } doReturn Mono.just(prisoner)
    }

    listOf(allocation().copy(allocationId = 1, prisonerNumber = "123456")).also {
      whenever(allocationRepository.findByPrisonCodeAndPrisonerNumber(moorlandPrisonCode, "123456")) doReturn it
    }

    val todaysHistoricScheduledInstance = scheduledInstanceOn(TimeSource.today(), LocalTime.now().minusMinutes(1))
    val todaysHistoricAttendance = attendanceFor(todaysHistoricScheduledInstance)

    val todaysFuturescheduledInstance = scheduledInstanceOn(TimeSource.today(), LocalTime.now().plusMinutes(1))
    val todaysFutureAttendance = attendanceFor(todaysFuturescheduledInstance)

    val tomorrowsScheduledInstance = scheduledInstanceOn(TimeSource.tomorrow(), LocalTime.now().plusMinutes(1))
    val tomorrowsFutureAttendance = attendanceFor(tomorrowsScheduledInstance)

    whenever(
      attendanceRepository.findAttendancesOnOrAfterDateForPrisoner(
        prisonCode = moorlandPrisonCode,
        sessionDate = LocalDate.now(),
        prisonerNumber = "123456",
      ),
    ) doReturn listOf(todaysHistoricAttendance, todaysFutureAttendance, tomorrowsFutureAttendance)

    handler.handle(
      OffenderReleasedEvent(
        ReleaseInformation(
          nomsNumber = "123456",
          reason = "RELEASED",
          prisonId = moorlandPrisonCode,
        ),
      ),
    )

    verify(todaysHistoricScheduledInstance, never()).remove(todaysHistoricAttendance)
    verify(todaysFuturescheduledInstance).remove(todaysFutureAttendance)
    verify(tomorrowsScheduledInstance).remove(tomorrowsFutureAttendance)
  }

  private fun scheduledInstanceOn(date: LocalDate, time: LocalTime): ScheduledInstance = mock {
    on { sessionDate } doReturn date
    on { startTime } doReturn time
  }

  private fun attendanceFor(instance: ScheduledInstance): Attendance = mock {
    on { scheduledInstance } doReturn instance
    on { editable() } doReturn true
  }
}
