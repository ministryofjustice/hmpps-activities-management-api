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
@Table(name = "activity_waitlist")
data class ActivityWaitlist(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val activityWaitingListId: Int? = null,

  val prisonCode: String,

  val prisonerNumber: String,

  @ManyToOne
  @JoinColumn(name = "activity_session_id", nullable = false)
  val activitySession: ActivitySession,

  val priority: Int,

  val createdAt: LocalDateTime,

  val createdBy: String
)
