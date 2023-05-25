package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.onOrBefore
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.enumeration.ServiceName
import java.time.LocalDate
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation as ModelAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.DeallocationReason as ModelDeallocationReason

@Entity
@Table(name = "allocation")
@EntityListeners(AllocationEntityListener::class, AuditableListener::class)
data class Allocation(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val allocationId: Long = 0,

  @ManyToOne
  @JoinColumn(name = "activity_schedule_id", nullable = false)
  val activitySchedule: ActivitySchedule,

  val prisonerNumber: String,

  val bookingId: Long,

  @OneToOne
  @JoinColumn(name = "prison_pay_band_id")
  var payBand: PrisonPayBand,

  var startDate: LocalDate,

  var endDate: LocalDate? = null,

  var allocatedTime: LocalDateTime,

  var allocatedBy: String,
) {

  @OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true)
  @JoinColumn(name = "planned_deallocation_id", nullable = true)
  var plannedDeallocation: PlannedDeallocation? = null
    private set

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

  @Enumerated(EnumType.STRING)
  var prisonerStatus: PrisonerStatus = PrisonerStatus.ACTIVE
    private set

  private fun activitySummary() = activitySchedule.activity.summary

  fun ends(date: LocalDate) = date == endDate

  fun deallocateOn(date: LocalDate, reason: DeallocationReason, deallocatedBy: String) {
    this.apply {
      if (prisonerStatus == PrisonerStatus.ENDED) throw IllegalStateException("Allocation with ID '$allocationId' is already deallocated.")
      if (plannedDeallocation != null) throw IllegalStateException("Allocation with ID '$allocationId' is already planned.")
      if (date.onOrBefore(LocalDate.now())) throw IllegalArgumentException("Planned deallocation date must be in the future.")
      if (maybeEndDate() != null && date.isAfter(maybeEndDate())) throw IllegalArgumentException("Planned date cannot be after ${maybeEndDate()}.")

      plannedDeallocation = PlannedDeallocation(
        allocation = this,
        plannedReason = reason,
        plannedDate = date,
        plannedBy = deallocatedBy,
      )
    }
  }

  private fun maybeEndDate() =
    when {
      endDate != null -> endDate
      activitySchedule.endDate != null -> activitySchedule.endDate
      activitySchedule.activity.endDate != null -> activitySchedule.activity.endDate
      else -> null
    }

  // TODO remove date/time timestamp from function signature
  fun deallocateNow(dateTime: LocalDateTime, reason: DeallocationReason) =
    this.apply {
      if (prisonerStatus == PrisonerStatus.ENDED) throw IllegalStateException("Allocation with ID '$allocationId' is already deallocated.")

      prisonerStatus = PrisonerStatus.ENDED
      deallocatedReason = reason
      deallocatedBy = ServiceName.SERVICE_NAME.value
      deallocatedTime = dateTime
    }

  fun status(vararg status: PrisonerStatus) = status.any { it == prisonerStatus }

  fun toModel() =
    ModelAllocation(
      id = allocationId,
      prisonerNumber = prisonerNumber,
      bookingId = bookingId,
      prisonPayBand = payBand.toModel(),
      startDate = startDate,
      endDate = endDate,
      allocatedTime = allocatedTime,
      allocatedBy = allocatedBy,
      activitySummary = activitySummary(),
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
    )

  fun autoSuspend(dateTime: LocalDateTime, reason: String) =
    this.apply {
      failWithMessageIfAllocationsIsNot("You can only suspend active allocations", PrisonerStatus.ACTIVE)

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

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(allocationId = $allocationId )"
  }
}

enum class PrisonerStatus {
  ACTIVE, SUSPENDED, AUTO_SUSPENDED, ENDED
}

enum class DeallocationReason(val description: String) {
  DIED("Deceased"),
  ENDED("Allocation end date reached"),
  EXPIRED("Expired"),
  OTHER("Other"),
  PERSONAL("Personal reason"),
  PROBLEM("Problem understanding material"),
  RELEASED("Released from prison"),
  REMOVED("Removed"),
  SECURITY("Security"),
  TEMPORARY_ABSENCE("Temporary absence"),
  UNACCEPTABLE_ATTENDANCE("Unacceptable attendance"),
  UNACCEPTABLE_BEHAVIOUR("Unacceptable behaviour"),
  WITHDRAWN("Withdrawn"),
  ;

  fun toModel() = ModelDeallocationReason(name, description)

  companion object {
    fun toModelDeallocationReasons() = DeallocationReason.values().map(DeallocationReason::toModel)
  }
}
