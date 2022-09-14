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
@Table(name = "activity_instance")
data class ActivityInstance(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val activityInstanceId: Int? = null,

  val prisonCode: String,

  @ManyToOne
  @JoinColumn(name = "activity_session_id", nullable = false)
  val activitySession: ActivitySession,

  val sessionDate: LocalDate,

  val startTime: LocalDateTime,

  val endTime: LocalDateTime,

  var internalLocationId: Int? = null,

  var cancelled: Boolean = false,

  var cancelledAt: LocalDateTime? = null,

  var cancelledBy: String? = null,
)
