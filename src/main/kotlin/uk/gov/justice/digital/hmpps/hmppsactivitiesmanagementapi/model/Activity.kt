package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Describes a top-level activity")
data class Activity(

  @Schema(description = "The internally-generated ID for this activity", example = "123456")
  val id: Long,

  @Schema(description = "The prison code where this activity takes place", example = "PVI")
  val prisonCode: String,

  @Schema(description = "A brief summary description of this activity for use in forms and lists", example = "Maths level 1")
  val summary: String,

  @Schema(description = "A detailed description for this activity", example = "A basic maths course suitable for introduction to the subject")
  val description: String,

  @Schema(description = "The category for this activity, one of the high-level categories", example = "Education, Work, Intervention, Health")
  val category: ActivityCategory,

  @Schema(description = "The tier for this activity, as defined by the Future Prison Regime team", example = "Tier 1, Tier 2, Foundation")
  val tier: ActivityTier,

  @Schema(description = "A list of eligibility rules which apply to this activity. These can be positive (include) and negative (exclude)", example = "[FEMALE_ONLY,AGED_18-25]")
  val eligibilityRules: List<ActivityEligibility> = emptyList(),

  @Schema(description = "A list of schedules for this activity. These contain the time slots / recurrence settings for instances of this activity.")
  val schedules: List<ActivitySchedule> = emptyList(),

  @Schema(description = "A list of prisoners who are waiting for allocation to this activity. This list is held against the activity, though allocation is against particular schedules of the activity")
  val waitingList: List<PrisonerWaiting> = emptyList(),

  @Schema(description = "The list of pay rates by incentive level and pay band that can apply to this activity")
  val pay: List<ActivityPay> = emptyList(),

  @Schema(description = "The date on which this activity will start. From this date, any schedules will be created as real, planned instances", example = "21/09/2022")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val startDate: LocalDate,

  @Schema(description = "The date on which this activity ends. From this date, there will be no more planned instances of the activity. If null, the activity has no end date and will be scheduled indefinitely.", example = "21/12/2022")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val endDate: LocalDate? = null,

  @Schema(description = "Flag to indicate if this activity is presently active", example = "true")
  val active: Boolean = true,

  @Schema(description = "The date and time when this activity was created", example = "01/09/2022 9:00")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
  val createdTime: LocalDateTime,

  @Schema(description = "The person who created this activity", example = "Adam Smith")
  val createdBy: String
)
