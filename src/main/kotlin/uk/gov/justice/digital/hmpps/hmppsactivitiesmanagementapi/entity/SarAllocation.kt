package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.time.LocalDate

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
)
