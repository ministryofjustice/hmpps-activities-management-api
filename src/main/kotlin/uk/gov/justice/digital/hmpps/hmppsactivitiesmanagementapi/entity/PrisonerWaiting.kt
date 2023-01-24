package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "prisoner_waiting")
data class PrisonerWaiting(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val prisonerWaitingId: Long = -1,

  @ManyToOne
  @JoinColumn(name = "activity_id", nullable = false)
  val activity: Activity,

  val prisonerNumber: String,

  val priority: Int,

  val createdTime: LocalDateTime,

  val createdBy: String
)
