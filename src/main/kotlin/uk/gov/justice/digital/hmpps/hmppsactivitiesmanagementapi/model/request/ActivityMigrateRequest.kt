package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import java.time.LocalDate
import java.time.LocalTime

@Schema(description = "A request to migrate an activity from NOMIS into this service.")
data class ActivityMigrateRequest(

  @field:NotEmpty(message = "A program service code must be present.")
  @Schema(description = "A nomis program service code (activity category)", example = "PRISON JOBS")
  val programServiceCode: String,

  @field:NotEmpty(message = "Prison code must be supplied")
  @field:Size(max = 3, message = "Prison code should not exceed {max} characters")
  @Schema(description = "The prison code where this activity takes place", example = "PVI")
  val prisonCode: String,

  @field:NotNull
  @Schema(description = "Date on which this activity starts or started. Can not be null.", example = "2022-12-23")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  val startDate: LocalDate,

  @Schema(description = "Date when this activity ends. Can be null.", example = "2022-12-23")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  val endDate: LocalDate? = null,

  @Schema(description = "Optional NOMIS internal location id", example = "98877667")
  val internalLocationId: Long?,

  @Schema(description = "Optional NOMIS internal location code", example = "A011")
  val internalLocationCode: String?,

  @Schema(description = "Optional NOMIS internal location description", example = "PVI-1-2-A011")
  val internalLocationDescription: String?,

  @Schema(description = "The maximum number of prisoners who can attend. Not null.", example = "10")
  @field:Positive(message = "Capacity must be a positive number.")
  val capacity: Int,

  @field:NotEmpty(message = "Activity description is required.")
  @field:Size(max = 50, message = "The activity description should not exceed {max} characters")
  @Schema(description = "A summary description for the activity.", example = "Maths level 1")
  val description: String,

  @Schema(description = "Indicates (F)ull or (H)alf day (for payment purposes). Not nullable.", example = "H", allowableValues = ["H", "F"])
  val payPerSession: String,

  @Schema(description = "Whether the schedule runs on bank holidays", example = "true")
  val runsOnBankHoliday: Boolean = false,

  @Schema(description = "Whether the activity takes place outside of the prison", example = "true")
  val outsideWork: Boolean = false,

  @field:NotEmpty(message = "Activity schedules must have at least one time when they run.")
  @Schema(description = "Details of when this activity runs during the week")
  val scheduleRules: List<NomisScheduleRule>,

  @Schema(description = "The pay rates which apply to this activity. Where none are specified we will assume an unpaid activity.")
  val payRates: List<NomisPayRate>,
)

@Schema(description = "The scheduling rules in Nomis. At least one day and time must be specified.")
data class NomisScheduleRule(
  @Schema(description = "Start time on the day", example = "10:45")
  @field:NotNull
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime,

  @Schema(description = "End time on the day", example = "11:45")
  @field:NotNull
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime,

  @Schema(description = "Runs on a Monday", example = "true")
  val monday: Boolean = false,

  @Schema(description = "Runs on a Tuesday", example = "true")
  val tuesday: Boolean = false,

  @Schema(description = "Runs on a Wednesday", example = "true")
  val wednesday: Boolean = false,

  @Schema(description = "Runs on a Thursday", example = "true")
  val thursday: Boolean = false,

  @Schema(description = "Runs on a Friday", example = "true")
  val friday: Boolean = false,

  @Schema(description = "Runs on a Saturday", example = "true")
  val saturday: Boolean = false,

  @Schema(description = "Runs on a Sunday", example = "true")
  val sunday: Boolean = false,

  @Schema(description = "Time slot")
  val timeSlot: TimeSlot? = null,
)

@Schema(description = "Describes a pay rate for an activity.")
data class NomisPayRate(
  @Schema(description = "The incentive level code from NOMIS", example = "BAS")
  @field:NotEmpty(message = "An incentive level must be specified")
  val incentiveLevel: String,

  @Schema(description = "The pay band code from NOMIS. If null, will be defaulted to 1", example = "2")
  val nomisPayBand: String?,

  @Schema(description = "The pay rate for one half day session in pence", example = "120")
  @field:NotNull
  @field:PositiveOrZero(message = "Pay rates must be zero or above, cannot be negative.")
  val rate: Int,
)
