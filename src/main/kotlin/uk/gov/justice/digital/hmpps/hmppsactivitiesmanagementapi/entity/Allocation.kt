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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.containsAny
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.isAfterDates
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.PrisonPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.enumeration.ServiceName
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Slot
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation as ModelAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.DeallocationReason as ModelDeallocationReason

@Entity
@Table(name = "allocation")
@EntityListeners(AuditableEntityListener::class)
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

  @OneToMany(mappedBy = "allocation", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  private val exclusions: MutableSet<Exclusion> = mutableSetOf()

  @OneToMany(mappedBy = "allocation", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  private val plannedSuspensions: MutableSet<PlannedSuspension> = mutableSetOf()

  var deallocatedTime: LocalDateTime? = null
    private set

  var deallocatedBy: String? = null
    private set

  @Enumerated(EnumType.STRING)
  var deallocatedReason: DeallocationReason? = null
    private set

  var deallocationCaseNoteId: Long? = null
    private set

  var deallocationDpsCaseNoteId: UUID? = null
    private set

  var suspendedTime: LocalDateTime? = null
    private set

  var suspendedBy: String? = null
    private set

  var suspendedReason: String? = null
    private set

  fun plannedSuspension() = plannedSuspensions.singleOrNull { it.endDate()?.isAfterDates(LocalDate.now(), it.startDate()) ?: true }

  fun isCurrentlySuspended() = plannedSuspension()?.hasStarted() == true

  fun isCurrentlyPaidSuspension() = plannedSuspension()?.hasStarted() ?: false && plannedSuspension()?.paid() ?: false

  fun addPlannedSuspension(suspension: PlannedSuspension) = run {
    require(suspension.allocation == this) { "Cannot add this suspension associated to the allocation with id ${suspension.allocation.allocationId} to allocation with id $allocationId" }
    require(plannedSuspension() == null) { "Cannot add this planned suspension to allocation with id $allocationId because another planned suspension already exists" }
    plannedSuspensions.add(suspension)
  }

  fun exclusions(filter: ExclusionsFilter) = filter.filtered(exclusions)

  private fun exclusionsOnDate(date: LocalDate) = exclusions.filter { date.between(it.startDate, it.endDate) }.toSet()

  fun removeExclusions(exclusionsToRemove: Set<Exclusion>) = run {
    require(exclusionsToRemove.all { it.allocation == this }) { "Cannot remove the given exclusions because some of them do not belong to the allocation with id $allocationId" }
    exclusions.removeAll(exclusionsToRemove)
  }

  private fun removeExclusion(exclusion: Exclusion) = run {
    require(exclusion.allocation == this) { "Cannot remove the given exclusion because it does not belong to the allocation with id $allocationId" }
    exclusions.remove(exclusion)
  }

  private fun removeAllAdvanceAttendances() = this.activitySchedule.instances().forEach { scheduledInstance ->
    scheduledInstance.advanceAttendances
      .filter { it.prisonerNumber == this.prisonerNumber }
      .forEach { advanceAttendance -> scheduledInstance.advanceAttendances.remove(advanceAttendance) }
  }

  fun removeRedundantAdvanceAttendances(startDate: LocalDate, endDate: LocalDate? = null) = this.activitySchedule.instances()
    .filter { scheduledInstance -> scheduledInstance.sessionDate < startDate || (endDate != null && scheduledInstance.sessionDate > endDate) }
    .forEach { scheduledInstance ->
      scheduledInstance.advanceAttendances
        .filter { advanceAttendance -> advanceAttendance.prisonerNumber == this.prisonerNumber }
        .forEach { advanceAttendance -> scheduledInstance.advanceAttendances.remove(advanceAttendance) }
    }

  fun endExclusions(exclusionsToEnd: Set<Exclusion>) = run {
    require(exclusionsToEnd.all { it.allocation == this }) { "Cannot end the given exclusions because some of them do not belong to the allocation with id $allocationId" }
    exclusionsToEnd.forEach { it.endNow() }
  }

  fun prisonCode() = activitySchedule.activity.prisonCode

  private fun activitySummary() = activitySchedule.activity.summary

  /**
   * This will also check the planned end date should the end date be different or null.
   */
  fun endsOn(date: LocalDate) = date == endDate || date == plannedDeallocation?.plannedDate

  fun deallocateOn(date: LocalDate, reason: DeallocationReason, deallocatedBy: String, dpsCaseNoteId: UUID? = null) = this.apply {
    if (prisonerStatus == PrisonerStatus.ENDED) throw IllegalStateException("Allocation with ID '$allocationId' is already deallocated.")
    if (date.isBefore(LocalDate.now())) throw IllegalArgumentException("Planned deallocation date must not be in the past.")
    if (activitySchedule.endDate != null && date.isAfter(activitySchedule.endDate)) throw IllegalArgumentException("Planned deallocation date cannot be after activity schedule end date, ${activitySchedule.endDate}.")

    if (plannedDeallocation == null) {
      plannedDeallocation = PlannedDeallocation(
        allocation = this,
        plannedReason = reason,
        plannedDate = date,
        plannedBy = deallocatedBy,
        dpsCaseNoteId = dpsCaseNoteId,
      )
    } else {
      plannedDeallocation?.apply {
        plannedReason = reason
        plannedDate = date
        plannedBy = deallocatedBy
        plannedAt = LocalDateTime.now()
        this.dpsCaseNoteId = dpsCaseNoteId
      }
    }
  }

  fun plannedEndDate() = plannedDeallocation?.plannedDate ?: endDate

  private fun maybeEndDate() = when {
    endDate != null -> endDate
    plannedDeallocation?.plannedDate != null -> plannedDeallocation?.plannedDate
    activitySchedule.endDate != null -> activitySchedule.endDate
    activitySchedule.activity.endDate != null -> activitySchedule.activity.endDate
    else -> null
  }

  fun deallocateNowWithReason(reason: DeallocationReason) = this.apply {
    if (prisonerStatus == PrisonerStatus.ENDED) throw IllegalStateException("Allocation with ID '$allocationId' is already deallocated.")

    prisonerStatus = PrisonerStatus.ENDED
    deallocatedReason = reason
    deallocatedBy = ServiceName.SERVICE_NAME.value
    deallocatedTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
    endDate = LocalDate.now()

    endExclusions(exclusions(ExclusionsFilter.PRESENT))
    removeExclusions(exclusions(ExclusionsFilter.FUTURE))
    removeAllAdvanceAttendances()
  }

  fun deallocateBeforeStart(reason: DeallocationReason, by: String) = this.apply {
    if (prisonerStatus == PrisonerStatus.ENDED) throw IllegalStateException("Allocation with ID '$allocationId' is already deallocated.")

    prisonerStatus = PrisonerStatus.ENDED
    deallocatedReason = reason
    deallocatedBy = by
    deallocatedTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
    endDate = startDate

    endExclusions(exclusions(ExclusionsFilter.PRESENT))
    removeExclusions(exclusions(ExclusionsFilter.FUTURE))
    removeAllAdvanceAttendances()
  }

  /**
   * This will default to ENDED for the reason unless there is planned deallocation that matches now which overrides it.
   *
   * Deallocation date cannot be in the future, this is deallocating/ending the allocation.
   */
  fun deallocateNowOn(date: LocalDate) = this.apply {
    if (prisonerStatus == PrisonerStatus.ENDED) throw IllegalStateException("Allocation with ID '$allocationId' is already deallocated.")

    require(date <= LocalDate.now()) {
      "Allocation '$allocationId' cannot be deallocated with the future date '$date'"
    }

    if (plannedDeallocation != null && plannedDeallocation?.plannedDate == date) {
      prisonerStatus = PrisonerStatus.ENDED
      deallocatedReason = plannedDeallocation?.plannedReason
      deallocatedBy = plannedDeallocation?.plannedBy
      deallocatedTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
      deallocationCaseNoteId = plannedDeallocation?.caseNoteId
      deallocationDpsCaseNoteId = plannedDeallocation?.dpsCaseNoteId
      endDate = date
    } else {
      prisonerStatus = PrisonerStatus.ENDED
      deallocatedReason = DeallocationReason.ENDED
      deallocatedBy = ServiceName.SERVICE_NAME.value
      deallocatedTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
      endDate = date
    }

    endExclusions(exclusions(ExclusionsFilter.PRESENT))
    removeExclusions(exclusions(ExclusionsFilter.FUTURE))
    removeAllAdvanceAttendances()
  }

  fun status(vararg status: PrisonerStatus) = status.any { it == prisonerStatus }

  fun allocationPay(incentiveLevelCode: String) = payBand?.let { activitySchedule.activity.activityPayFor(it, incentiveLevelCode) }

  fun toModel() = ModelAllocation(
    id = allocationId,
    prisonerNumber = prisonerNumber,
    bookingId = bookingId,
    prisonPayBand = payBand?.toModel(),
    startDate = startDate,
    endDate = plannedEndDate(),
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
    plannedSuspension = plannedSuspension()?.toModel(),
    exclusions = exclusions(ExclusionsFilter.ACTIVE).toSlotModel(),
  )

  private fun isExcluded(date: LocalDate, timeSlot: TimeSlot) = exclusionsOnDate(date).any {
    date.dayOfWeek in it.getDaysOfWeek() &&
      timeSlot == it.timeSlot &&
      activitySchedule.getWeekNumber(date) == it.weekNumber
  }

  fun activate() = this.apply {
    failWithMessageIfAllocationIsNotStatus("You can only activate pending allocations", PrisonerStatus.PENDING)

    prisonerStatus = PrisonerStatus.ACTIVE
  }

  /**
   * Only active and pending allocations can be auto-suspended.
   */
  fun autoSuspend(dateTime: LocalDateTime, reason: String) = this.apply {
    failWithMessageIfAllocationIsNotStatus("You can only auto-suspend active, pending or suspended allocations", PrisonerStatus.ACTIVE, PrisonerStatus.PENDING, PrisonerStatus.SUSPENDED)

    prisonerStatus = PrisonerStatus.AUTO_SUSPENDED
    suspendedTime = dateTime
    suspendedReason = reason
    suspendedBy = ServiceName.SERVICE_NAME.value
  }

  fun activatePlannedSuspension(prisonStatus: PrisonerStatus = PrisonerStatus.SUSPENDED) = this.apply {
    val plannedSuspension = plannedSuspension()
    require(plannedSuspension != null && plannedSuspension.hasStarted()) { "Failed to activate planned suspension for allocation with id $allocationId - no suspensions planned at this time" }
    failWithMessageIfAllocationIsNotStatus("You can only suspend active or auto-suspended allocations", PrisonerStatus.ACTIVE, PrisonerStatus.AUTO_SUSPENDED)

    if (plannedSuspension.paid() == true) {
      prisonerStatus = PrisonerStatus.SUSPENDED_WITH_PAY
    } else {
      prisonerStatus = prisonStatus
    }
    suspendedTime = LocalDateTime.now()
    suspendedReason = "Planned suspension"
    suspendedBy = plannedSuspension.plannedBy()
  }

  fun reactivateSuspension() = this.apply {
    failWithMessageIfAllocationIsNotStatus(
      "You can only reactivate suspended, suspended with pay or auto-suspended allocations",
      PrisonerStatus.SUSPENDED,
      PrisonerStatus.AUTO_SUSPENDED,
      PrisonerStatus.SUSPENDED_WITH_PAY,
    )
    prisonerStatus = PrisonerStatus.ACTIVE
    suspendedTime = null
    suspendedReason = null
    suspendedBy = null
  }

  private fun failWithMessageIfAllocationIsNotStatus(failureMessage: String, vararg statuses: PrisonerStatus) {
    if (status(*statuses).not()) {
      throw IllegalStateException(failureMessage)
    }
  }

  fun isEnded() = status(PrisonerStatus.ENDED)

  fun updateExclusion(exclusionSlot: Slot, startDate: LocalDate): Exclusion? {
    val exclusion = exclusions(ExclusionsFilter.FUTURE)
      .singleOrNull { it.weekNumber == exclusionSlot.weekNumber && it.timeSlot == exclusionSlot.timeSlot }
      ?.apply { setDaysOfWeek(exclusionSlot.daysOfWeek) }
      ?: Exclusion.valueOf(
        allocation = this,
        weekNumber = exclusionSlot.weekNumber,
        daysOfWeek = exclusionSlot.daysOfWeek,
        startDate = startDate,
        timeSlot = exclusionSlot.timeSlot,
      )

    return if (exclusion.getDaysOfWeek().isNotEmpty()) {
      if (exclusions.contains(exclusion).not()) addExclusion(exclusion)
      exclusion
    } else {
      removeExclusion(exclusion)
      null
    }
  }

  /**
   * Returns true if the date is between the start and end date, no clashing exclusions and not ended, otherwise false.
   */
  fun canAttendOn(date: LocalDate, timeSlot: TimeSlot) = date.between(startDate, maybeEndDate()) &&
    isExcluded(
      date = date,
      timeSlot = timeSlot,
    ).not() &&
    prisonerStatus != PrisonerStatus.ENDED

  fun syncExclusionsWithScheduleSlots(scheduleSlots: List<ActivityScheduleSlot>): Long? {
    var editedSome: Boolean
    val scheduleSlotPairs = scheduleSlots.map { it.weekNumber to it.timeSlot }
    exclusions(ExclusionsFilter.PRESENT)
      .filter { it.weekNumber to it.timeSlot !in scheduleSlotPairs }
      .let {
        editedSome = it.isNotEmpty()
        endExclusions(it.toSet())
      }
    exclusions(ExclusionsFilter.FUTURE)
      .filter { it.weekNumber to it.timeSlot !in scheduleSlotPairs }
      .let {
        editedSome = it.isNotEmpty() || editedSome
        removeExclusions(it.toSet())
      }

    // TODO: The disallowedExclusionDays in the following blocks are temporary, for as long as we need to sync events of this service back to nomis.
    //  This is to respect a restraint on the nomis data model. Exclusions on any slot which has a matching slot over multiple weeks
    //  must be ended or removed.

    exclusions(ExclusionsFilter.PRESENT).filter { it.endDate == null }.forEach {
      val matchingSlots = scheduleSlots.filter { slot -> slot.weekNumber == it.weekNumber && slot.timeSlot == it.timeSlot && slot.getDaysOfWeek().any { dow -> it.getDaysOfWeek().contains(dow) } }
      val slotDaysOfWeek = matchingSlots.flatMap { it.getDaysOfWeek() }.toSet()
      val matchingSlotsInOtherWeeks = scheduleSlots.filter { slot -> slot.weekNumber != it.weekNumber && slot.timeSlot == it.timeSlot }
      val disallowedExclusionDays = matchingSlotsInOtherWeeks.flatMap { slot -> slot.getDaysOfWeek() }.toSet()

      if (it.getDaysOfWeek().containsAny(disallowedExclusionDays) || !slotDaysOfWeek.containsAll(it.getDaysOfWeek())) {
        editedSome = true
        it.endNow()
        val intersect = it.getDaysOfWeek().intersect(slotDaysOfWeek).subtract(disallowedExclusionDays)
        if (intersect.isNotEmpty()) {
          addExclusion(
            Exclusion.valueOf(
              allocation = this,
              weekNumber = it.weekNumber,
              daysOfWeek = intersect,
              timeSlot = it.timeSlot,
            ),
          )
        }
      }
    }
    exclusions(ExclusionsFilter.FUTURE).forEach {
      val matchingSlots = scheduleSlots.filter { slot -> slot.weekNumber == it.weekNumber && slot.timeSlot == it.timeSlot && slot.getDaysOfWeek().any { dow -> it.getDaysOfWeek().contains(dow) } }
      val slotDaysOfWeek = matchingSlots.flatMap { it.getDaysOfWeek() }.toSet()
      val matchingSlotsInOtherWeeks = scheduleSlots.filter { slot -> slot.weekNumber != it.weekNumber && slot.timeSlot == it.timeSlot }
      val disallowedExclusionDays = matchingSlotsInOtherWeeks.flatMap { slot -> slot.getDaysOfWeek() }.toSet()
      if (it.getDaysOfWeek().containsAny(disallowedExclusionDays) || !slotDaysOfWeek.containsAll(it.getDaysOfWeek())) {
        editedSome = true
        val intersect = it.getDaysOfWeek().intersect(slotDaysOfWeek).subtract(disallowedExclusionDays)
        if (intersect.isNotEmpty()) {
          it.setDaysOfWeek(intersect)
        } else {
          removeExclusion(it)
        }
      }
    }

    return if (editedSome) allocationId else null
  }

  fun addExclusion(exclusion: Exclusion) = run {
    require(
      activitySchedule.hasMatchingSlots(exclusion),
    ) {
      "Cannot set exclusions where the activity does not run"
    }

    require(
      exclusions(ExclusionsFilter.ACTIVE).none { it.timeSlot == exclusion.timeSlot && it.weekNumber == exclusion.weekNumber },
    ) {
      "Failed to add exclusion to allocation with Id $allocationId, because an active exclusion for the same slot already exists"
    }

    exclusions.add(exclusion)
  }

  @Override
  override fun toString(): String = this::class.simpleName + "(allocationId = $allocationId )"
}

enum class PrisonerStatus {
  ACTIVE,
  PENDING,
  SUSPENDED,
  AUTO_SUSPENDED,
  ENDED,
  SUSPENDED_WITH_PAY,
  ;

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
  DISMISSED("Dismissed", true),
  HEALTH("Health", true),
  SECURITY("Security", true),
  OTHER("Other", true),
  ;

  fun toModel() = ModelDeallocationReason(name, description)

  companion object {
    fun displayedDeallocationReasons() = entries.filter(DeallocationReason::displayed)
    fun toModelDeallocationReasons() = displayedDeallocationReasons().map(DeallocationReason::toModel)
  }
}

enum class ExclusionsFilter(private val f: (Collection<Exclusion>) -> Set<Exclusion>) {
  ACTIVE({ exclusions -> exclusions.filter { it.endDate == null }.toSet() }),
  PRESENT({ exclusions -> exclusions.filter { LocalDate.now().between(it.startDate, it.endDate) }.toSet() }),
  FUTURE({ exclusions -> exclusions.filter { it.startDate.isAfter(LocalDate.now()) }.toSet() }),
  ;

  fun filtered(exclusions: Collection<Exclusion>) = f(exclusions)
}
