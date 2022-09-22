package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

data class Activity(

  @Schema(description = "The internal ID for this activity", example = "123456")
  val id: Long,

  @Schema(description = "The prison code where this activity takes place", example = "PVI")
  val prisonCode: String,

  @Schema(description = "A brief summary for this activity", example = "Maths")
  val summary: String,

  @Schema(description = "A detailed description for this activity", example = "Maths Level One")
  val description: String,

  @Schema(description = "The category for this activity")
  val category: ActivityCategory,

  @Schema(description = "The tier for this activity")
  val tier: ActivityTier,

  @Schema(description = "The eligibility rules for this activity")
  val eligibilityRules: List<ActivityEligibility> = emptyList(),

  @Schema(description = "The sessions for this activity")
  val sessions: List<ActivitySession> = emptyList(),

  @Schema(description = "The waiting list for this activity")
  val waitingList: List<ActivityWaiting> = emptyList(),

  @Schema(description = "The amount paid for this activity")
  val pay: ActivityPay? = null,

  @Schema(description = "The date on which this activity starts", example = "21/09/2022")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val startDate: LocalDate,

  @Schema(description = "The date on which this activity ends", example = "21/12/2022")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val endDate: LocalDate? = null,

  val active: Boolean = true,

  @Schema(description = "The date and time when this activity was created", example = "01/09/2022 9:00")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
  val createdTime: LocalDateTime,

  @Schema(description = "The person whom created this activity", example = "Adam Smith")
  val createdBy: String
)
