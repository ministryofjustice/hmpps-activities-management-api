package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
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
  val startDate: LocalDate?,

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

  @Schema(description = "Indicates (F)ull or (H)alf day (for payment purposes). Not nullable.", example = "H")
  var payPerSession: String,

  @Schema(description = "The NOMIS code for the minimum incentive level for this activity", example = "BAS")
  val minimumIncentiveLevel: String?,

  @Schema(description = "Whether the schedule runs on bank holidays", example = "true")
  val runsOnBankHoliday: Boolean = false,

  @Schema(description = "Details of when this activity runs during the week")
  val scheduleRules: List<NomisScheduleRule> = emptyList(),

  @Schema(description = "The pay rates which apply to this activity")
  val payRates: List<NomisPayRate> = emptyList(),

  @Schema(description = "The people currently allocated to this activity")
  val allocations: List<NomisAllocation> = emptyList(),
)

@Schema(description = "The scheduling rules in Nomis")
data class NomisScheduleRule(
  val startTime: LocalTime,

  val endTime: LocalTime,

  val monday: Boolean = false,

  val tuesday: Boolean = false,

  val wednesday: Boolean = false,

  val thursday: Boolean = false,

  val friday: Boolean = false,

  val saturday: Boolean = false,

  val sunday: Boolean = false,
)

@Schema(description = "Describes the pay rates defined for this activity")
data class NomisPayRate(
  @Schema(description = "The incentive level code from NOMIS")
  val incentiveLevel: String,

  @Schema(description = "The pay band code from NOMIS")
  val nomisPayBand: String,

  @Schema(description = "The pay rate for one half day session (in pence)")
  @field:PositiveOrZero(message = "Pay rates must be zero or above, cannot be negative.")
  val rate: Int,
)

@Schema(description = "Describes the allocations to this activity")
data class NomisAllocation(
  @field:NotNull
  @Schema(description = "The prisoner display number from NOMIS.")
  val prisonerNumber: String,

  @field:NotNull
  @Schema(description = "The prisoner booking id from NOMIS.")
  val bookingId: Long,

  @field:NotNull
  @Schema(description = "The pay band code from NOMIS")
  val nomisPayBand: String,

  @field:NotNull
  @Schema(description = "Date on which this allocation starts or started. Can not be null.", example = "2022-12-23")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  val startDate: LocalDate,

  @Schema(description = "Date on which this allocation starts or started. Nullable.", example = "2022-12-23")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  val endDate: LocalDate? = null,

  @Schema(description = "If an end date is set there may be a comment. Nullable.", example = "Due to end in January.")
  val endComment: String? = null,

  @Schema(description = "True id this prisoner is suspended.", example = "true")
  val suspendedFlag: Boolean = false,

  @Schema(description = "There may be a comment to explain the suspension.", example = "Suspended for poor attendance.")
  val suspendedComment: String? = null,
)
