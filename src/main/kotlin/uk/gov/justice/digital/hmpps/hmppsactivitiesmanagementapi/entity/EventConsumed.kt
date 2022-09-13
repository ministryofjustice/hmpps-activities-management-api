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
@Table(name = "event_consumed")
data class EventConsumed(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val eventId: Int = -1,

  val eventType: String,

  val eventTime: LocalDateTime,

  @ManyToOne
  @JoinColumn(name = "rollout_prison_id", nullable = false)
  val rolloutPrison: RolloutPrison,

  val bookingId: Int,

  val prisonerNumber: String,

  val eventData: String
)
