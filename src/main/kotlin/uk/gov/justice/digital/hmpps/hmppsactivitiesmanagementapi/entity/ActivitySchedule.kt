package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.hibernate.annotations.Filters
import org.hibernate.annotations.ParamDef
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.PrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.between
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.ifNotEmpty
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocation
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

typealias AllocationIds = Set<Long>

const val SESSION_DATE_FILTER = "SessionDateFilter"
const val ALLOCATION_DATE_FILTER = "AllocationDateFilter"

@Entity
@Table(name = "activity_schedule")
@EntityListeners(ActivityScheduleEntityListener::class)
@FilterDef(
  name = SESSION_DATE_FILTER,
  parameters = [
    ParamDef(name = "earliestSessionDate", type = LocalDate::class),
  ],
)
@FilterDef(
  name = ALLOCATION_DATE_FILTER,
  parameters = [
    ParamDef(name = "allocationsActiveOnDate", type = LocalDate::class),
  ],
)
data class ActivitySchedule(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val activityScheduleId: Long = 0,

  @ManyToOne
  @JoinColumn(name = "activity_id", nullable = false)
  val activity: Activity,

  @OneToMany(mappedBy = "activitySchedule", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  val suspensions: MutableList<ActivityScheduleSuspension> = mutableListOf(),

  var description: String,

  var internalLocationId: Int? = null,

  var internalLocationCode: String? = null,

  var internalLocationDescription: String? = null,

  var capacity: Int,

  var startDate: LocalDate,

  var runsOnBankHoliday: Boolean = false,

  var updatedTime: LocalDateTime? = null,

  var updatedBy: String? = null,

  var instancesLastUpdatedTime: LocalDateTime? = null,

  var scheduleWeeks: Int,
) {

  init {
    require(capacity > 0) { "The schedule capacity must be greater than zero." }
    require(scheduleWeeks > 0) { "Schedule weeks must be greater than zero." }
  }

  @OneToMany(mappedBy = "activitySchedule", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Filters(Filter(name = "SessionDateFilter", condition = "session_date >= :earliestSessionDate"))
  private val instances: MutableList<ScheduledInstance> = mutableListOf()

  @OneToMany(mappedBy = "activitySchedule", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  @Filters(
    Filter(
      name = ALLOCATION_DATE_FILTER,
      condition = "start_date <= :allocationsActiveOnDate AND (end_date IS NULL OR end_date >= :allocationsActiveOnDate)",
    ),
  )
  private val allocations: MutableList<Allocation> = mutableListOf()

  @OneToMany(mappedBy = "activitySchedule", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  private val slots: MutableList<ActivityScheduleSlot> = mutableListOf()

  var endDate: LocalDate? = null
    set(value) {
      require(value == null || value >= startDate) {
        "Activity schedule end date cannot be before activity schedule start date."
      }

      field = value.also { updateImpactedAllocations(it) }
    }

  private fun updateImpactedAllocations(newEndDate: LocalDate?) {
    newEndDate?.let {
      allocations
        .filterNot { allocation -> allocation.status(PrisonerStatus.ENDED) }
        .filter { allocation -> allocation.endDate != null && allocation.endDate?.isAfter(newEndDate) == true }
        .forEach { allocation ->
          allocation.endDate = newEndDate
        }
    }
  }

  fun instances() = instances.toList()

  fun slots() = slots.toList()

  fun allocations(excludeEnded: Boolean = false): List<Allocation> =
    allocations.toList().filter { !excludeEnded || !it.status(PrisonerStatus.ENDED) }

  companion object {
    fun valueOf(
      activity: Activity,
      description: String,
      internalLocationId: Int?,
      internalLocationCode: String?,
      internalLocationDescription: String?,
      capacity: Int,
      startDate: LocalDate,
      endDate: LocalDate?,
      runsOnBankHoliday: Boolean,
      scheduleWeeks: Int,
    ) = ActivitySchedule(
      activity = activity,
      description = description,
      internalLocationId = internalLocationId,
      internalLocationCode = internalLocationCode,
      internalLocationDescription = internalLocationDescription,
      capacity = capacity,
      startDate = startDate,
      runsOnBankHoliday = runsOnBankHoliday,
      scheduleWeeks = scheduleWeeks,
    ).apply {
      this.endDate = endDate
    }
  }

  /**
   * This does not take into account any suspensions that may be in effect.
   */
  fun isActiveOn(date: LocalDate): Boolean = date.between(startDate, endDate)

  fun isSuspendedOn(date: LocalDate) = suspensions.any { it.isSuspendedOn(date) }

  fun getWeekNumber(date: LocalDate): Int {
    require(!date.isBefore(startDate) && (endDate == null || !date.isAfter(endDate))) {
      "Date must be within activity schedule range."
    }
    // To calculate the week number for a given date:
    // - Calculate first Monday of schedule
    // - Calculate the total number of days into the schedule from the first Monday
    // - Calculate the number of days into the schedule period for the given date
    // - Finally, calculate the week number from the number of days into this schedule period
    val daysInWeek = 7
    val scheduleFirstMonday = startDate.minusDays(startDate.dayOfWeek.value - 1L)
    val daysIntoSchedule = ChronoUnit.DAYS.between(scheduleFirstMonday, date)
    val daysIntoThisSchedulePeriod = daysIntoSchedule % (daysInWeek * scheduleWeeks)
    return (daysIntoThisSchedulePeriod / daysInWeek).toInt() + 1
  }

  fun hasNoInstancesOnDate(day: LocalDate) =
    instances.none { instance -> instance.sessionDate == day }

  fun hasNoInstancesOnDate(day: LocalDate, startEndTime: Pair<LocalTime, LocalTime>) =
    instances.none { instance ->
      instance.sessionDate == day &&
        instance.startTime == startEndTime.first &&
        instance.endTime == startEndTime.second
    }

  fun addInstance(
    sessionDate: LocalDate,
    slot: ActivityScheduleSlot,
  ): ScheduledInstance {
    failIfMatchingInstanceAlreadyPresent(sessionDate, slot)
    failIfSlotNotPartOfThisSchedule(slot)

    instances.add(
      ScheduledInstance(
        activitySchedule = this,
        sessionDate = sessionDate,
        startTime = slot.startTime,
        endTime = slot.endTime,
      ),
    )
    instancesLastUpdatedTime = LocalDateTime.now()

    return instances.last()
  }

  private fun failIfSlotNotPartOfThisSchedule(slot: ActivityScheduleSlot) {
    if (slots.none { it == slot }) {
      throw IllegalArgumentException("Cannot add instance for slot '${slot.activityScheduleSlotId}', slot does not belong to this schedule.")
    }
  }

  private fun failIfMatchingInstanceAlreadyPresent(
    sessionDate: LocalDate,
    slot: ActivityScheduleSlot,
  ) {
    if (instances.any { it.sessionDate == sessionDate && it.startTime == slot.startTime && it.endTime == slot.endTime }) {
      throw IllegalArgumentException("An instance for date '$sessionDate', start time '${slot.startTime}' and end time '${slot.endTime}' already exists")
    }
  }

  fun addSlot(weekNumber: Int, startTime: LocalTime, endTime: LocalTime, daysOfWeek: Set<DayOfWeek>): ActivityScheduleSlot {
    slots.add(ActivityScheduleSlot.valueOf(this, weekNumber, startTime, endTime, daysOfWeek))
    return slots.last()
  }

  fun allocatePrisoner(
    prisonerNumber: PrisonerNumber,
    payBand: PrisonPayBand,
    bookingId: Long,
    startDate: LocalDate = LocalDate.now(),
    endDate: LocalDate? = null,
    allocatedBy: String,
  ): Allocation {
    failIfAlreadyAllocated(prisonerNumber)
    failIfAllocatedByIsBlank(allocatedBy)

    require(startDate >= this.activity.startDate) {
      "Allocation start date cannot be before activity start date"
    }
    require(endDate == null || this.activity.endDate == null || endDate <= this.activity.endDate) {
      "Allocation end date cannot be after activity end date"
    }
    require(endDate == null || endDate >= startDate) {
      "Allocation end date cannot be before allocation start date"
    }

    require(activity.endDate == null || startDate <= activity.endDate) {
      "Allocation start date cannot be after the activity end date."
    }

    allocations.add(
      Allocation(
        activitySchedule = this,
        prisonerNumber = prisonerNumber.toString(),
        prisonerStatus = if (startDate.isAfter(LocalDate.now())) PrisonerStatus.PENDING else PrisonerStatus.ACTIVE,
        bookingId = bookingId,
        payBand = payBand,
        startDate = startDate,
        allocatedBy = allocatedBy,
        allocatedTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES),
      ).apply {
        this.endDate = endDate?.also { deallocateOn(it, DeallocationReason.PLANNED, allocatedBy) }
      },
    )

    return allocations.last()
  }

  private fun failIfAllocatedByIsBlank(allocatedBy: String) {
    if (allocatedBy.isBlank()) throw IllegalArgumentException("Allocated by cannot be blank.")
  }

  private fun failIfAlreadyAllocated(prisonerNumber: PrisonerNumber) =
    allocations.firstOrNull { PrisonerNumber.valueOf(it.prisonerNumber) == prisonerNumber && it.prisonerStatus != PrisonerStatus.ENDED }
      ?.let { throw IllegalArgumentException("Prisoner '$prisonerNumber' is already allocated to schedule $description.") }

  fun deallocatePrisonerOn(prisonerNumber: String, date: LocalDate, reason: DeallocationReason, by: String): Allocation {
    if (isActiveOn(date)) {
      return allocations(excludeEnded = true).firstOrNull { it.prisonerNumber == prisonerNumber }?.deallocateOn(date, reason, by)
        ?: throw IllegalArgumentException("Allocation not found for prisoner $prisonerNumber for schedule $activityScheduleId.")
    } else {
      throw IllegalStateException("Schedule $activityScheduleId is not active on the planned deallocated date $date.")
    }
  }

  fun toModelLite() = ActivityScheduleLite(
    id = this.activityScheduleId,
    description = this.description,
    capacity = this.capacity,
    activity = this.activity.toModelLite(),
    scheduleWeeks = this.scheduleWeeks,
    slots = this.slots.map { it.toModel() },
    startDate = this.startDate,
    endDate = this.endDate,
  ).apply {
    if (!this.activity.inCell && !this.activity.onWing && !this.activity.offWing) {
      this.internalLocation = InternalLocation(
        id = internalLocationId!!,
        code = internalLocationCode!!,
        description = internalLocationDescription!!,
      )
    }
  }

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(activityScheduleId = $activityScheduleId )"
  }

  fun previous(scheduledInstance: ScheduledInstance): ScheduledInstance? =
    instances()
      .sortedWith(compareBy<ScheduledInstance> { it.sessionDate }.thenBy { it.startTime })
      .let { sorted -> sorted.getOrNull(sorted.indexOf(scheduledInstance) - 1) }

  fun next(scheduledInstance: ScheduledInstance): ScheduledInstance? =
    instances()
      .sortedWith(compareBy<ScheduledInstance> { it.sessionDate }.thenBy { it.startTime })
      .let { sorted -> sorted.getOrNull(sorted.indexOf(scheduledInstance) + 1) }

  fun removeInstances(instancesToRemove: List<ScheduledInstance>) {
    instances.removeAll(instancesToRemove)
    this.instancesLastUpdatedTime = LocalDateTime.now()
  }

  fun updateSlots(updates: Map<Pair<Int, Pair<LocalTime, LocalTime>>, Set<DayOfWeek>>): AllocationIds {
    val updatedAllocationIds = mutableSetOf<Long>()
    updateMatchingSlots(updates).let { updatedAllocationIds.addAll(it) }
    addNewSlots(updates)
    removeRedundantSlots(updates).let { updatedAllocationIds.addAll(it) }
    return updatedAllocationIds
  }

  private fun removeRedundantSlots(updates: Map<Pair<Int, Pair<LocalTime, LocalTime>>, Set<DayOfWeek>>): AllocationIds {
    val slotsToRemove = slots.filterNot { updates.containsKey(Pair(it.weekNumber, it.startTime to it.endTime)) }
    slots.removeAll(slotsToRemove)
    require(slots.isNotEmpty()) { "Must have at least 1 active slot across the schedule" }
    return slotsToRemove.flatMap { it.exclusions.map { ex -> ex.allocation.allocationId } }.toSet()
  }

  private fun updateMatchingSlots(updates: Map<Pair<Int, Pair<LocalTime, LocalTime>>, Set<DayOfWeek>>): AllocationIds {
    val updatedAllocationIds = mutableSetOf<Long>()
    slots.forEach { slot ->
      updates[Pair(slot.weekNumber, slot.startTime to slot.endTime)]?.let(slot::update)?.ifNotEmpty { updatedAllocationIds.addAll(it) }
    }
    return updatedAllocationIds
  }

  private fun addNewSlots(updates: Map<Pair<Int, Pair<LocalTime, LocalTime>>, Set<DayOfWeek>>) {
    updates.keys.filterNot { key ->
      slots.map { Pair(it.weekNumber, it.startTime to it.endTime) }.contains(key)
    }.forEach {
      addSlot(it.first, it.second.first, it.second.second, updates[it]!!)
    }
  }
}

fun List<ActivitySchedule>.toModelLite() = map { it.toModelLite() }
