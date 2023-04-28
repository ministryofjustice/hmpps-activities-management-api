package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.rolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class OffenderDeallocationServiceTest {

  private val rolloutPrisonRepo: RolloutPrisonRepository = mock()
  private val activityRepo: ActivityRepository = mock()
  private val activityScheduleRepo: ActivityScheduleRepository = mock()
  private val service = OffenderDeallocationService(rolloutPrisonRepo, activityRepo, activityScheduleRepo)
  private val yesterday = LocalDate.now().minusDays(1)
  private val today = yesterday.plusDays(1)

  @Test
  fun `deallocate offenders from activity ending today`() {
    val prison = rolloutPrison()
    val activity = activityEntity(startDate = yesterday, endDate = today)
    val schedule = activity.schedules().first()
    val allocation = schedule.allocations().first()

    with(allocation) {
      assertThat(status(PrisonerStatus.ACTIVE)).isTrue
      assertThat(deallocatedTime).isNull()
      assertThat(deallocatedReason).isNull()
      assertThat(deallocatedBy).isNull()
    }

    whenever(rolloutPrisonRepo.findAll()).thenReturn(listOf(prison))
    whenever(activityRepo.getAllForPrisonAndDate(prison.code, LocalDate.now())).thenReturn(listOf(activity))

    service.deallocateOffendersWhenEndDatesReached()

    with(allocation) {
      assertThat(status(PrisonerStatus.ENDED)).isTrue
      assertThat(deallocatedTime).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
      assertThat(deallocatedReason).isNotNull
      assertThat(deallocatedBy).isNotNull
    }

    verify(activityScheduleRepo).saveAndFlush(schedule)
  }

  @Test
  fun `deallocate offenders from activity with no end date and allocation ends today`() {
    val prison = rolloutPrison()
    val activity = activityEntity(startDate = yesterday, endDate = null)
    val schedule = activity.schedules().first()
    val allocation = schedule.allocations().first().apply { endDate = today }

    with(allocation) {
      assertThat(status(PrisonerStatus.ACTIVE)).isTrue
      assertThat(deallocatedTime).isNull()
      assertThat(deallocatedReason).isNull()
      assertThat(deallocatedBy).isNull()
    }

    whenever(rolloutPrisonRepo.findAll()).thenReturn(listOf(prison))
    whenever(activityRepo.getAllForPrisonAndDate(prison.code, LocalDate.now())).thenReturn(listOf(activity))

    service.deallocateOffendersWhenEndDatesReached()

    with(allocation) {
      assertThat(status(PrisonerStatus.ENDED)).isTrue
      assertThat(deallocatedTime).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
      assertThat(deallocatedReason).isNotNull
      assertThat(deallocatedBy).isNotNull
    }

    verify(activityScheduleRepo).saveAndFlush(schedule)
  }

  @Test
  fun `offenders not deallocated from activity with no end date and allocation does not end today`() {
    val prison = rolloutPrison()
    val activity = activityEntity(startDate = yesterday, endDate = null)
    val schedule = activity.schedules().first()
    val allocation = schedule.allocations().first()

    with(allocation) {
      assertThat(status(PrisonerStatus.ACTIVE)).isTrue
      assertThat(deallocatedTime).isNull()
      assertThat(deallocatedReason).isNull()
      assertThat(deallocatedBy).isNull()
    }

    whenever(rolloutPrisonRepo.findAll()).thenReturn(listOf(prison))
    whenever(activityRepo.getAllForPrisonAndDate(prison.code, LocalDate.now())).thenReturn(listOf(activity))

    service.deallocateOffendersWhenEndDatesReached()

    with(allocation) {
      assertThat(status(PrisonerStatus.ACTIVE)).isTrue
      assertThat(deallocatedTime).isNull()
      assertThat(deallocatedReason).isNull()
      assertThat(deallocatedBy).isNull()
    }

    verify(activityScheduleRepo, never()).saveAndFlush(any())
  }
}
