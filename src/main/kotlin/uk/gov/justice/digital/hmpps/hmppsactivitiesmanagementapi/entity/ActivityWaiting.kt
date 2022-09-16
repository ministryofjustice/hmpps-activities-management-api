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
@Table(name = "activity_waiting")
data class ActivityWaiting(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val activityWaitingId: Long? = null,

  @ManyToOne
  @JoinColumn(name = "activity_id", nullable = false)
  val activity: Activity,

  val prisonerNumber: String,

  val priority: Int,

  val createdTime: LocalDateTime,

  val createdBy: String
)
