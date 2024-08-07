package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Slot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.consolidateMatchingSlots
import java.time.DayOfWeek
import java.time.LocalDate

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

  @Enumerated(EnumType.STRING)
  var timeSlot: TimeSlot,

  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "exclusion_id")
  private var exclusionDaysOfWeek: MutableList<ExclusionDaysOfWeek> = mutableListOf(),

) {
  var endDate: LocalDate? = null
    private set

  override fun hashCode(): Int = exclusionId.hashCode()

  fun endNow() = run { endDate = LocalDate.now() }

  fun getDaysOfWeek() = this.exclusionDaysOfWeek.map { it.dayOfWeek }.toSet()

  fun setDaysOfWeek(daysOfWeek: Set<DayOfWeek>) {
    this.exclusionDaysOfWeek.clear()
    this.exclusionDaysOfWeek.addAll(
      daysOfWeek.map {
        ExclusionDaysOfWeek(dayOfWeek = it)
      },
    )
  }

  fun toSlotModel() = Slot(
    weekNumber = weekNumber,
    timeSlot = timeSlot.name,
    monday = exclusionDaysOfWeek.containsDay(DayOfWeek.MONDAY),
    tuesday = exclusionDaysOfWeek.containsDay(DayOfWeek.TUESDAY),
    wednesday = exclusionDaysOfWeek.containsDay(DayOfWeek.WEDNESDAY),
    thursday = exclusionDaysOfWeek.containsDay(DayOfWeek.THURSDAY),
    friday = exclusionDaysOfWeek.containsDay(DayOfWeek.FRIDAY),
    saturday = exclusionDaysOfWeek.containsDay(DayOfWeek.SATURDAY),
    sunday = exclusionDaysOfWeek.containsDay(DayOfWeek.SUNDAY),
  )

  companion object {
    val tomorrow: LocalDate = LocalDate.now().plusDays(1)

    fun List<ExclusionDaysOfWeek>.containsDay(day: DayOfWeek) =
      this.map { it.dayOfWeek }.contains(day)

    fun valueOf(
      allocation: Allocation,
      weekNumber: Int,
      daysOfWeek: Set<DayOfWeek>,
      startDate: LocalDate = maxOf(tomorrow, allocation.startDate),
      timeSlot: TimeSlot,
    ) = Exclusion(
      allocation = allocation,
      weekNumber = weekNumber,
      startDate = startDate,
      timeSlot = timeSlot,
      exclusionDaysOfWeek = daysOfWeek.map {
        ExclusionDaysOfWeek(dayOfWeek = it)
      }.toMutableList(),
    )
  }
}

fun Set<Exclusion>.toSlotModel() = map { it.toSlotModel() }.consolidateMatchingSlots()
