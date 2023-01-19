package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityScheduleSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityScheduleSuspension
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.RolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.BankHolidayService
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@ExtendWith(MockitoExtension::class)
class CreateActivitySessionsJobTest {
  private val repository: ActivityRepository = mock()
  private val rolloutPrisonRepository: RolloutPrisonRepository = mock()
  private val bankHolidayService: BankHolidayService = mock()
  private val job = CreateActivitySessionsJob(repository, rolloutPrisonRepository, bankHolidayService, 7L)

  @Captor
  private lateinit var activitySaveCaptor: ArgumentCaptor<Activity>

  @Test
  fun `schedules session instances 7 days in advance for multiple active prisons`() {
    val today = LocalDate.now()
    val toDate = today.plusDays(7)

    whenever(rolloutPrisonRepository.findAll()).thenReturn(rolledOutPrisons)
    whenever(repository.getAllForPrisonBetweenDates("MDI", today, toDate)).thenReturn(moorlandActivities)
    whenever(repository.getAllForPrisonBetweenDates("LEI", today, toDate)).thenReturn(leedsActivities)
    whenever(bankHolidayService.isEnglishBankHoliday(any())).thenReturn(false)

    job.execute()

    // Creates 6 instances for 3 activities in Moorland and 3 in Leeds
    verify(repository, times(6)).save(activitySaveCaptor.capture())
    assertThat(activitySaveCaptor.allValues).hasSize(6)

    with(activitySaveCaptor.allValues) {
      this.forEach { activity ->
        assertThat(activity.prisonCode).isIn(listOf("LEI", "MDI"))
        activity.schedules.forEach { schedule ->
          schedule.instances.forEach { instance ->
            assertThat(instance.sessionDate).isBetween(today, toDate)
          }
        }
      }
    }
  }

  @Test
  fun `can schedule multiple slots for the same day`() {
    val today = LocalDate.now()
    val toDate = today.plusDays(7)

    whenever(rolloutPrisonRepository.findAll()).thenReturn(rolledOutPrisons)
    whenever(repository.getAllForPrisonBetweenDates("MDI", today, toDate)).thenReturn(activityWithMultipleSlots)
    whenever(repository.getAllForPrisonBetweenDates("LEI", today, toDate)).thenReturn(emptyList())
    whenever(bankHolidayService.isEnglishBankHoliday(any())).thenReturn(false)

    job.execute()

    verify(repository, times(1)).save(activitySaveCaptor.capture())

    with(activitySaveCaptor.allValues) {
      this.forEach { activity ->
        assertThat(activity.prisonCode).isEqualTo("MDI")
        assertThat(activity.schedules).hasSize(1)
        activity.schedules.forEach { schedule ->
          assertThat(schedule.slots()).hasSize(2)
          schedule.instances.forEach { instance ->
            assertThat(instance.sessionDate).isBetween(today, toDate)
            assertThat(instance.startTime).isIn(listOf(LocalTime.of(9, 30), LocalTime.of(13, 30)))
          }
        }
      }
    }
  }

  @Test
  fun `does not schedule an activity instance when one already exists for a given date`() {
    val today = LocalDate.now()
    val toDate = today.plusDays(7)

    whenever(rolloutPrisonRepository.findAll()).thenReturn(rolledOutPrisons)
    whenever(repository.getAllForPrisonBetweenDates("MDI", today, toDate)).thenReturn(activityWithExistingInstance)
    whenever(repository.getAllForPrisonBetweenDates("LEI", today, toDate)).thenReturn(emptyList())
    whenever(bankHolidayService.isEnglishBankHoliday(any())).thenReturn(false)

    job.execute()

    verify(repository, times(0)).save(activitySaveCaptor.capture())
    assertThat(activitySaveCaptor.allValues.size).isEqualTo(0)
  }

  @Test
  fun `does not schedule an instance when it has an active suspension`() {
    val today = LocalDate.now()
    val toDate = today.plusDays(7)

    whenever(rolloutPrisonRepository.findAll()).thenReturn(rolledOutPrisons)
    whenever(repository.getAllForPrisonBetweenDates("MDI", today, toDate)).thenReturn(activityWithSuspension)
    whenever(repository.getAllForPrisonBetweenDates("LEI", today, toDate)).thenReturn(emptyList())
    whenever(bankHolidayService.isEnglishBankHoliday(any())).thenReturn(false)

    job.execute()

    verify(repository, times(0)).save(activitySaveCaptor.capture())
    assertThat(activitySaveCaptor.allValues.size).isEqualTo(0)
  }

