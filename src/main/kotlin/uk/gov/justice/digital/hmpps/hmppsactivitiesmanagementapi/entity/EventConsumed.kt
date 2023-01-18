package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "event_consumed")
data class EventConsumed(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val eventId: Long = -1,

  val eventType: String,

  val eventTime: LocalDateTime,

  val prisonCode: String,

  val bookingId: Int,

  val prisonerNumber: String,

  val eventData: String
)
