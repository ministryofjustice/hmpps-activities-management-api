package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.firstValue
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityScheduleSuspension
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@ExtendWith(MockitoExtension::class)
class CreateActivitySessionsJobTest {
  private val repository: ActivityRepository = mock()
  private val job = CreateActivitySessionsJob(repository, 60L)

  @Captor
  private lateinit var activitySaveCaptor: ArgumentCaptor<Activity>

  @Test
  fun `schedules session instance 60 days in advance`() {
    val expectedDate = LocalDate.now().plusDays(60)

    val activity = activityEntity().apply {
      this.schedules.clear()
      this.schedules.add(
        activitySchedule(
          this,
          LocalDate.now().atStartOfDay(),
          monday = expectedDate.dayOfWeek.equals(DayOfWeek.MONDAY),
          tuesday = expectedDate.dayOfWeek.equals(DayOfWeek.TUESDAY),
          wednesday = expectedDate.dayOfWeek.equals(DayOfWeek.WEDNESDAY),
          thursday = expectedDate.dayOfWeek.equals(DayOfWeek.THURSDAY),
          friday = expectedDate.dayOfWeek.equals(DayOfWeek.FRIDAY),
          saturday = expectedDate.dayOfWeek.equals(DayOfWeek.SATURDAY),
          sunday = expectedDate.dayOfWeek.equals(DayOfWeek.SUNDAY),
        )
      )
    }

    whenever(repository.getAllForDate(expectedDate)).thenReturn(listOf(activity))

    job.execute()

    verify(repository, times(1)).save(activitySaveCaptor.capture())

    val sessionCreatedOnExpectedDay = activitySaveCaptor.firstValue.schedules.first().instances.find { it.sessionDate == expectedDate }

    assertThat(sessionCreatedOnExpectedDay).isNotNull
  }

  @Test
  fun `does not schedule session instance if instance already exists`() {
    val expectedDate = LocalDate.now().plusDays(60)

    val activity = activityEntity().apply {
      val schedule = activitySchedule(
        this,
        monday = expectedDate.dayOfWeek.equals(DayOfWeek.MONDAY),
        tuesday = expectedDate.dayOfWeek.equals(DayOfWeek.TUESDAY),
        wednesday = expectedDate.dayOfWeek.equals(DayOfWeek.WEDNESDAY),
        thursday = expectedDate.dayOfWeek.equals(DayOfWeek.THURSDAY),
        friday = expectedDate.dayOfWeek.equals(DayOfWeek.FRIDAY),
        saturday = expectedDate.dayOfWeek.equals(DayOfWeek.SATURDAY),
        sunday = expectedDate.dayOfWeek.equals(DayOfWeek.SUNDAY),
      ).apply {
        this.instances.clear()
        this.instances.add(ScheduledInstance(activitySchedule = this, sessionDate = expectedDate, startTime = LocalTime.now(), endTime = LocalTime.now()))
      }

      this.schedules.clear()
      this.schedules.add(schedule)
    }

    whenever(repository.getAllForDate(expectedDate)).thenReturn(listOf(activity))

    job.execute()

    verify(repository, times(1)).save(activitySaveCaptor.capture())

    val sessionCreatedOnExpectedDay = activitySaveCaptor.firstValue.schedules.first().instances.find { it.sessionDate == expectedDate }

    assertThat(activitySaveCaptor.firstValue.schedules.first().instances.size).isEqualTo(1)
    assertThat(sessionCreatedOnExpectedDay).isNotNull
  }

  @Test
  fun `does not schedule session instance if schedule is suspended`() {
    val expectedDate = LocalDate.now().plusDays(60)

    val activity = activityEntity().apply {
      val schedule = activitySchedule(
        this,
        monday = expectedDate.dayOfWeek.equals(DayOfWeek.MONDAY),
        tuesday = expectedDate.dayOfWeek.equals(DayOfWeek.TUESDAY),
        wednesday = expectedDate.dayOfWeek.equals(DayOfWeek.WEDNESDAY),
        thursday = expectedDate.dayOfWeek.equals(DayOfWeek.THURSDAY),
        friday = expectedDate.dayOfWeek.equals(DayOfWeek.FRIDAY),
        saturday = expectedDate.dayOfWeek.equals(DayOfWeek.SATURDAY),
        sunday = expectedDate.dayOfWeek.equals(DayOfWeek.SUNDAY),
      ).apply {
        this.suspensions.clear()
        this.suspensions.add(ActivityScheduleSuspension(activitySchedule = this, suspendedFrom = LocalDate.now()))
      }

      this.schedules.clear()
      this.schedules.add(schedule)
    }

    whenever(repository.getAllForDate(expectedDate)).thenReturn(listOf(activity))

    job.execute()

    verify(repository, times(1)).save(activitySaveCaptor.capture())

    val sessionCreatedOnExpectedDay = activitySaveCaptor.firstValue.schedules.first().instances.find { it.sessionDate == expectedDate }

    assertThat(sessionCreatedOnExpectedDay).isNull()
  }
}
