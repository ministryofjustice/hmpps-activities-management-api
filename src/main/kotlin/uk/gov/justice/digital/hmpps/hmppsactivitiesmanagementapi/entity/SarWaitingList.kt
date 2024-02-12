package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.time.LocalDate

@Entity
@Immutable
@Table(name = "v_sar_waiting_list")
data class SarWaitingList(
  @Id
  val waitingListId: Long,
  val prisonCode: String,
  val prisonerNumber: String,
  val activitySummary: String,
  val applicationDate: LocalDate,
  val originator: String,
  val status: String,
  val statusDate: LocalDate?,
  val comments: String?,
  val createdDate: LocalDate,
)
