package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

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
  val allocationId: Long? = null,

  @ManyToOne
  @JoinColumn(name = "activity_schedule_id", nullable = false)
  val activitySchedule: ActivitySchedule,

  val prisonerNumber: String,

  var incentiveLevel: String? = null,

  var payBand: String? = null,

  var startDate: LocalDate? = null,

  var endDate: LocalDate? = null,

  var active: Boolean = true,

  var allocatedTime: LocalDateTime? = null,

  var allocatedBy: String? = null,

  var deallocatedTime: LocalDateTime? = null,

  var deallocatedBy: String? = null,

  var deallocatedReason: String? = null,
)
