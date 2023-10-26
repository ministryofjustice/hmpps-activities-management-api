package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import java.time.LocalDate

@Schema(
  description = "Returns all waiting list applications that match the search criteria.",
)
data class WaitingListSearchRequest(
  @Schema(
    description = "Filter applications with a request date on or after provided date",
    example = "2023-06-20",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val applicationDateFrom: LocalDate? = null,

  @Schema(
    description = "Filter applications with a request date on or before provided date",
    example = "2023-06-20",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val applicationDateTo: LocalDate? = null,

  @Schema(
    description = "The activity to return waiting list applications for.",
    example = "3",
  )
  val activityId: Long? = null,

  @Schema(
    description = "The prisoner or prisoners to retrieve waiting list applications for.",
    example = "[\"A1234BC\"]",
  )
  val prisonerNumbers: List<String>? = null,

  @Schema(
    description = "Filter by the status of the application. PENDING, APPROVED or DECLINED.",
    example = "[\"DECLINED\", \"PENDING\"]",
  )
  val status: List<WaitingListStatus>? = null,
)
