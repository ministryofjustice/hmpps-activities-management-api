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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.PrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.between
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocation
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@Entity
@Table(name = "activity_schedule")
@EntityListeners(ActivityScheduleEntityListener::class)
data class ActivitySchedule(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val activityScheduleId: Long = 0,

  @ManyToOne
  @JoinColumn(name = "activity_id", nullable = false)
  val activity: Activity,

  @OneToMany(
    mappedBy = "activitySchedule",
    fetch = FetchType.EAGER,
    cascade = [CascadeType.ALL],
    orphanRemoval = true,
  )
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
) {
  init {
    failIfInvalidCapacity()
  }

  private fun failIfInvalidCapacity() {
    if (capacity < 1) {
      throw IllegalArgumentException("The schedule capacity must be greater than zero.")
    }
  }

  @OneToMany(
    mappedBy = "activitySchedule",
    fetch = FetchType.EAGER,
    cascade = [CascadeType.ALL],
    orphanRemoval = true,
  )
  @Fetch(FetchMode.SUBSELECT)
  private val instances: MutableList<ScheduledInstance> = mutableListOf()

  @OneToMany(
    mappedBy = "activitySchedule",
    fetch = FetchType.EAGER,
    cascade = [CascadeType.ALL],
    orphanRemoval = true,
  )
  @Fetch(FetchMode.SUBSELECT)
  private val allocations: MutableList<Allocation> = mutableListOf()

  @OneToMany(
    mappedBy = "activitySchedule",
    fetch = FetchType.EAGER,
    cascade = [CascadeType.ALL],
    orphanRemoval = true,
  )
  @Fetch(FetchMode.SUBSELECT)
  private val slots: MutableList<ActivityScheduleSlot> = mutableListOf()

  var endDate: LocalDate? = null
    set(value) {
      field = if (value != null && value.isAfter(startDate).not()) {
        throw IllegalArgumentException("End date must be after the start date")
      } else {
        value
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
    ) = ActivitySchedule(
      activity = activity,
      description = description,
      internalLocationId = internalLocationId,
      internalLocationCode = internalLocationCode,
      internalLocationDescription = internalLocationDescription,
      capacity = capacity,
      startDate = startDate,
      runsOnBankHoliday = runsOnBankHoliday,
    ).apply {
      this.endDate = endDate
    }
  }

  /**
   * This does not take into account any suspensions that may be in effect.
   */
  fun isActiveOn(date: LocalDate): Boolean = date.between(startDate, endDate)

  fun isSuspendedOn(date: LocalDate) = suspensions.any { it.isSuspendedOn(date) }

  fun hasNoInstancesOnDate(day: LocalDate) =
    this.instances.none { scheduledInstance -> scheduledInstance.sessionDate == day }

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

  fun addSlot(startTime: LocalTime, endTime: LocalTime, daysOfWeek: Set<DayOfWeek>) =
    addSlot(ActivityScheduleSlot.valueOf(this, startTime, endTime, daysOfWeek))

  fun addSlot(slot: ActivityScheduleSlot): ActivityScheduleSlot {
    if (slot.activitySchedule.activityScheduleId != activityScheduleId) throw IllegalArgumentException("Can only add slots that belong to this schedule.")

    slots.add(slot)

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

  fun deallocatePrisonerOn(prisonerNumber: String, date: LocalDate, reason: DeallocationReason, by: String) {
    if (isActiveOn(date)) {
      allocations.firstOrNull { it.prisonerNumber == prisonerNumber }?.deallocateOn(date, reason, by)
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
    slots = this.slots.map { it.toModel() },
    startDate = this.startDate,
    endDate = this.endDate,
  ).apply {
    if (!this.activity.inCell) {
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

  fun removeInstances(fromDate: LocalDate, toDate: LocalDate) {
    instances.removeAll(instances().filter { it.sessionDate.between(fromDate, toDate) })
  }

  fun updateSlots(updates: Map<Pair<LocalTime, LocalTime>, Set<DayOfWeek>>) {
    removeRedundantSlots(updates)
    updateMatchingSlots(updates)
    addNewSlots(updates)
  }

  private fun removeRedundantSlots(updates: Map<Pair<LocalTime, LocalTime>, Set<DayOfWeek>>) {
    slots.removeAll(slots.filterNot { updates.containsKey(Pair(it.startTime, it.endTime)) })
  }

  private fun updateMatchingSlots(updates: Map<Pair<LocalTime, LocalTime>, Set<DayOfWeek>>) {
    slots.forEach { slot ->
      updates[slot.startTime to slot.endTime]?.let(slot::update)
    }
  }

  private fun addNewSlots(updates: Map<Pair<LocalTime, LocalTime>, Set<DayOfWeek>>) {
    updates.keys.filterNot { key ->
      slots.map { Pair(it.startTime, it.endTime) }.contains(key)
    }.forEach {
      addSlot(it.first, it.second, updates[it]!!)
    }
  }
}

fun List<ActivitySchedule>.toModelLite() = map { it.toModelLite() }
