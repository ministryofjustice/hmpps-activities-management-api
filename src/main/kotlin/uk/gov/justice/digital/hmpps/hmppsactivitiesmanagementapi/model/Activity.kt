package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import java.time.LocalDate
import java.time.LocalDateTime

// TODO swagger docs
data class Activity(

  val id: Long,

  val prisonCode: String,

  val summary: String,

  val description: String,

  val category: ActivityCategory,

  val tier: ActivityTier,

  val eligibilityRules: List<ActivityEligibility> = emptyList(),

  val sessions: List<ActivitySession> = emptyList(),

  val pay: ActivityPay? = null,

  val startDate: LocalDate,

  val endDate: LocalDate? = null,

  val active: Boolean,

  val createdAt: LocalDateTime,

  val createdBy: String
)
