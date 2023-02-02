package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentCategory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Schema(
  description =
  """
  """
)
data class Appointment (
  @Schema(
    description = "The internally generated identifier for this appointment"
  )
  val id: Long,

  @Schema(
    description = "",
  )
  val category: AppointmentCategory,

  @Schema(
    description = "",
    example = ""
  )
  val prisonCode: String,

  @Schema(
    description = "",
    example = ""
  )
  val internalLocationId: Int?,

  @Schema(
    description = "",
    example = ""
  )
  val inCell: Boolean,

  @Schema(
    description = "",
    example = ""
  )
  val startDate: LocalDate,

  @Schema(
    description = "",
    example = ""
  )
  val startTime: LocalTime,

  @Schema(
    description = "",
    example = ""
  )
  val endTime: LocalTime?,

  @Schema(
    description = "",
    example = ""
  )
  val comment: String,

  @Schema(
    description = "",
    example = ""
  )
  val created: LocalDateTime,

  @Schema(
    description = "",
    example = ""
  )
  val createdBy: String,

  @Schema(
    description = "",
    example = ""
  )
  val updated: LocalDateTime?,

  @Schema(
    description = "",
    example = ""
  )
  val updatedBy: String,

  @Schema(
    description = "",
    example = ""
  )
  val deleted: Boolean,

  @Schema(
    description = "",
    example = ""
  )
  val allocations: List<AppointmentAllocation> = emptyList()
)
