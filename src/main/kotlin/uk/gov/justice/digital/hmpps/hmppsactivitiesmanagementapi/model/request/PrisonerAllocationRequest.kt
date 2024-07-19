package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.FutureOrPresent
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Slot
import java.time.LocalDate

data class PrisonerAllocationRequest(

  @Schema(description = "The prisoner number (Nomis ID)", example = "A1234AA")
  @field:NotBlank(message = "Prisoner number must be supplied")
  @field:Size(max = 7, message = "Prisoner number cannot be more than 7 characters")
  val prisonerNumber: String?,

  @Schema(
    description = "Where a prison uses pay bands to differentiate earnings, this is the pay band code given to this prisoner. Can be null for unpaid activities.",
    example = "1",
  )
  val payBandId: Long? = null,

  @Schema(description = "The date when the prisoner will start the activity", example = "2022-09-10")
  @JsonFormat(pattern = "yyyy-MM-dd")
  @field:NotNull(message = "Start date must be supplied")
  @field:FutureOrPresent(message = "Start date must not be in the past")
  val startDate: LocalDate? = null,

  @Schema(description = "The date when the prisoner will stop attending the activity", example = "2023-09-10")
  @JsonFormat(pattern = "yyyy-MM-dd")
  val endDate: LocalDate? = null,

  @Schema(description = "The days and times that the prisoner is excluded from this activity's schedule")
  val exclusions: List<Slot>? = null,

  @Schema(description = "The scheduled instance id required when allocation starts today")
  val scheduleInstanceId: Long? = null,
)
