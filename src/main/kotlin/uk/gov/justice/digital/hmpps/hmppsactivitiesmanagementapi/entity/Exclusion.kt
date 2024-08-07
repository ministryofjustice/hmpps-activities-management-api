package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Slot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.consolidateMatchingSlots
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@Entity
@Table(name = "exclusion")
data class Exclusion(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val exclusionId: Long = 0,

  @ManyToOne
  @JoinColumn(name = "allocation_id", nullable = false)
  val allocation: Allocation,

  var startDate: LocalDate,

  val weekNumber: Int,

  private var slotStartTime: LocalTime,

  private var slotEndTime: LocalTime,

  private var mondayFlag: Boolean = false,

  private var tuesdayFlag: Boolean = false,

  private var wednesdayFlag: Boolean = false,

  private var thursdayFlag: Boolean = false,

  private var fridayFlag: Boolean = false,

  private var saturdayFlag: Boolean = false,

  private var sundayFlag: Boolean = false,

  @Enumerated(EnumType.STRING)
  var timeSlot: TimeSlot

) {
  var endDate: LocalDate? = null
    private set

  override fun hashCode(): Int = exclusionId.hashCode()

  fun getDaysOfWeek(): Set<DayOfWeek> = setOfNotNull(
    DayOfWeek.MONDAY.takeIf { mondayFlag },
    DayOfWeek.TUESDAY.takeIf { tuesdayFlag },
    DayOfWeek.WEDNESDAY.takeIf { wednesdayFlag },
    DayOfWeek.THURSDAY.takeIf { thursdayFlag },
    DayOfWeek.FRIDAY.takeIf { fridayFlag },
    DayOfWeek.SATURDAY.takeIf { saturdayFlag },
    DayOfWeek.SUNDAY.takeIf { sundayFlag },
  )
  fun setDaysOfWeek(days: Set<DayOfWeek>) {
    mondayFlag = days.contains(DayOfWeek.MONDAY)
    tuesdayFlag = days.contains(DayOfWeek.TUESDAY)
    wednesdayFlag = days.contains(DayOfWeek.WEDNESDAY)
    thursdayFlag = days.contains(DayOfWeek.THURSDAY)
    fridayFlag = days.contains(DayOfWeek.FRIDAY)
    saturdayFlag = days.contains(DayOfWeek.SATURDAY)
    sundayFlag = days.contains(DayOfWeek.SUNDAY)
  }

  fun endNow() = run { endDate = LocalDate.now() }

  fun timeSlot() = TimeSlot.slot(slotStartTime)

  fun slotTimes() = slotStartTime to slotEndTime

  fun setSlotTimes(slotTimes: SlotTimes) = run {
    slotStartTime = slotTimes.first
    slotEndTime = slotTimes.second
  }

  fun toSlotModel() = Slot(
    weekNumber = weekNumber,
    timeSlot = timeSlot().toString(),
    monday = mondayFlag,
    tuesday = tuesdayFlag,
    wednesday = wednesdayFlag,
    thursday = thursdayFlag,
    friday = fridayFlag,
    saturday = saturdayFlag,
    sunday = sundayFlag,
  )

  companion object {
    val tomorrow: LocalDate = LocalDate.now().plusDays(1)

    fun valueOf(
      allocation: Allocation,
      slotTimes: SlotTimes,
      weekNumber: Int,
      daysOfWeek: Set<DayOfWeek>,
      startDate: LocalDate = maxOf(tomorrow, allocation.startDate),
    ) = Exclusion(
      allocation = allocation,
      slotStartTime = slotTimes.first,
      slotEndTime = slotTimes.second,
      weekNumber = weekNumber,
      startDate = startDate,
    ).apply {
      setDaysOfWeek(daysOfWeek)
    }
  }
}

fun Set<Exclusion>.toSlotModel() = map { it.toSlotModel() }.consolidateMatchingSlots()
