package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCategory
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Describes a top-level activity")
data class Activity(

  @Schema(description = "The internally-generated ID for this activity", example = "123456")
  val id: Long,

  @Schema(description = "The prison code where this activity takes place", example = "PVI")
  val prisonCode: String,

  @Schema(description = "Flag to indicate if attendance is required for this activity, e.g. gym induction might not be mandatory attendance", example = "false")
  val attendanceRequired: Boolean,

  @Schema(description = "Flag to indicate if the location of the activity is in cell", example = "false")
  var inCell: Boolean,

  @Schema(description = "Flag to indicate if the location of the activity is on wing", example = "false")
  var onWing: Boolean,

  @Schema(description = "Flag to indicate if the location of the activity is off wing and not in a listed location", example = "false")
  var offWing: Boolean,

  @Schema(description = "Flag to indicate if the activity is piece work", example = "false")
  var pieceWork: Boolean,

  @Schema(description = "Flag to indicate if the activity carried out outside of the prison", example = "false")
  var outsideWork: Boolean,

  @Schema(description = "Indicates whether the activity session is a (F)ull day or a (H)alf day (for payment purposes). ", example = "H")
  var payPerSession: PayPerSession = PayPerSession.H,

  @Schema(description = "A brief summary description of this activity for use in forms and lists", example = "Maths level 1")
  val summary: String,

  @Schema(description = "A detailed description for this activity", example = "A basic maths course suitable for introduction to the subject")
  val description: String?,

  @Schema(description = "The category for this activity, one of the high-level categories")
  val category: ActivityCategory,

  @Schema(description = "The tier for this activity, as defined by the Future Prison Regime team")
  val tier: EventTier?,

  @Schema(description = "The organiser of this activity")
  val organiser: EventOrganiser? = null,

  @Schema(description = "A list of eligibility rules which apply to this activity. These can be positive (include) and negative (exclude)", example = "[FEMALE_ONLY,AGED_18-25]")
  val eligibilityRules: List<ActivityEligibility> = emptyList(),

  @Schema(description = "A list of schedules for this activity. These contain the time slots / recurrence settings for instances of this activity.")
  val schedules: List<ActivitySchedule> = emptyList(),

  @Schema(description = "The list of pay rates by incentive level and pay band that can apply to this activity")
  val pay: List<ActivityPay> = emptyList(),

  @Schema(description = "The list of recent pay rate changes that can apply to this activity")
  val payChange: List<ActivityPayHistory> = emptyList(),

  @Schema(description = "The date on which this activity will start. From this date, any schedules will be created as real, planned instances", example = "2022-09-21")
  @JsonFormat(pattern = "yyyy-MM-dd")
  val startDate: LocalDate,

  @Schema(description = "The date on which this activity ends. From this date, there will be no more planned instances of the activity. If null, the activity has no end date and will be scheduled indefinitely.", example = "2022-12-21")
  @JsonFormat(pattern = "yyyy-MM-dd")
  val endDate: LocalDate? = null,

  @Schema(description = "The most recent risk assessment level for this activity", example = "high")
  val riskLevel: String,

  @Schema(description = "The date and time when this activity was created", example = "2022-09-01T09:01:02")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val createdTime: LocalDateTime,

  @Schema(description = "The person who created this activity", example = "Adam Smith")
  val createdBy: String,

  @Schema(description = "The date and time when this activity was updated", example = "2022-09-01T09:01:02")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val updatedTime: LocalDateTime?,

  @Schema(description = "The person who updated this activity", example = "Adam Smith")
  val updatedBy: String?,

  @Schema(description = "The list of minimum education levels that can apply to this activity")
  val minimumEducationLevel: List<ActivityMinimumEducationLevel> = emptyList(),

  @Schema(description = "Whether the activity is a paid activity", example = "true")
  val paid: Boolean,
)

enum class PayPerSession { H, F }
