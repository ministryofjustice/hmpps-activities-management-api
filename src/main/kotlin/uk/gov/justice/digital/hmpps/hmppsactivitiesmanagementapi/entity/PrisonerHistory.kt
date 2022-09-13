package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "prisoner_history")
data class PrisonerHistory(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val prisonerHistoryId: Int = -1,

  val historyType: String,

  @ManyToOne
  @JoinColumn(name = "rollout_prison_id", nullable = false)
  val rolloutPrison: RolloutPrison,

  val prisonerNumber: String,

  val eventDescription: String,

  val eventTime: LocalDateTime,

  val eventBy: String,
)
