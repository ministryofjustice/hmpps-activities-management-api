package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

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
