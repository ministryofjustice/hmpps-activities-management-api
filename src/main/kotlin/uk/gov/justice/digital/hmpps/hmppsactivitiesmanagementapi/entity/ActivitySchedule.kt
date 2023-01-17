package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.PayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.PrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocation
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table

@Entity
@Table(name = "activity_schedule")
@EntityListeners(ActivityScheduleEntityListener::class)
data class ActivitySchedule(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val activityScheduleId: Long? = null,

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
  val instances: MutableList<ScheduledInstance> = mutableListOf(),

  @OneToMany(
    mappedBy = "activitySchedule",
    fetch = FetchType.EAGER,
    cascade = [CascadeType.ALL],
    orphanRemoval = true
  )
  @Fetch(FetchMode.SUBSELECT)
  val suspensions: MutableList<ActivityScheduleSuspension> = mutableListOf(),

  @OneToMany(
    mappedBy = "activitySchedule",
    fetch = FetchType.EAGER,
    cascade = [CascadeType.ALL],
    orphanRemoval = true
  )
  @Fetch(FetchMode.SUBSELECT)
  val allocations: MutableList<Allocation> = mutableListOf(),

  @OneToMany(
    mappedBy = "activitySchedule",
    fetch = FetchType.EAGER,
    cascade = [CascadeType.ALL],
    orphanRemoval = true
  )
  @Fetch(FetchMode.SUBSELECT)
  val slots: MutableList<ActivityScheduleSlot> = mutableListOf(),

  val description: String,

  var internalLocationId: Int? = null,

  var internalLocationCode: String? = null,

  var internalLocationDescription: String? = null,

  val capacity: Int,

  val startDate: LocalDate
) {
  var endDate: LocalDate? = null
    set(value) {
      field = if (value != null && value.isAfter(startDate).not()) {
        throw IllegalArgumentException("End date must be after the start date")
      } else {
        value
      }
    }

  fun addSlot(startTime: LocalTime, endTime: LocalTime, daysOfWeek: Set<DayOfWeek>) {
    if (endTime.isAfter(startTime).not()) {
      throw IllegalArgumentException("Start time '$startTime' must be before end time '$endTime'.")
    }

    if (daysOfWeek.isEmpty()) throw IllegalArgumentException("One ore more days must be specified for a given slot.")

    slots.add(
      ActivityScheduleSlot(
        activitySchedule = this,
        startTime = startTime,
        endTime = endTime,
        mondayFlag = daysOfWeek.contains(DayOfWeek.MONDAY),
        tuesdayFlag = daysOfWeek.contains(DayOfWeek.TUESDAY),
        wednesdayFlag = daysOfWeek.contains(DayOfWeek.WEDNESDAY),
        thursdayFlag = daysOfWeek.contains(DayOfWeek.THURSDAY),
        fridayFlag = daysOfWeek.contains(DayOfWeek.FRIDAY),
        saturdayFlag = daysOfWeek.contains(DayOfWeek.SATURDAY),
        sundayFlag = daysOfWeek.contains(DayOfWeek.SUNDAY)
      )
    )
  }

  fun toModelLite() = ActivityScheduleLite(
    id = this.activityScheduleId ?: -1,
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

  fun allocatePrisoner(prisonerNumber: PrisonerNumber, payBand: PayBand, bookingId: Long?, allocatedBy: String) {
    failIfAlreadyAllocated(prisonerNumber)
    failIfAllocatedByIsBlank(allocatedBy)

    allocations.add(
      Allocation(
        activitySchedule = this,
        prisonerNumber = prisonerNumber.toString(),
        bookingId = bookingId,
        payBand = payBand.toString(),
        // TODO not sure if this is supported in the UI
        startDate = LocalDate.now(),
        allocatedBy = allocatedBy,
        allocatedTime = LocalDateTime.now()
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
