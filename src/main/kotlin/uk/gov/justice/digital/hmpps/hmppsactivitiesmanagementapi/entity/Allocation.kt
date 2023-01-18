package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.between
import java.time.LocalDate
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "allocation")
data class Allocation(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val allocationId: Long = -1,

  @ManyToOne
  @JoinColumn(name = "activity_schedule_id", nullable = false)
  val activitySchedule: ActivitySchedule,

  val prisonerNumber: String,

  val bookingId: Long? = null,

  var payBand: String? = null,

  var startDate: LocalDate,

  var endDate: LocalDate? = null,

  var allocatedTime: LocalDateTime,

  var allocatedBy: String,

  var deallocatedTime: LocalDateTime? = null,

  var deallocatedBy: String? = null,

  var deallocatedReason: String? = null,
) {
  fun isActive(date: LocalDate) = date.between(startDate, endDate)

  fun activitySummary() = activitySchedule.activity.summary

  fun scheduleDescription() = activitySchedule.description
}
