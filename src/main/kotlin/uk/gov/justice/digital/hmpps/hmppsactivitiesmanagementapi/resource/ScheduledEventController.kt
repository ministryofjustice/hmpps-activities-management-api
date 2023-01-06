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
@RequestMapping("/scheduled-events")
class ScheduledEventController(private val scheduledEventService: ScheduledEventService) {

  @GetMapping(
    value = ["/prison/{prisonCode}"],
    produces = [MediaType.APPLICATION_JSON_VALUE]
  )
  @ResponseBody
  @Operation(
    summary = "Get a list of scheduled events for a prison, prisoner, date range (max 3 months) and optional time slot.",
    description = """
      Returns scheduled events for the prison, prisoner, date range (max 3 months) and optional time slot.
      Court hearings, appointments and visits always come from NOMIS (via prison API).
      Activities come from either NOMIS or the new Activities database, depending on whether the prison is
      marked as rolled-out in the activities database.
      (Intended usage: Prisoner calendar)
    """,
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
  fun getScheduledEventsByPrisonAndPrisonerAndDateRange(
    @PathVariable("prisonCode")
    @Parameter(description = "The 3-digit prison code.")
    prisonCode: String,

    @RequestParam(value = "prisonerNumber", required = true)
    @Parameter(description = "Prisoner number (required). Format A9999AA.")
    prisonerNumber: String,

    @RequestParam(value = "startDate", required = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "Start date of query (required). Format YYYY-MM-DD.")
    startDate: LocalDate,

    @RequestParam(value = "endDate", required = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "End date of query (required). Format YYYY-MM-DD. The end date must be within 3 months of the start date)")
    endDate: LocalDate,

    @RequestParam(value = "timeSlot", required = false)
    @Parameter(description = "Time slot for the events (optional). If supplied, one of AM, PM or ED.")
    timeSlot: TimeSlot?,
  ): PrisonerScheduledEvents? {
    val dateRange = LocalDateRange(startDate, endDate)
    if (endDate.isAfter(startDate.plusMonths(3))) {
      throw ValidationException("Date range cannot exceed 3 months")
    }
    return scheduledEventService.getScheduledEventsByPrisonAndPrisonerAndDateRange(prisonCode, prisonerNumber, dateRange, timeSlot)
  }

  @PostMapping(
    value = ["/prison/{prisonCode}"],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE]
  )
  @ResponseBody
  @Operation(
    summary = "Get a list of scheduled events for a prison and list of prisoner numbers for a date and time slot",
    description = """
      Returns scheduled events for the prison, prisoner numbers, single date and an optional time slot.
      Court hearings, appointments and visits always come from NOMIS (via prison API).
      Activities come from either NOMIS or the new activities database, depending on whether the prison is
      marked as rolled-out in the activities database.
      (Intended usage: Unlock list)
    """,
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
  fun getScheduledEventsByPrisonAndPrisonersAndDateRange(
    @PathVariable("prisonCode")
    @Parameter(description = "The 3-character prison code.")
    prisonCode: String,

    @RequestParam(value = "date", required = true)
    @Parameter(description = "The exact date to return events for (required) in format YYYY-MM-DD")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    date: LocalDate,

    @RequestParam(value = "timeSlot", required = false)
    @Parameter(description = "Time slot of the events (optional). If supplied, one of AM, PM or ED.")
    timeSlot: TimeSlot?,

    @RequestBody(required = true)
    @Parameter(description = "Set of prisoner numbers (required). Example ['G11234YI', 'B5234YI'].", required = true)
    prisonerNumbers: Set<String>
  ): PrisonerScheduledEvents? {
    return scheduledEventService.getScheduledEventsByPrisonAndPrisonersAndDateRange(prisonCode, prisonerNumbers, date, timeSlot)
  }
}
