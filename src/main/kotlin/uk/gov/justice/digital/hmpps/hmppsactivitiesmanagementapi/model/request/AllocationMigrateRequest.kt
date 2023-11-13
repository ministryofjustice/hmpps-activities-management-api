package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Slot
import java.time.LocalDate

@Schema(
  description = """
    A request to migrate an allocation from Nomis into the activities service.
    Where the request relates to a prison which operates a split regime two activity IDs
    can be provided and the service will use rules to determine to which the allocation will be made.
  """,
)
data class AllocationMigrateRequest(
  @field:NotEmpty(message = "Prison code must be supplied")
  @field:Size(max = 3, message = "Prison code should not exceed {max} characters")
  @Schema(description = "The prison code where this allocation is to be made", example = "PVI")
  val prisonCode: String,

  @field:NotNull
  @Schema(description = "The activity ID to which this allocation should be made", example = "12332")
  val activityId: Long,

  @Schema(description = "The alternative activity ID in a split regime", example = "1322")
  val splitRegimeActivityId: Long? = null,

  @field:NotNull
  @Schema(description = "The prisoner display number from NOMIS", example = "A3334AB")
  val prisonerNumber: String,

  @field:NotNull
  @Schema(description = "The prisoner booking id from NOMIS", example = "99098484")
  val bookingId: Long,

  @Schema(description = "The prisoner cell location", example = "RSI-A-1-2-001")
  val cellLocation: String? = null,

  @Schema(description = "The pay band code from NOMIS", example = "2")
  val nomisPayBand: String?,

  @field:NotNull
  @Schema(description = "Date on which this allocation starts or started. Not nullable.", example = "2022-12-23")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  val startDate: LocalDate,

  @Schema(description = "Date on which this allocation ended or will end. Nullable.", example = "2022-12-23")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  val endDate: LocalDate? = null,

  @Schema(description = "If an end date is set there may be a comment. Nullable.", example = "Due to end in January.")
  val endComment: String? = null,

  @Schema(description = "True if this prisoner allocation is suspended.", example = "true")
  val suspendedFlag: Boolean = false,

  @Schema(description = "The days and times that the prisoner is excluded from this activity's schedule")
  val exclusions: List<Slot>? = null,
)
