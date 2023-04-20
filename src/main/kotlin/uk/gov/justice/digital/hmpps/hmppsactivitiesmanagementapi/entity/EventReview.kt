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
  val eventReviewId: Long = -1,

  val serviceIdentifier: String?,

  val eventType: String?,

  val eventTime: LocalDateTime?,

  val prisonCode: String?,

  val prisonerNumber: String?,

  val bookingId: Int?,

  val eventData: String?,

  val acknowledgedTime: LocalDateTime?,

  val acknowledgedBy: String?,
)
