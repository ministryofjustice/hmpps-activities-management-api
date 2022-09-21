package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
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

  val waitingList: List<ActivityWaiting> = emptyList(),

  val pay: ActivityPay? = null,

  @JsonFormat(pattern = "dd/MM/yyyy")
  val startDate: LocalDate,

  @JsonFormat(pattern = "dd/MM/yyyy")
  val endDate: LocalDate? = null,

  val active: Boolean = true,

  @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
  val createdTime: LocalDateTime,

  val createdBy: String
)
