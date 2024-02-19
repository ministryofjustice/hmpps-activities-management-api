package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.onOrBefore
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "planned_suspension")
data class PlannedSuspension(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private val plannedSuspensionId: Long = 0,

  @ManyToOne
  @JoinColumn(name = "allocation_id", nullable = false)
  val allocation: Allocation,

  private var plannedStartDate: LocalDate,
  private var plannedReason: String,
  private var plannedBy: String,
  private var plannedAt: LocalDateTime = LocalDateTime.now(),
  private var plannedEndDate: LocalDate? = null,
) {
  private var updatedBy: String? = null
  private var updatedAt: LocalDateTime? = null

  fun startDate() = plannedStartDate
  fun endDate() = plannedEndDate
  fun plannedReason() = plannedReason
  fun plannedBy() = plannedBy

  fun hasStarted() = plannedStartDate.onOrBefore(LocalDate.now())
  fun endOn(date: LocalDate, byWhom: String) = apply {
    plannedEndDate = date
    updatedBy = byWhom
    updatedAt = LocalDateTime.now()
  }
  fun endNow(byWhom: String) {
    plannedEndDate = LocalDate.now()
    updatedAt = LocalDateTime.now()
    updatedBy = byWhom
  }
  fun plan(reason: String, startDate: LocalDate, byWhom: String) = apply {
    plannedReason = reason
    plannedStartDate = startDate
    updatedBy = byWhom
    updatedAt = LocalDateTime.now()
  }
}
