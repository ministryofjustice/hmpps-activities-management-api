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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.enumeration.ServiceName
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

  @OneToOne
  @JoinColumn(name = "prison_pay_band_id")
  var payBand: PrisonPayBand,

  var startDate: LocalDate,

  var allocatedTime: LocalDateTime,

  var allocatedBy: String,

) {

  var endDate: LocalDate? = null
    set(value) {
      require(value == null || value >= startDate) {
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

  private fun activitySummary() = activitySchedule.activity.summary

  /**
   * This will also check the planned end date should the end date be different or null.
   */
  fun ends(date: LocalDate) = date == endDate || date == plannedDeallocation?.plannedDate

  fun deallocateOn(date: LocalDate, reason: DeallocationReason, deallocatedBy: String) =
    this.apply {
      if (prisonerStatus == PrisonerStatus.ENDED) throw IllegalStateException("Allocation with ID '$allocationId' is already deallocated.")
      if (date.isBefore(LocalDate.now())) throw IllegalArgumentException("Planned deallocation date must not be in the past.")
      if (maybeEndDate() != null && date.isAfter(maybeEndDate())) throw IllegalArgumentException("Planned date cannot be after ${maybeEndDate()}.")

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
    }

  fun status(vararg status: PrisonerStatus) = status.any { it == prisonerStatus }

  fun allocationPay(incentiveLevelCode: String) =
    activitySchedule.activity.activityPayFor(payBand, incentiveLevelCode)

  fun toModel() =
    ModelAllocation(
      id = allocationId,
      prisonerNumber = prisonerNumber,
      bookingId = bookingId,
      prisonPayBand = payBand.toModel(),
      startDate = startDate,
      endDate = plannedDeallocation?.plannedDate ?: endDate,
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
      plannedDeallocation = plannedDeallocation?.toModel(),
    )

  fun activate() =
    this.apply {
      failWithMessageIfAllocationsIsNot("You can only activate pending allocations", PrisonerStatus.PENDING)

      prisonerStatus = PrisonerStatus.ACTIVE
    }

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

  fun isEnded() = status(PrisonerStatus.ENDED)

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(allocationId = $allocationId )"
  }
}

enum class PrisonerStatus {
  ACTIVE, PENDING, SUSPENDED, AUTO_SUSPENDED, ENDED
}

enum class DeallocationReason(val description: String, val displayed: Boolean = false) {
  // System reasons
  DIED("Deceased"),
  ENDED("Allocation end date reached"),
  PLANNED("Allocation end date entered on initial allocation"),
  EXPIRED("Expired"),
  RELEASED("Released from prison"),
  TEMPORARY_ABSENCE("Temporary absence"),

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
      DeallocationReason.values().filter(DeallocationReason::displayed).map(DeallocationReason::toModel)
  }
}
