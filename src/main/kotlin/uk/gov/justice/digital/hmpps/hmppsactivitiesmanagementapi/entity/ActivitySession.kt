package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import java.time.LocalDate
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "activity_session")
data class ActivitySession(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val activitySessionId: Int = -1,

  @ManyToOne
  @JoinColumn(name = "activity_id", nullable = false)
  val activity: Activity,

  val description: String,

  var suspendUntil: LocalDate? = null,

  val startTime: LocalDateTime,

  val endTime: LocalDateTime,

  var internalLocationId: Int? = null,

  var internalLocationCode: String? = null,

  var internalLocationDescription: String? = null,

  val capacity: Int,

  val daysOfWeek: String
) {
  init {
    // TODO - ensure the days of the week is valid or introduce value type?
  }
}
