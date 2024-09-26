package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "data_fix")
data class DataFix(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val dataFixId: Long = 0,

  val activityScheduleId: Long,

  val prisonerNumber: String,

  val startDate: LocalDate,

  @Enumerated(EnumType.STRING)
  val prisonerStatus: PrisonerStatus?,
)
