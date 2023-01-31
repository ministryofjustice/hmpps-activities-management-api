package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.firstValue
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityScheduleSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityScheduleSuspension
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.RolloutPrison
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
  private val rolloutPrisonRepository: RolloutPrisonRepository = mock { on { findAll() } doReturn (rolledOutPrisons) }
  private val bankHolidayService: BankHolidayService = mock { on { isEnglishBankHoliday(any()) } doReturn (false) }

  private val job = CreateActivitySessionsJob(repository, rolloutPrisonRepository, bankHolidayService, 7L)
  private val today = LocalDate.now()
  private val weekFromToday = today.plusWeeks(1)

  @Captor
  private lateinit var activitySaveCaptor: ArgumentCaptor<Activity>

  @Test
  fun `schedules session instances 7 days in advance for multiple active prisons`() {
    whenever(repository.getAllForPrisonBetweenDates("MDI", today, weekFromToday)).thenReturn(moorlandActivities)
    whenever(repository.getAllForPrisonBetweenDates("LEI", today, weekFromToday)).thenReturn(leedsActivities)

    job.execute()

    // Creates 6 instances for 3 activities in Moorland and 3 in Leeds
    verify(repository, times(6)).save(activitySaveCaptor.capture())

    activitySaveCaptor.savedActivities(6).forEach { activity ->
      assertThat(activity.prisonCode).isIn(listOf("LEI", "MDI"))
      activity.schedules().forEach { schedule ->
        schedule.instances().forEach { instance ->
          assertThat(instance.sessionDate).isBetween(today, weekFromToday)
        }
      }
    }
  }

  @Test
  fun `can schedule multiple slots for the same day`() {
    whenever(repository.getAllForPrisonBetweenDates("MDI", today, weekFromToday)).thenReturn(activityWithMultipleSlots)
    whenever(repository.getAllForPrisonBetweenDates("LEI", today, weekFromToday)).thenReturn(emptyList())

    job.execute()

    verify(repository).save(activitySaveCaptor.capture())

    with(activitySaveCaptor.savedActivityAtPrison("MDI")) {
      assertThat(schedules()).hasSize(1)

      with(schedules().first()) {
        assertThat(slots()).hasSize(2)
        instances().forEach { instance ->
          assertThat(instance.sessionDate).isBetween(today, weekFromToday)
          assertThat(instance.startTime).isIn(listOf(LocalTime.of(9, 30), LocalTime.of(13, 30)))
        }
      }
    }
  }

  @Test
  fun `does not schedule an activity instance when one already exists for a given date`() {
    whenever(repository.getAllForPrisonBetweenDates("MDI", today, weekFromToday)).thenReturn(activityWithExistingInstance)
    whenever(repository.getAllForPrisonBetweenDates("LEI", today, weekFromToday)).thenReturn(emptyList())

    job.execute()

    verify(repository, never()).save(any())
  }

  @Test
  fun `does schedule an instance when it has an active suspension`() {
    whenever(repository.getAllForPrisonBetweenDates("MDI", today, weekFromToday)).thenReturn(activityWithSuspension)
    whenever(repository.getAllForPrisonBetweenDates("LEI", today, weekFromToday)).thenReturn(emptyList())

    job.execute()

    verify(repository).save(activitySaveCaptor.capture())

    with(activitySaveCaptor.savedActivityAtPrison("MDI")) {
      assertThat(schedules().first().isSuspendedOn(today)).isTrue
      assertThat(schedules().first().instances()).hasSize(1)
    }
  }

  @Test
  fun `does not schedule a instance on a bank holiday when activity does not run on bank holidays`() {
    whenever(repository.getAllForPrisonBetweenDates("MDI", today, weekFromToday)).thenReturn(activityDoesNotRunOnABankHoliday)
    whenever(repository.getAllForPrisonBetweenDates("LEI", today, weekFromToday)).thenReturn(emptyList())
    whenever(bankHolidayService.isEnglishBankHoliday(today)).thenReturn(true)

    job.execute()

    verify(repository, never()).save(any())
  }

  @Test
  fun `schedules an instance on a bank holiday if activity runs on a bank holiday`() {
    whenever(repository.getAllForPrisonBetweenDates("MDI", today, weekFromToday)).thenReturn(activityRunsOnABankHoliday)
    whenever(repository.getAllForPrisonBetweenDates("LEI", today, weekFromToday)).thenReturn(emptyList())
    whenever(bankHolidayService.isEnglishBankHoliday(today)).thenReturn(true)

    job.execute()

    verify(repository).save(activitySaveCaptor.capture())

    with(activitySaveCaptor.savedActivityAtPrison("MDI")) {
      schedules().forEach { schedule ->
        schedule.instances().forEach { instance ->
          assertThat(instance.sessionDate).isBetween(today, weekFromToday)
        }
      }
    }
  }

  private fun ArgumentCaptor<Activity>.savedActivityAtPrison(expectedPrisonCode: String) =
    this.firstValue.also {
      assertThat(this.allValues).hasSize(1)
      assertThat(this.firstValue.prisonCode).isEqualTo(expectedPrisonCode)
    }

  private fun ArgumentCaptor<Activity>.savedActivities(expectedNumberOfSavedActivities: Int) =
    this.allValues.toList().also { assertThat(it).hasSize(expectedNumberOfSavedActivities) }

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
        startDate = LocalDate.now().minusDays(1),
        noSchedules = true
      ).apply {
        this.addSchedule(
          activitySchedule(
            activity = this,
            activityScheduleId = 1,
            monday = true,
            noAllocations = true,
            noInstances = true
          )
        )
      },
      activityEntity(
        activityId = 2L,
        prisonCode = "MDI",
        summary = "B",
        description = "BBB",
        startDate = LocalDate.now().minusDays(1),
        noSchedules = true
      ).apply {
        this.addSchedule(
          activitySchedule(
            activity = this,
            activityScheduleId = 2,
            monday = false,
            tuesday = true,
            noAllocations = true,
            noInstances = true
          )
        )
      },
      activityEntity(
        activityId = 3L,
        prisonCode = "MDI",
        summary = "C",
        description = "CCC",
        startDate = LocalDate.now().minusDays(1),
        noSchedules = true
      ).apply {
        this.addSchedule(
          activitySchedule(
            activity = this,
            activityScheduleId = 3,
            monday = false,
            wednesday = true,
            noAllocations = true,
            noInstances = true
          )
        )
      },
    )

    val leedsActivities = listOf(
      activityEntity(
        activityId = 4L,
        prisonCode = "LEI",
        summary = "D",
        description = "DDD",
        startDate = LocalDate.now().minusDays(1),
        noSchedules = true
      ).apply {
        this.addSchedule(
          activitySchedule(
            activity = this,
            activityScheduleId = 4,
            monday = true,
            noAllocations = true,
            noInstances = true
          )
        )
      },
      activityEntity(
        activityId = 5L,
        prisonCode = "LEI",
        summary = "E",
        description = "EEE",
        startDate = LocalDate.now().minusDays(1),
        noSchedules = true
      ).apply {
        this.addSchedule(
          activitySchedule(
            activity = this,
            activityScheduleId = 5,
            monday = false,
            tuesday = true,
            noAllocations = true,
            noInstances = true
          )
        )
      },
      activityEntity(
        activityId = 6L,
        prisonCode = "LEI",
        summary = "F",
        description = "FFF",
        startDate = LocalDate.now().minusDays(1),
        noSchedules = true
      ).apply {
        this.addSchedule(
          activitySchedule(
            activity = this,
            activityScheduleId = 6,
            monday = false,
            wednesday = true,
            noAllocations = true,
            noInstances = true
          )
        )
      },
    )

    val activityWithExistingInstance = listOf(
      activityEntity(
        activityId = 1L,
        prisonCode = "MDI",
        summary = "Existing",
        description = "Existing instances",
        startDate = LocalDate.now().minusDays(1),
        noSchedules = true
      ).apply {
        this.addSchedule(
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
            noInstances = true
          ).apply {
            this.addInstance(
              sessionDate = LocalDate.now(),
              slot = this.slots().first()
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
        startDate = LocalDate.now().minusDays(1),
        noSchedules = true
      ).apply {
        this.addSchedule(
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
            noInstances = true
          ).apply {
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
        startDate = LocalDate.now().minusDays(1),
        noSchedules = true
      ).apply {
        this.addSchedule(
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
            noInstances = true
          ).apply {
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
        startDate = LocalDate.now().minusDays(1),
        noSchedules = true
      ).apply {
        this.addSchedule(
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
            noInstances = true
          ).apply {
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
        this.addSchedule(
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
            noSlots = true,
          ).apply {
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
