package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "prisoner_history")
data class PrisonerHistory(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val prisonerHistoryId: Long = 0,

  val historyType: String,

  val prisonCode: String,

  val prisonerNumber: String,

  val eventDescription: String,

  val eventTime: LocalDateTime,

  val eventBy: String,
)
