package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "event_review")
data class EventReview(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val eventReviewId: Long = 0,

  val serviceIdentifier: String? = null,

  val eventType: String? = null,

  val eventTime: LocalDateTime? = null,

  val prisonCode: String? = null,

  val prisonerNumber: String? = null,

  val bookingId: Int? = 0,

  val eventData: String? = null,

  val acknowledgedTime: LocalDateTime? = null,

  val acknowledgedBy: String? = null,
)
