package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.envers.Audited
import java.time.LocalDateTime

@Entity
@Audited
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

  val bookingId: Int? = null,

  val eventData: String? = null,

  var acknowledgedTime: LocalDateTime? = null,

  var acknowledgedBy: String? = null,

  @Enumerated(EnumType.STRING)
  var eventDescription: EventReviewDescription? = null,
)

enum class EventReviewDescription {
  ACTIVITY_SUSPENDED,
  ACTIVITY_ENDED,
  RELEASED,
  PERMANENT_RELEASE,
  TEMPORARY_RELEASE,
}
