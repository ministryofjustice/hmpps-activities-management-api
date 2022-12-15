package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ScheduledEventService
import java.time.LocalDate
import javax.validation.ValidationException

@RestController
@RequestMapping("/prisons/{prisonCode}/scheduled-events", produces = [MediaType.APPLICATION_JSON_VALUE])
class ScheduledEventController(private val scheduledEventService: ScheduledEventService) {

  @GetMapping
  @ResponseBody
  @Operation(
    summary = "Get a list of scheduled events for a prison, prisoner and date range (max 3 months)",
    description = "Returns zero or more scheduled events for a prison, prisoner and date range (max 3 months).",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful call - zero or more scheduled events found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = PrisonerScheduledEvents::class)
          )
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
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
      ),
      ApiResponse(
        responseCode = "404",
        description = "Requested resource not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      )
    ]
  )
  fun getScheduledEventsByDateRange(
    @PathVariable("prisonCode") prisonCode: String,
    @RequestParam(value = "prisonerNumber", required = true) @Parameter(description = "Prisoner number") prisonerNumber: String,
    @RequestParam(value = "startDate", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Parameter(description = "Start date of query") startDate: LocalDate,
    @RequestParam(value = "endDate", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Parameter(description = "End date of query (max 3 months from start date)") endDate: LocalDate,
  ): PrisonerScheduledEvents? {
    val dateRange = LocalDateRange(startDate, endDate)
    if (endDate.isAfter(startDate.plusMonths(3))) {
      throw ValidationException("Date range cannot exceed 3 months")
    }
    return scheduledEventService.getScheduledEventsByDateRange(prisonCode, prisonerNumber, dateRange)
  }

  @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseBody
  @Operation(
    summary = "Get a list of scheduled events for a prison, offender list",
    description = "Returns zero or more scheduled events for a prison, offender list.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful call - zero or more scheduled events found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = PrisonerScheduledEvents::class)
          )
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
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
      ),
      ApiResponse(
        responseCode = "404",
        description = "Requested resource not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      )
    ]
  )
  fun getScheduledEventsForOffenderList(
    @PathVariable("prisonCode") prisonCode: String,
    @RequestParam(value = "date", required = false) @Parameter(description = "Date of the events") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    date: LocalDate?,
    @RequestParam(value = "timeSlot", required = false) @Parameter(description = "Time slot of the events")
    timeSlot: TimeSlot?,
    @RequestBody(required = true) @Parameter(description = "Set of prisoner numbers, for example ['G11234YI', 'B52SYI']", required = true)
    prisonerNumbers: Set<String>
  ): PrisonerScheduledEvents? {
    return scheduledEventService.getScheduledEventsForOffenderList(prisonCode, prisonerNumbers, date, timeSlot)
  }
}
