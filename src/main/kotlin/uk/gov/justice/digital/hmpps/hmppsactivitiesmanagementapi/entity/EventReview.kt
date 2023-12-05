package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.OffenderMergeDetails
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

  val bookingId: Int? = null,

  val eventData: String? = null,

  var acknowledgedTime: LocalDateTime? = null,

  var acknowledgedBy: String? = null,
) {
  fun merge(offenderMergeDetails: OffenderMergeDetails) {
    // TODO to be implemented
  }
}
