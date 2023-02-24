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
  val activityScheduleId: Long = -1,

  @ManyToOne
  @JoinColumn(name = "activity_id", nullable = false)
  val activity: Activity,

  @OneToMany(
    mappedBy = "activitySchedule",
    fetch = FetchType.EAGER,
    cascade = [CascadeType.ALL],
    orphanRemoval = true
  )
  @Fetch(FetchMode.SUBSELECT)
  val suspensions: MutableList<ActivityScheduleSuspension> = mutableListOf(),

  val description: String,

  var internalLocationId: Int? = null,

  var internalLocationCode: String? = null,

  var internalLocationDescription: String? = null,

  val capacity: Int,

  val startDate: LocalDate,

  val runsOnBankHoliday: Boolean = false,

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
    orphanRemoval = true
  )
  @Fetch(FetchMode.SUBSELECT)
  private val instances: MutableList<ScheduledInstance> = mutableListOf()

  @OneToMany(
    mappedBy = "activitySchedule",
    fetch = FetchType.EAGER,
    cascade = [CascadeType.ALL],
    orphanRemoval = true
  )
  @Fetch(FetchMode.SUBSELECT)
  private val allocations: MutableList<Allocation> = mutableListOf()

  @OneToMany(
    mappedBy = "activitySchedule",
    fetch = FetchType.EAGER,
    cascade = [CascadeType.ALL],
    orphanRemoval = true
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

  fun allocations() = allocations.toList()

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
      runsOnBankHoliday = runsOnBankHoliday
    ).apply {
      this.endDate = endDate
    }
  }

  /**
   * This does not take into account any suspensions that may be in effect.
   */
  fun isActiveOn(date: LocalDate): Boolean = date.between(startDate, endDate)

  fun isSuspendedOn(date: LocalDate) = suspensions.any { it.isSuspendedOn(date) }

  fun addInstance(
    sessionDate: LocalDate,
    slot: ActivityScheduleSlot
  ): ScheduledInstance {
    failIfMatchingInstanceAlreadyPresent(sessionDate, slot)
    failIfSlotNotPartOfThisSchedule(slot)

    instances.add(
      ScheduledInstance(
        activitySchedule = this,
        sessionDate = sessionDate,
        startTime = slot.startTime,
        endTime = slot.endTime
      )
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
    slot: ActivityScheduleSlot
  ) {
    if (instances.any { it.sessionDate == sessionDate && it.startTime == slot.startTime && it.endTime == slot.endTime }) {
      throw IllegalArgumentException("An instance for date '$sessionDate', start time '${slot.startTime}' and end time '${slot.endTime}' already exists")
    }
  }

  fun addSlot(startTime: LocalTime, endTime: LocalTime, daysOfWeek: Set<DayOfWeek>) {
    addSlot(ActivityScheduleSlot.valueOf(this, startTime, endTime, daysOfWeek))
  }

  fun addSlot(slot: ActivityScheduleSlot) {
    if (slot.activitySchedule.activityScheduleId != activityScheduleId) throw IllegalArgumentException("Can only add slots that belong to this schedule.")

    slots.add(slot)
  }

  fun toModelLite() = ActivityScheduleLite(
    id = this.activityScheduleId,
    description = this.description,
    internalLocation = InternalLocation(
      id = internalLocationId!!,
      code = internalLocationCode!!,
      description = internalLocationDescription!!
    ),
    capacity = this.capacity,
    activity = this.activity.toModelLite(),
    slots = this.slots.map { it.toModel() },
    startDate = this.startDate,
    endDate = this.endDate
  )

  fun getAllocationsOnDate(date: LocalDate): List<Allocation> = this.allocations.filter {
    !date.isBefore(it.startDate) && (it.endDate == null || !date.isAfter(it.endDate))
  }

  fun allocatePrisoner(
    prisonerNumber: PrisonerNumber,
    payBand: PrisonPayBand,
    bookingId: Long?,
    startDate: LocalDate = LocalDate.now(),
    endDate: LocalDate? = null,
    allocatedBy: String
  ) {
    failIfAlreadyAllocated(prisonerNumber)
    failIfAllocatedByIsBlank(allocatedBy)

    allocations.add(
      Allocation(
        activitySchedule = this,
        prisonerNumber = prisonerNumber.toString(),
        bookingId = bookingId,
        payBand = payBand,
        // TODO not sure if this is supported in the UI
        startDate = startDate,
        endDate = endDate,
        allocatedBy = allocatedBy,
        allocatedTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
      )
    )
  }

  private fun failIfAllocatedByIsBlank(allocatedBy: String) {
    if (allocatedBy.isBlank()) throw IllegalArgumentException("Allocated by cannot be blank.")
  }

  private fun failIfAlreadyAllocated(prisonerNumber: PrisonerNumber) =
    allocations.firstOrNull { PrisonerNumber.valueOf(it.prisonerNumber) == prisonerNumber }
      ?.let { throw IllegalArgumentException("Prisoner '$prisonerNumber' is already allocated to schedule $description.") }

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(activityScheduleId = $activityScheduleId )"
  }
}

fun List<ActivitySchedule>.toModelLite() = map { it.toModelLite() }
