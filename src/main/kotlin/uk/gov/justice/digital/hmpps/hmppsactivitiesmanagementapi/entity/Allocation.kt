package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.between
import java.time.LocalDate
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation as ModelAllocation

@Entity
@Table(name = "allocation")
@EntityListeners(AllocationEntityListener::class)
data class Allocation(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val allocationId: Long = -1,

  @ManyToOne
  @JoinColumn(name = "activity_schedule_id", nullable = false)
  val activitySchedule: ActivitySchedule,

  val prisonerNumber: String,

  val bookingId: Long? = null,

  @OneToOne
  @JoinColumn(name = "prison_pay_band_id")
  var payBand: PrisonPayBand,

  var startDate: LocalDate,

  var endDate: LocalDate? = null,

  var allocatedTime: LocalDateTime,

  var allocatedBy: String,

  var deallocatedTime: LocalDateTime? = null,

  var deallocatedBy: String? = null,

  var deallocatedReason: String? = null,
) {
  fun isActive(date: LocalDate) = date.between(startDate, endDate)

  fun isUnemployment() = activitySchedule.activity.activityCategory.code == "SAA_NOT_IN_WORK"

  fun activitySummary() = activitySchedule.activity.summary

  fun scheduleDescription() = activitySchedule.description

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
      isUnemployment = isUnemployment(),
    )
}
