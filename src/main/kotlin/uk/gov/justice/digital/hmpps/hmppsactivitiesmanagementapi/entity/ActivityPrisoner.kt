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
@Table(name = "activity_prisoner")
data class ActivityPrisoner(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val activityPrisonerId: Int = -1,

  val prisonerNumber: String,

  @ManyToOne
  @JoinColumn(name = "activity_session_id", nullable = false)
  val activitySession: ActivitySession,

  var iepLevel: String? = null,

  var payBand: String? = null,

  var startDate: LocalDate? = null,

  var endDate: LocalDate? = null,

  var isActive: Boolean = false,

  var allocationAt: LocalDateTime? = null,

  var allocatedBy: String? = null,

  var deallocatedAt: LocalDateTime? = null,

  var deallocatedBy: String? = null,

  var deallocationReason: String? = null,
)