  @Test
  fun `does not schedule a instance on a bank holiday when activity does not run on bank holidays`() {
    val today = LocalDate.now()
    val toDate = today.plusDays(7)

    whenever(rolloutPrisonRepository.findAll()).thenReturn(rolledOutPrisons)
    whenever(repository.getAllForPrisonBetweenDates("MDI", today, toDate)).thenReturn(activityDoesNotRunOnABankHoliday)
    whenever(repository.getAllForPrisonBetweenDates("LEI", today, toDate)).thenReturn(emptyList())
    whenever(bankHolidayService.isEnglishBankHoliday(today)).thenReturn(true)

    job.execute()

    verify(repository, times(0)).save(activitySaveCaptor.capture())
    assertThat(activitySaveCaptor.allValues.size).isEqualTo(0)
  }

  @Test
  fun `schedules an instance on a bank holiday if activity runs on a bank holiday`() {
    val today = LocalDate.now()
    val toDate = today.plusDays(7)

    whenever(rolloutPrisonRepository.findAll()).thenReturn(rolledOutPrisons)
    whenever(repository.getAllForPrisonBetweenDates("MDI", today, toDate)).thenReturn(activityRunsOnABankHoliday)
    whenever(repository.getAllForPrisonBetweenDates("LEI", today, toDate)).thenReturn(emptyList())
    whenever(bankHolidayService.isEnglishBankHoliday(today)).thenReturn(true)

    job.execute()

    verify(repository, times(1)).save(activitySaveCaptor.capture())
    assertThat(activitySaveCaptor.allValues).hasSize(1)
    with(activitySaveCaptor.allValues) {
      this.forEach { activity ->
        assertThat(activity.prisonCode).isEqualTo("MDI")
        activity.schedules.forEach { schedule ->
          schedule.instances.forEach { instance ->
            assertThat(instance.sessionDate).isBetween(today, toDate)
          }
        }
      }
    }
  }

