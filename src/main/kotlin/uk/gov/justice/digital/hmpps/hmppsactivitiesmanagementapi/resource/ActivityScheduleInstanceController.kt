package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ScheduledInstanceService
import java.time.LocalDate
import javax.validation.ValidationException

@RestController
@RequestMapping("/prisons/{prisonCode}/scheduled-instances", produces = [MediaType.APPLICATION_JSON_VALUE])
class ActivityScheduleInstanceController(private val scheduledInstanceService: ScheduledInstanceService) {

  @GetMapping()
  @ResponseBody
  @Operation(
    summary = "Get a list of scheduled instances for a prison, prisoner (optional) and date range (max 3 months)",
    description = "Returns zero or more scheduled instances for a prison, prisoner (optional) and date range (max 3 months).",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful call - zero or more scheduled instance records found",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = ActivityScheduleInstance::class))
          )
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      )
    ]
  )
  fun getActivityScheduleInstancesByDateRange(
    @PathVariable("prisonCode") prisonCode: String,
    @RequestParam(value = "prisonerNumber") @Parameter(description = "Prisoner number (optional)") prisonerNumber: String?,
    @RequestParam(value = "startDate", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Parameter(description = "Start date of query") startDate: LocalDate,
    @RequestParam(value = "endDate", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Parameter(description = "End date of query (max 3 months from start date)") endDate: LocalDate,
  ): List<ActivityScheduleInstance> {
    val dateRange = LocalDateRange(startDate, endDate)
    if (endDate.isAfter(startDate.plusMonths(3))) {
      throw ValidationException("Date range cannot exceed 3 months")
    }
    return scheduledInstanceService.getActivityScheduleInstancesByDateRange(prisonCode, prisonerNumber, dateRange)
  }
}
