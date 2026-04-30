package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.time.LocalDate
import java.util.UUID

@Entity
@Immutable
@Table(name = "v_sar_allocation")
data class SarAllocation(
  @Id
  val allocationId: Long,
  val prisonCode: String,
  val prisonerNumber: String,
  val prisonerStatus: String,
  val startDate: LocalDate,
  val endDate: LocalDate?,
  val activityId: Long,
  val activitySummary: String,
  val payBand: String?,
  val createdDate: LocalDate,
  val activityCategoryName: String,
  val activityCategoryDescription: String,
  val attendanceRequired: Boolean,
  val paid: Boolean,
  val outsideWork: Boolean,
  val riskLevel: String,
  val organiser: String?,
  val dpsLocationId: UUID?,
  val inCell: Boolean,
  val offWing: Boolean,
  val onWing: Boolean,
)
