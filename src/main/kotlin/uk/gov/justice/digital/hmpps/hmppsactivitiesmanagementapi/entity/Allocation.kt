package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.between
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.enumeration.ServiceName
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Slot
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation as ModelAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.DeallocationReason as ModelDeallocationReason

@Entity
@Table(name = "allocation")
@EntityListeners(AllocationEntityListener::class, AuditableEntityListener::class)
data class Allocation(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val allocationId: Long = 0,

  @ManyToOne
  @JoinColumn(name = "activity_schedule_id", nullable = false)
  val activitySchedule: ActivitySchedule,

  val prisonerNumber: String,

  val bookingId: Long,

  @Enumerated(EnumType.STRING)
  var prisonerStatus: PrisonerStatus = PrisonerStatus.ACTIVE,

  var startDate: LocalDate,

  var allocatedTime: LocalDateTime,

  var allocatedBy: String,

  @Transient
  private val initialPayBand: PrisonPayBand?,
) {
  @OneToOne
  @JoinColumn(name = "prison_pay_band_id")
  var payBand: PrisonPayBand? = null
    set(value) {
      if (activitySchedule.isPaid()) requireNotNull(value) { "Pay band must be provided for paid activity ID '${activitySchedule.activity.activityId}'" }
      if (!activitySchedule.isPaid()) require(value == null) { "Pay band must not be provided for unpaid activity ID '${activitySchedule.activity.activityId}'" }

      field = value
    }

  init {
    payBand = initialPayBand
  }

  var endDate: LocalDate? = null
    set(value) {
      require(value == null || value >= startDate || prisonerStatus == PrisonerStatus.ENDED) {
        "Allocation end date for prisoner $prisonerNumber cannot be before allocation start date."
      }

      field = value.also { updatePlannedDeallocation(it) }
    }

  private fun updatePlannedDeallocation(newEndDate: LocalDate?) {
    if (newEndDate == null) {
      plannedDeallocation = null
    } else {
      plannedDeallocation?.apply {
        if (plannedDate.isAfter(newEndDate)) {
          plannedDate = newEndDate
        }
      }
    }
  }

  @OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true)
  @JoinColumn(name = "planned_deallocation_id", nullable = true)
  var plannedDeallocation: PlannedDeallocation? = null
    private set

  @OneToMany(mappedBy = "allocation", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  private val exclusions: MutableSet<Exclusion> = mutableSetOf()

  var deallocatedTime: LocalDateTime? = null
    private set

  var deallocatedBy: String? = null
    private set

  @Enumerated(EnumType.STRING)
  var deallocatedReason: DeallocationReason? = null
    private set

  var suspendedTime: LocalDateTime? = null
    private set

  var suspendedBy: String? = null
    private set

  var suspendedReason: String? = null
    private set

  fun activeExclusions() = exclusions.filter { it.endDate == null }.toSet()

  fun exclusionsOnDate(date: LocalDate) = exclusions.filter { it.startDate <= date && (it.endDate == null || it.endDate!! >= date) }.toSet()

  fun presentExclusions() = exclusionsOnDate(LocalDate.now())

  fun futureExclusions() = exclusions.filter { it.startDate > LocalDate.now() }.toSet()

  fun removeExclusions(exclusionsToRemove: Set<Exclusion>) = exclusions.removeAll(exclusionsToRemove)

  fun removeExclusion(exclusion: Exclusion) = exclusions.remove(exclusion)

  fun endExclusions(exclusionsToEnd: Set<Exclusion>) = exclusionsToEnd.forEach { it.endNow() }

  fun prisonCode() = activitySchedule.activity.prisonCode

  private fun activitySummary() = activitySchedule.activity.summary

  /**
   * This will also check the planned end date should the end date be different or null.
   */
  fun ends(date: LocalDate) = date == endDate || date == plannedDeallocation?.plannedDate

  fun deallocateOn(date: LocalDate, reason: DeallocationReason, deallocatedBy: String) =
    this.apply {
      if (prisonerStatus == PrisonerStatus.ENDED) throw IllegalStateException("Allocation with ID '$allocationId' is already deallocated.")
      if (date.isBefore(LocalDate.now())) throw IllegalArgumentException("Planned deallocation date must not be in the past.")
      if (activitySchedule.endDate != null && date.isAfter(activitySchedule.endDate)) throw IllegalArgumentException("Planned deallocation date cannot be after activity schedule end date, ${activitySchedule.endDate}.")

      if (plannedDeallocation == null) {
        plannedDeallocation = PlannedDeallocation(
          allocation = this,
          plannedReason = reason,
          plannedDate = date,
          plannedBy = deallocatedBy,
        )
      } else {
        plannedDeallocation?.apply {
          plannedReason = reason
          plannedDate = date
          plannedBy = deallocatedBy
          plannedAt = LocalDateTime.now()
        }
      }
    }

  private fun maybeEndDate() =
    when {
      endDate != null -> endDate
      activitySchedule.endDate != null -> activitySchedule.endDate
      activitySchedule.activity.endDate != null -> activitySchedule.activity.endDate
      else -> null
    }

  fun deallocateNowWithReason(reason: DeallocationReason) =
    this.apply {
      if (prisonerStatus == PrisonerStatus.ENDED) throw IllegalStateException("Allocation with ID '$allocationId' is already deallocated.")

      prisonerStatus = PrisonerStatus.ENDED
      deallocatedReason = reason
      deallocatedBy = ServiceName.SERVICE_NAME.value
      deallocatedTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
      endDate = LocalDate.now()

      endExclusions(presentExclusions())
      removeExclusions(futureExclusions())
    }

  /**
   * This will default to ENDED for the reason unless there is planned deallocation that matches now which overrides it.
   */
  fun deallocateNow() =
    this.apply {
      if (prisonerStatus == PrisonerStatus.ENDED) throw IllegalStateException("Allocation with ID '$allocationId' is already deallocated.")

      val today = LocalDate.now()

      if (plannedDeallocation != null && plannedDeallocation?.plannedDate == today) {
        prisonerStatus = PrisonerStatus.ENDED
        deallocatedReason = plannedDeallocation?.plannedReason
        deallocatedBy = plannedDeallocation?.plannedBy
        deallocatedTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
        endDate = today
      } else {
        prisonerStatus = PrisonerStatus.ENDED
        deallocatedReason = DeallocationReason.ENDED
        deallocatedBy = ServiceName.SERVICE_NAME.value
        deallocatedTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
        endDate = today
      }

      endExclusions(presentExclusions())
      removeExclusions(futureExclusions())
    }

  fun status(vararg status: PrisonerStatus) = status.any { it == prisonerStatus }

  fun allocationPay(incentiveLevelCode: String) =
    payBand?.let { activitySchedule.activity.activityPayFor(it, incentiveLevelCode) }

  fun toModel() =
    ModelAllocation(
      id = allocationId,
      prisonerNumber = prisonerNumber,
      bookingId = bookingId,
      prisonPayBand = payBand?.toModel(),
      startDate = startDate,
      endDate = plannedDeallocation?.plannedDate ?: endDate,
      allocatedTime = allocatedTime,
      allocatedBy = allocatedBy,
      activitySummary = activitySummary(),
      activityId = activitySchedule.activity.activityId,
      scheduleId = activitySchedule.activityScheduleId,
      scheduleDescription = activitySchedule.description,
      isUnemployment = activitySchedule.activity.isUnemployment(),
      deallocatedBy = deallocatedBy,
      deallocatedReason = deallocatedReason?.toModel(),
      deallocatedTime = deallocatedTime,
      suspendedBy = suspendedBy,
      suspendedReason = suspendedReason,
      suspendedTime = suspendedTime,
      status = prisonerStatus,
      plannedDeallocation = plannedDeallocation?.toModel(),
      exclusions = activeExclusions().toSlotModel(),
    )

  fun isExcluded(date: LocalDate, timeSlot: TimeSlot) =
    exclusionsOnDate(date).any {
      date.dayOfWeek in it.getDaysOfWeek() &&
        timeSlot == it.timeSlot() &&
        activitySchedule.getWeekNumber(date) == it.weekNumber
    }

  fun activate() =
    this.apply {
      failWithMessageIfAllocationsIsNot("You can only activate pending allocations", PrisonerStatus.PENDING)

      prisonerStatus = PrisonerStatus.ACTIVE
    }

  /**
   * Only active and pending allocations can be auto-suspended.
   */
  fun autoSuspend(dateTime: LocalDateTime, reason: String) =
    this.apply {
      failWithMessageIfAllocationsIsNot("You can only auto-suspend active and pending allocations", PrisonerStatus.ACTIVE, PrisonerStatus.PENDING)

      prisonerStatus = PrisonerStatus.AUTO_SUSPENDED
      suspendedTime = dateTime
      suspendedReason = reason
      suspendedBy = ServiceName.SERVICE_NAME.value
    }

  fun userSuspend(dateTime: LocalDateTime, reason: String, byWhom: String) =
    this.apply {
      failWithMessageIfAllocationsIsNot("You can only suspend active allocations", PrisonerStatus.ACTIVE)

      prisonerStatus = PrisonerStatus.SUSPENDED
      suspendedTime = dateTime
      suspendedReason = reason
      suspendedBy = byWhom
    }

  fun reactivateAutoSuspensions() =
    this.apply {
      failWithMessageIfAllocationsIsNot(
        "You can only reactivate auto-suspended allocations",
        PrisonerStatus.AUTO_SUSPENDED,
      )
      prisonerStatus = PrisonerStatus.ACTIVE
      suspendedTime = null
      suspendedReason = null
      suspendedBy = null
    }

  private fun failWithMessageIfAllocationsIsNot(failureMessage: String, vararg statuses: PrisonerStatus) {
    if (status(*statuses).not()) {
      throw IllegalStateException(failureMessage)
    }
  }

  fun isEnded() = status(PrisonerStatus.ENDED)

  fun updateExclusion(slot: ActivityScheduleSlot, daysOfWeek: Set<DayOfWeek>): Exclusion? {
    require(slot.getDaysOfWeek().containsAll(daysOfWeek)) {
      "Cannot set exclusions for slot with id ${slot.activityScheduleSlotId} where the activity does not run"
    }

    val exclusion = futureExclusions()
      .singleOrNull { it.weekNumber == slot.weekNumber && it.timeSlot() == slot.timeSlot() }
      ?.apply { setDaysOfWeek(daysOfWeek) }
      ?: Exclusion.valueOf(this, slot.startTime, slot.weekNumber, daysOfWeek)

    return if (exclusion.getDaysOfWeek().isNotEmpty()) {
      // TODO: The following requirement is temporary, for as long as we need to sync events of this service back to nomis.
      //  This is to respect a restraint on the nomis data model
      require(
        activeExclusions().none {
          exclusion.weekNumber != it.weekNumber &&
            exclusion.timeSlot() == it.timeSlot() &&
            exclusion.getDaysOfWeek().intersect(it.getDaysOfWeek()).isNotEmpty()
        },
      ) { "Exclusions cannot be added for the same day and time slot over multiple weeks." }

      addExclusion(exclusion)
      exclusions.last()
    } else {
      removeExclusion(exclusion)
      null
    }
  }

  /**
   * Returns true if the date is between the start and end date, no clashing exclusions and not ended, otherwise false.
   */
  fun canAttendOn(date: LocalDate, timeSlot: TimeSlot) = date.between(startDate, maybeEndDate()) && isExcluded(date, timeSlot).not() && prisonerStatus != PrisonerStatus.ENDED

  fun syncExclusionsWithScheduleSlots(scheduleSlots: List<Slot>): Long? {
    var endedSome: Boolean
    val scheduleSlotPairs = scheduleSlots.map { it.weekNumber to it.timeSlot() }
    presentExclusions()
      .filter { it.weekNumber to it.timeSlot() !in scheduleSlotPairs }
      .let {
        endedSome = it.isNotEmpty()
        endExclusions(it.toSet())
      }
    futureExclusions()
      .filter { it.weekNumber to it.timeSlot() !in scheduleSlotPairs }
      .let {
        endedSome = it.isNotEmpty() || endedSome
        removeExclusions(it.toSet())
      }

    var editedSome = false
    presentExclusions().forEach {
      val matchingSlot = scheduleSlots.singleOrNull { slot -> slot.weekNumber == it.weekNumber && slot.timeSlot() == it.timeSlot() }
      if (matchingSlot != null && !matchingSlot.getDaysOfWeek().containsAll(it.getDaysOfWeek())) {
        editedSome = true
        it.endNow()
        val intersect = matchingSlot.getDaysOfWeek().intersect(it.getDaysOfWeek())
        if (intersect.isNotEmpty()) { addExclusion(Exclusion.valueOf(this, it.slotStartTime, it.weekNumber, intersect)) }
      }
    }
    futureExclusions().forEach {
      val matchingSlot = scheduleSlots.singleOrNull { slot -> slot.weekNumber == it.weekNumber && slot.timeSlot() == it.timeSlot() }
      if (matchingSlot != null && !matchingSlot.getDaysOfWeek().containsAll(it.getDaysOfWeek())) {
        editedSome = true
        val intersect = matchingSlot.getDaysOfWeek().intersect(it.getDaysOfWeek())
        if (intersect.isEmpty()) { removeExclusion(it) } else { it.setDaysOfWeek(intersect) }
      }
    }

    return if (endedSome || editedSome) allocationId else null
  }

  fun addExclusion(exclusion: Exclusion) = run {
    require(
      activeExclusions().none { it.slotStartTime == exclusion.slotStartTime && it.weekNumber == exclusion.weekNumber } || activeExclusions().contains(exclusion),
    ) {
      "Failed to add exclusion to allocation with Id $allocationId, because an active exclusion for the same slot already exists"
    }

    exclusions.add(exclusion)
  }

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(allocationId = $allocationId )"
  }
}

enum class PrisonerStatus {
  ACTIVE, PENDING, SUSPENDED, AUTO_SUSPENDED, ENDED;

  companion object {
    fun allExcuding(vararg status: PrisonerStatus) = entries.filterNot { status.contains(it) }.toTypedArray()
  }
}

enum class DeallocationReason(val description: String, val displayed: Boolean = false) {
  // System reasons
  DIED("Deceased"),
  ENDED("Allocation end date reached"),
  PLANNED("Duration set by staff"),
  EXPIRED("Expired"),
  RELEASED("Released from prison"),
  TEMPORARILY_RELEASED("Temporarily released or transferred"),

  // Displayed reasons
  COMPLETED("Completed course or task", true),
  TRANSFERRED("Transferred to another activity", true),
  WITHDRAWN_STAFF("Withdrawn by staff", true),
  WITHDRAWN_OWN("Withdrawn at own request", true),
  HEALTH("Health", true),
  SECURITY("Security", true),
  OTHER("Other", true),
  ;

  fun toModel() = ModelDeallocationReason(name, description)

  companion object {
    fun toModelDeallocationReasons() =
      entries.filter(DeallocationReason::displayed).map(DeallocationReason::toModel)
  }
}