  companion object {

    val rolledOutPrisons = listOf(
      RolloutPrison(1, "MDI", "Moorland", true, LocalDate.of(2022, 11, 1)),
      RolloutPrison(2, "LEI", "Leeds", true, LocalDate.of(2022, 11, 1)),
      RolloutPrison(3, "XXX", "Other prison", false, null),
    )

    val moorlandActivities = listOf(
      activityEntity(
        activityId = 1L,
        prisonCode = "MDI",
        summary = "A",
        description = "AAA",
        startDate = LocalDate.now().minusDays(1)
      ).apply {
        schedules.clear()
        schedules.add(
          activitySchedule(activity = this, activityScheduleId = 1, monday = true).apply {
            this.instances.clear()
            this.allocations.clear()
          }
        )
      },
      activityEntity(
        activityId = 2L,
        prisonCode = "MDI",
        summary = "B",
        description = "BBB",
        startDate = LocalDate.now().minusDays(1)
      ).apply {
        schedules.clear()
        schedules.add(
          activitySchedule(activity = this, activityScheduleId = 2, monday = false, tuesday = true).apply {
            this.instances.clear()
            this.allocations.clear()
          }
        )
      },
      activityEntity(
        activityId = 3L,
        prisonCode = "MDI",
        summary = "C",
        description = "CCC",
        startDate = LocalDate.now().minusDays(1)
      ).apply {
        schedules.clear()
        schedules.add(
          activitySchedule(activity = this, activityScheduleId = 3, monday = false, wednesday = true).apply {
            this.instances.clear()
            this.allocations.clear()
          }
        )
      },
    )

    val leedsActivities = listOf(
      activityEntity(
        activityId = 4L,
        prisonCode = "LEI",
        summary = "D",
        description = "DDD",
        startDate = LocalDate.now().minusDays(1)
      ).apply {
        schedules.clear()
        schedules.add(
          activitySchedule(activity = this, activityScheduleId = 4, monday = true).apply {
            this.instances.clear()
            this.allocations.clear()
          }
        )
      },
      activityEntity(
        activityId = 5L,
        prisonCode = "LEI",
        summary = "E",
        description = "EEE",
        startDate = LocalDate.now().minusDays(1)
      ).apply {
        schedules.clear()
        schedules.add(
          activitySchedule(activity = this, activityScheduleId = 5, monday = false, tuesday = true).apply {
            this.instances.clear()
            this.allocations.clear()
          }
        )
      },
      activityEntity(
        activityId = 6L,
        prisonCode = "LEI",
        summary = "F",
        description = "FFF",
        startDate = LocalDate.now().minusDays(1)
      ).apply {
        schedules.clear()
        schedules.add(
          activitySchedule(activity = this, activityScheduleId = 6, monday = false, wednesday = true).apply {
            this.instances.clear()
            this.allocations.clear()
          }
        )
      },
    )

    val activityWithExistingInstance = listOf(
      activityEntity(
        activityId = 1L,
        prisonCode = "MDI",
        summary = "Existing",
        description = "Existing instances",
        startDate = LocalDate.now().minusDays(1)
      ).apply {
        schedules.clear()
        schedules.add(
          activitySchedule(
            activity = this,
            activityScheduleId = 1,
            monday = LocalDate.now().dayOfWeek.equals(DayOfWeek.MONDAY),
            tuesday = LocalDate.now().dayOfWeek.equals(DayOfWeek.TUESDAY),
            wednesday = LocalDate.now().dayOfWeek.equals(DayOfWeek.WEDNESDAY),
            thursday = LocalDate.now().dayOfWeek.equals(DayOfWeek.THURSDAY),
            friday = LocalDate.now().dayOfWeek.equals(DayOfWeek.FRIDAY),
            saturday = LocalDate.now().dayOfWeek.equals(DayOfWeek.SATURDAY),
            sunday = LocalDate.now().dayOfWeek.equals(DayOfWeek.SUNDAY),
          ).apply {
            this.instances.clear()
            this.instances.add(
              ScheduledInstance(
                activitySchedule = this,
                sessionDate = LocalDate.now(),
                startTime = LocalTime.now(),
                endTime = LocalTime.now()
              )
            )
          }
        )
      }
    )

    val activityWithSuspension = listOf(
      activityEntity(
        activityId = 1L,
        prisonCode = "MDI",
        summary = "Existing",
        description = "Existing instances",
        startDate = LocalDate.now().minusDays(1)
      ).apply {
        schedules.clear()
        schedules.add(
          activitySchedule(
            activity = this,
            activityScheduleId = 1,
            monday = LocalDate.now().dayOfWeek.equals(DayOfWeek.MONDAY),
            tuesday = LocalDate.now().dayOfWeek.equals(DayOfWeek.TUESDAY),
            wednesday = LocalDate.now().dayOfWeek.equals(DayOfWeek.WEDNESDAY),
            thursday = LocalDate.now().dayOfWeek.equals(DayOfWeek.THURSDAY),
            friday = LocalDate.now().dayOfWeek.equals(DayOfWeek.FRIDAY),
            saturday = LocalDate.now().dayOfWeek.equals(DayOfWeek.SATURDAY),
            sunday = LocalDate.now().dayOfWeek.equals(DayOfWeek.SUNDAY),
          ).apply {
            this.instances.clear()
            this.suspensions.clear()
            this.suspensions.add(
              ActivityScheduleSuspension(
                activitySchedule = this,
                suspendedFrom = LocalDate.now(),
                suspendedUntil = LocalDate.now().plusDays(3)
              )
            )
          }
        )
      }
    )

    val activityDoesNotRunOnABankHoliday = listOf(
      activityEntity(
        activityId = 1L,
        prisonCode = "MDI",
        summary = "BH",
        description = "Not on bank holiday",
        startDate = LocalDate.now().minusDays(1)
      ).apply {
        schedules.clear()
        schedules.add(
          activitySchedule(
            activity = this,
            activityScheduleId = 1,
            monday = LocalDate.now().dayOfWeek.equals(DayOfWeek.MONDAY),
            tuesday = LocalDate.now().dayOfWeek.equals(DayOfWeek.TUESDAY),
            wednesday = LocalDate.now().dayOfWeek.equals(DayOfWeek.WEDNESDAY),
            thursday = LocalDate.now().dayOfWeek.equals(DayOfWeek.THURSDAY),
            friday = LocalDate.now().dayOfWeek.equals(DayOfWeek.FRIDAY),
            saturday = LocalDate.now().dayOfWeek.equals(DayOfWeek.SATURDAY),
            sunday = LocalDate.now().dayOfWeek.equals(DayOfWeek.SUNDAY),
            runsOnBankHolidays = false,
          ).apply {
            this.instances.clear()
            this.suspensions.clear()
          }
        )
      }
    )

    val activityRunsOnABankHoliday = listOf(
      activityEntity(
        activityId = 1L,
        prisonCode = "MDI",
        summary = "BH",
        description = "Not on bank holiday",
        startDate = LocalDate.now().minusDays(1)
      ).apply {
        schedules.clear()
        schedules.add(
          activitySchedule(
            activity = this,
            activityScheduleId = 1,
            monday = LocalDate.now().dayOfWeek.equals(DayOfWeek.MONDAY),
            tuesday = LocalDate.now().dayOfWeek.equals(DayOfWeek.TUESDAY),
            wednesday = LocalDate.now().dayOfWeek.equals(DayOfWeek.WEDNESDAY),
            thursday = LocalDate.now().dayOfWeek.equals(DayOfWeek.THURSDAY),
            friday = LocalDate.now().dayOfWeek.equals(DayOfWeek.FRIDAY),
            saturday = LocalDate.now().dayOfWeek.equals(DayOfWeek.SATURDAY),
            sunday = LocalDate.now().dayOfWeek.equals(DayOfWeek.SUNDAY),
            runsOnBankHolidays = true,
          ).apply {
            this.instances.clear()
            this.suspensions.clear()
          }
        )
      }
    )

    val activityWithMultipleSlots = listOf(
      activityEntity(
        activityId = 1L,
        prisonCode = "MDI",
        summary = "Multiple",
        description = "Slots",
        startDate = LocalDate.now().minusDays(1),
        noSchedules = true
      ).apply {
        schedules.add(
          activitySchedule(
            activity = this,
            activityScheduleId = 1,
            monday = LocalDate.now().dayOfWeek.equals(DayOfWeek.MONDAY),
            tuesday = LocalDate.now().dayOfWeek.equals(DayOfWeek.TUESDAY),
            wednesday = LocalDate.now().dayOfWeek.equals(DayOfWeek.WEDNESDAY),
            thursday = LocalDate.now().dayOfWeek.equals(DayOfWeek.THURSDAY),
            friday = LocalDate.now().dayOfWeek.equals(DayOfWeek.FRIDAY),
            saturday = LocalDate.now().dayOfWeek.equals(DayOfWeek.SATURDAY),
            sunday = LocalDate.now().dayOfWeek.equals(DayOfWeek.SUNDAY),
            noSlots = true
          ).apply {
            this.instances.clear()
            this.suspensions.clear()
            this.addSlot(
              ActivityScheduleSlot(
                activityScheduleSlotId = 1,
                activitySchedule = this,
                startTime = LocalTime.of(9, 30),
                endTime = LocalTime.of(11, 30),
                mondayFlag = LocalDate.now().dayOfWeek.equals(DayOfWeek.MONDAY),
                tuesdayFlag = LocalDate.now().dayOfWeek.equals(DayOfWeek.TUESDAY),
                wednesdayFlag = LocalDate.now().dayOfWeek.equals(DayOfWeek.WEDNESDAY),
                thursdayFlag = LocalDate.now().dayOfWeek.equals(DayOfWeek.THURSDAY),
                fridayFlag = LocalDate.now().dayOfWeek.equals(DayOfWeek.FRIDAY),
                saturdayFlag = LocalDate.now().dayOfWeek.equals(DayOfWeek.SATURDAY),
                sundayFlag = LocalDate.now().dayOfWeek.equals(DayOfWeek.SUNDAY),
              )
            )
            this.addSlot(
              ActivityScheduleSlot(
                activityScheduleSlotId = 2,
                activitySchedule = this,
                startTime = LocalTime.of(13, 30),
                endTime = LocalTime.of(15, 30),
                mondayFlag = LocalDate.now().dayOfWeek.equals(DayOfWeek.MONDAY),
                tuesdayFlag = LocalDate.now().dayOfWeek.equals(DayOfWeek.TUESDAY),
                wednesdayFlag = LocalDate.now().dayOfWeek.equals(DayOfWeek.WEDNESDAY),
                thursdayFlag = LocalDate.now().dayOfWeek.equals(DayOfWeek.THURSDAY),
                fridayFlag = LocalDate.now().dayOfWeek.equals(DayOfWeek.FRIDAY),
                saturdayFlag = LocalDate.now().dayOfWeek.equals(DayOfWeek.SATURDAY),
                sundayFlag = LocalDate.now().dayOfWeek.equals(DayOfWeek.SUNDAY),
              ),
            )
          }
        )
      }
    )
  }
}
