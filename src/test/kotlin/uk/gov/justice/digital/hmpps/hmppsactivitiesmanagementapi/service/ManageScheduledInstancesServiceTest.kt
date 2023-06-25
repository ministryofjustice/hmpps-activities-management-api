package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Captor
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityScheduleSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityScheduleSuspension
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.RolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.projections.ActivityBasic
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.JobDefinition
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.SafeJobRunner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.JobRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ManageScheduledInstancesServiceTest {
  private val activityRepository: ActivityRepository = mock()
  private val activityScheduleRepository: ActivityScheduleRepository = mock()
  private val rolloutPrisonRepository: RolloutPrisonRepository = mock { on { findAll() } doReturn (rolledOutPrisons) }
  private val bankHolidayService: BankHolidayService = mock { on { isEnglishBankHoliday(any()) } doReturn (false) }
  private val jobRepository: JobRepository = mock()
  private val safeJobRunner = spy(SafeJobRunner(jobRepository))
  private val jobDefinitionCaptor = argumentCaptor<JobDefinition>()

  private val job = ManageScheduledInstancesService(
    activityRepository,
    activityScheduleRepository,
    rolloutPrisonRepository,
    bankHolidayService,
    safeJobRunner,
    7L,
  )

  private val today = LocalDate.now()

  private val weekFromToday = today.plusWeeks(1)

  @Captor
  private lateinit var scheduleSaveCaptor: ArgumentCaptor<ActivitySchedule>

  @Test
  fun `schedules 6 session instances over 7 days for multiple active prisons`() {
    whenever(activityRepository.getBasicForPrisonBetweenDates("MDI", today, weekFromToday)).thenReturn(moorlandBasic)
    whenever(activityRepository.getBasicForPrisonBetweenDates("LEI", today, weekFromToday)).thenReturn(leedsBasic)

    whenever(activityScheduleRepository.getActivityScheduleByIdWithFilters(1L, today))
      .thenReturn(Optional.of(moorlandActivities.first().schedules().first()))

    whenever(activityScheduleRepository.getActivityScheduleByIdWithFilters(2L, today))
      .thenReturn(Optional.of(moorlandActivities[1].schedules().first()))

    whenever(activityScheduleRepository.getActivityScheduleByIdWithFilters(3L, today))
      .thenReturn(Optional.of(moorlandActivities.last().schedules().first()))

    whenever(activityScheduleRepository.getActivityScheduleByIdWithFilters(4L, today))
      .thenReturn(Optional.of(leedsActivities.first().schedules().first()))

    whenever(activityScheduleRepository.getActivityScheduleByIdWithFilters(5L, today))
      .thenReturn(Optional.of(leedsActivities[1].schedules().first()))

    whenever(activityScheduleRepository.getActivityScheduleByIdWithFilters(6L, today))
      .thenReturn(Optional.of(leedsActivities.last().schedules().first()))

    job.create()

    verify(safeJobRunner).runJob(jobDefinitionCaptor.capture())
    assertThat(jobDefinitionCaptor.firstValue.jobType).isEqualTo(JobType.SCHEDULES)

    // Creates 6 scheduled instances for 3 activities in Moorland and 3 activities in Leeds
    verify(activityScheduleRepository, times(6)).getActivityScheduleByIdWithFilters(anyLong(), any())
    verify(activityScheduleRepository, times(6)).saveAndFlush(scheduleSaveCaptor.capture())

    scheduleSaveCaptor.savedSchedules(6).forEach { schedule ->
      assertThat(schedule.activity.prisonCode).isIn(listOf("LEI", "MDI"))
      schedule.instances().forEach { instance -> assertThat(instance.sessionDate).isBetween(today, weekFromToday) }
      assertThat(schedule.instancesLastUpdatedTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
    }
  }

  @Test
  fun `can schedule multiple slots for the same day`() {
    whenever(activityRepository.getBasicForPrisonBetweenDates("MDI", today, weekFromToday))
      .thenReturn(moorlandBasicWithMultipleSlots)

    whenever(activityRepository.getAllForPrisonBetweenDates("LEI", today, weekFromToday))
      .thenReturn(emptyList())

    whenever(activityScheduleRepository.getActivityScheduleByIdWithFilters(1L, today))
      .thenReturn(Optional.of(activityWithMultipleSlots.first().schedules().first()))

    job.create()

    verify(activityScheduleRepository).getActivityScheduleByIdWithFilters(1L, today)
    verify(activityScheduleRepository).saveAndFlush(scheduleSaveCaptor.capture())

    scheduleSaveCaptor.savedSchedules(1).forEach {
      with(it) {
        assertThat(slots()).hasSize(2)
        instances().forEach { instance ->
          assertThat(instance.sessionDate).isBetween(today, weekFromToday)
          assertThat(instance.startTime).isIn(listOf(LocalTime.of(9, 30), LocalTime.of(13, 30)))
        }
      }
    }
  }

  @Test
  fun `does not schedule an activity instance today when one exists for today`() {
    whenever(activityRepository.getBasicForPrisonBetweenDates("MDI", today, weekFromToday))
      .thenReturn(moorlandBasicWithExistingInstance)

    whenever(activityRepository.getBasicForPrisonBetweenDates("LEI", today, weekFromToday))
      .thenReturn(emptyList())

    whenever(activityScheduleRepository.getActivityScheduleByIdWithFilters(1L, today))
      .thenReturn(Optional.of(activityWithExistingInstance.first().schedules().first()))

    job.create()

    verify(activityScheduleRepository).getActivityScheduleByIdWithFilters(1L, today)
    verify(activityScheduleRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `does schedule an instance even when it has an active suspension`() {
    // There is no UI way to configure planned suspensions so for now we will schedule whether suspended or not
    whenever(activityRepository.getBasicForPrisonBetweenDates("MDI", today, weekFromToday))
      .thenReturn(moorlandBasicWithSuspension)

    whenever(activityRepository.getBasicForPrisonBetweenDates("LEI", today, weekFromToday))
      .thenReturn(emptyList())

    whenever(activityScheduleRepository.getActivityScheduleByIdWithFilters(1L, today))
      .thenReturn(Optional.of(activityWithSuspension.first().schedules().first()))

    job.create()

    verify(activityScheduleRepository).getActivityScheduleByIdWithFilters(1L, today)
    verify(activityScheduleRepository).saveAndFlush(scheduleSaveCaptor.capture())

    scheduleSaveCaptor.savedSchedules(1).forEach {
      assertThat(it.isSuspendedOn(today)).isTrue
      assertThat(it.instances()).hasSize(1)
    }
  }

  @Test
  fun `does not schedule an instance on a bank holiday when the activity does not run on bank holidays`() {
    whenever(activityRepository.getBasicForPrisonBetweenDates("MDI", today, weekFromToday))
      .thenReturn(moorlandBasicNotBankHoliday)

    whenever(activityRepository.getBasicForPrisonBetweenDates("LEI", today, weekFromToday))
      .thenReturn(emptyList())

    whenever(activityScheduleRepository.getActivityScheduleByIdWithFilters(1L, today))
      .thenReturn(Optional.of(activityDoesNotRunOnABankHoliday.first().schedules().first()))

    whenever(bankHolidayService.isEnglishBankHoliday(today)).thenReturn(true)

    job.create()

    verify(activityScheduleRepository).getActivityScheduleByIdWithFilters(1L, today)
    verify(activityRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `schedules an instance on a bank holiday if activity runs on a bank holiday`() {
    whenever(activityRepository.getBasicForPrisonBetweenDates("MDI", today, weekFromToday))
      .thenReturn(moorlandBasicBankHoliday)

    whenever(activityRepository.getBasicForPrisonBetweenDates("LEI", today, weekFromToday))
      .thenReturn(emptyList())

    whenever(activityScheduleRepository.getActivityScheduleByIdWithFilters(1L, today))
      .thenReturn(Optional.of(activityRunsOnABankHoliday.first().schedules().first()))

    whenever(bankHolidayService.isEnglishBankHoliday(today)).thenReturn(true)

    job.create()

    verify(activityScheduleRepository).getActivityScheduleByIdWithFilters(1L, today)
    verify(activityScheduleRepository).saveAndFlush(scheduleSaveCaptor.capture())

    scheduleSaveCaptor.savedSchedules(1).forEach {
      it.instances().forEach { instance ->
        assertThat(instance.sessionDate).isBetween(today, weekFromToday)
      }
    }
  }

  private fun ArgumentCaptor<ActivitySchedule>.savedSchedules(expectedNumberOfSavedSchedules: Int) =
    this.allValues.toList().also { assertThat(it).hasSize(expectedNumberOfSavedSchedules) }

  companion object {

    val rolledOutPrisons = listOf(
      RolloutPrison(1, "MDI", "Moorland", true, LocalDate.of(2022, 11, 1), true, LocalDate.of(2022, 11, 1)),
      RolloutPrison(2, "LEI", "Leeds", true, LocalDate.of(2022, 11, 1), true, LocalDate.of(2022, 11, 1)),
      RolloutPrison(3, "XXX", "Other prison", false, null, true, LocalDate.of(2022, 11, 1)),
    )

    val yesterday: LocalDate = LocalDate.now().minusDays(1)

    val moorlandBasic = listOf(
      ActivityBasic("MDI", 1L, 1L, "A", yesterday, null),
      ActivityBasic("MDI", 2L, 2L, "B", yesterday, null),
      ActivityBasic("MDI", 3L, 3L, "C", yesterday, null),
    )

    val leedsBasic = listOf(
      ActivityBasic("LEI", 4L, 4L, "D", yesterday, null),
      ActivityBasic("LEI", 5L, 5L, "E", yesterday, null),
      ActivityBasic("LEI", 6L, 6L, "F", yesterday, null),
    )

    val moorlandActivities = listOf(
      activityEntity(
        activityId = 1L,
        prisonCode = "MDI",
        summary = "A",
        description = "AAA",
        startDate = LocalDate.now().minusDays(1),
        noSchedules = true,
      ).apply {
        this.addSchedule(
          activitySchedule(
            activity = this,
            activityScheduleId = 1,
            monday = true,
            noAllocations = true,
            noInstances = true,
          ),
        )
      },
      activityEntity(
        activityId = 2L,
        prisonCode = "MDI",
        summary = "B",
        description = "BBB",
        startDate = LocalDate.now().minusDays(1),
        noSchedules = true,
      ).apply {
        this.addSchedule(
          activitySchedule(
            activity = this,
            activityScheduleId = 2,
            monday = false,
            tuesday = true,
            noAllocations = true,
            noInstances = true,
          ),
        )
      },
      activityEntity(
        activityId = 3L,
        prisonCode = "MDI",
        summary = "C",
        description = "CCC",
        startDate = LocalDate.now().minusDays(1),
        noSchedules = true,
      ).apply {
        this.addSchedule(
          activitySchedule(
            activity = this,
            activityScheduleId = 3,
            monday = false,
            wednesday = true,
            noAllocations = true,
            noInstances = true,
          ),
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
        noSchedules = true,
      ).apply {
        this.addSchedule(
          activitySchedule(
            activity = this,
            activityScheduleId = 4,
            monday = true,
            noAllocations = true,
            noInstances = true,
          ),
        )
      },
      activityEntity(
        activityId = 5L,
        prisonCode = "LEI",
        summary = "E",
        description = "EEE",
        startDate = LocalDate.now().minusDays(1),
        noSchedules = true,
      ).apply {
        this.addSchedule(
          activitySchedule(
            activity = this,
            activityScheduleId = 5,
            monday = false,
            tuesday = true,
            noAllocations = true,
            noInstances = true,
          ),
        )
      },
      activityEntity(
        activityId = 6L,
        prisonCode = "LEI",
        summary = "F",
        description = "FFF",
        startDate = LocalDate.now().minusDays(1),
        noSchedules = true,
      ).apply {
        this.addSchedule(
          activitySchedule(
            activity = this,
            activityScheduleId = 6,
            monday = false,
            wednesday = true,
            noAllocations = true,
            noInstances = true,
          ),
        )
      },
    )

    val moorlandBasicWithExistingInstance = listOf(
      ActivityBasic("MDI", 1L, 1L, "Existing", yesterday, null),
    )

    val activityWithExistingInstance = listOf(
      activityEntity(
        activityId = 1L,
        prisonCode = "MDI",
        summary = "Existing",
        description = "Existing instances",
        startDate = LocalDate.now().minusDays(1),
        noSchedules = true,
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
            noInstances = true,
          ).apply {
            this.addInstance(
              sessionDate = LocalDate.now(),
              slot = this.slots().first(),
            )
          },
        )
      },
    )

    val moorlandBasicWithSuspension = listOf(
      ActivityBasic("MDI", 1L, 1L, "Suspension", yesterday, null),
    )

    val activityWithSuspension = listOf(
      activityEntity(
        activityId = 1L,
        prisonCode = "MDI",
        summary = "Suspension",
        description = "With suspension",
        startDate = LocalDate.now().minusDays(1),
        noSchedules = true,
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
            noInstances = true,
          ).apply {
            this.suspensions.clear()
            this.suspensions.add(
              ActivityScheduleSuspension(
                activitySchedule = this,
                suspendedFrom = LocalDate.now(),
                suspendedUntil = LocalDate.now().plusDays(3),
              ),
            )
          },
        )
      },
    )

    val moorlandBasicNotBankHoliday = listOf(
      ActivityBasic("MDI", 1L, 1L, "Not BH", yesterday, null),
    )

    val activityDoesNotRunOnABankHoliday = listOf(
      activityEntity(
        activityId = 1L,
        prisonCode = "MDI",
        summary = "Not BH",
        description = "Not on bank holiday",
        startDate = LocalDate.now().minusDays(1),
        noSchedules = true,
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
            noInstances = true,
          ).apply {
            this.suspensions.clear()
          },
        )
      },
    )

    val moorlandBasicBankHoliday = listOf(
      ActivityBasic("MDI", 1L, 1L, "BH", yesterday, null),
    )

    val activityRunsOnABankHoliday = listOf(
      activityEntity(
        activityId = 1L,
        prisonCode = "MDI",
        summary = "BH",
        description = "Runs on a bank holiday",
        startDate = LocalDate.now().minusDays(1),
        noSchedules = true,
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
            noInstances = true,
          ).apply {
            this.suspensions.clear()
          },
        )
      },
    )

    val moorlandBasicWithMultipleSlots = listOf(
      ActivityBasic("MDI", 1L, 1L, "Multiple", yesterday, null),
    )

    val activityWithMultipleSlots = listOf(
      activityEntity(
        activityId = 1L,
        prisonCode = "MDI",
        summary = "Multiple",
        description = "Slots",
        startDate = LocalDate.now().minusDays(1),
        noSchedules = true,
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
              ),
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
          },
        )
      },
    )
  }
}
