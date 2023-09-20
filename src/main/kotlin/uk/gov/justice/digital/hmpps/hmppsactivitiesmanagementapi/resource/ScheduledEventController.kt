package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.ValidationException
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocationEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.InternalLocationService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.LocationService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ReferenceCodeDomain
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ReferenceCodeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ScheduledEventService
import java.time.LocalDate

@RestController
@RequestMapping("/scheduled-events")
class ScheduledEventController(
  private val scheduledEventService: ScheduledEventService,
  private val referenceCodeService: ReferenceCodeService,
  private val locationService: LocationService,
  private val internalLocationService: InternalLocationService,
) {

  @GetMapping(
    value = ["/prison/{prisonCode}"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @ResponseBody
  @Operation(
    summary = "Get a list of scheduled events for a prison, prisoner, date range (max 3 months) and optional time slot.",
    description = """
      Returns scheduled events for the prison, prisoner, date range (max 3 months) and optional time slot.
      Court hearings, adjudication hearings, transfers and visits come from NOMIS (via prison API).
      Activities and appointments come from either NOMIS or the local database depending on whether the prison is
      marked as active for appointments and/or activities.
      (Intended usage: Prisoner calendar / schedule)
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
            schema = Schema(implementation = PrisonerScheduledEvents::class),
          ),
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
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN')")
  fun getScheduledEventsForSinglePrisoner(
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
    return scheduledEventService.getScheduledEventsForSinglePrisoner(
      prisonCode,
      prisonerNumber,
      dateRange,
      timeSlot,
      referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY),
      locationService.getLocationsForAppointmentsMap(prisonCode),
    )
  }

  @PostMapping(
    value = ["/prison/{prisonCode}"],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @ResponseBody
  @Operation(
    summary = "Get a list of scheduled events for a prison and list of prisoner numbers for a date and time slot",
    description = """
      Returns scheduled events for the prison, prisoner numbers, single date and an optional time slot.
      Court hearings, adjudication hearings, transfers and visits come from NOMIS (via prison API).
      Activities and appointments come from either NOMIS or the local database depending on whether the prison is
      marked as rolled-out for activities and/or appointments.
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
            schema = Schema(implementation = PrisonerScheduledEvents::class),
          ),
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
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN')")
  fun getScheduledEventsForMultiplePrisoners(
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
    prisonerNumbers: Set<String>,
  ): PrisonerScheduledEvents? {
    return scheduledEventService.getScheduledEventsForMultiplePrisoners(
      prisonCode,
      prisonerNumbers,
      date,
      timeSlot,
      referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY),
      locationService.getLocationsForAppointmentsMap(prisonCode),
    )
  }

  @PostMapping(
    value = ["/prison/{prisonCode}/locations"],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @ResponseBody
  @Operation(
    summary = "Get a list of scheduled events for a prison and list of internal location ids numbers for a date and optional time slot",
    description = """
      Returns scheduled events for the prison, internal location ids, single date and an optional time slot.
      This endpoint only returns activities and appointments and these come from the local database.
      This endpoint supports the creation of movement lists.
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
            array = ArraySchema(schema = Schema(implementation = InternalLocationEvents::class)),
          ),
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
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN')")
  fun getScheduledEventsForMultipleLocations(
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
    @Parameter(description = "Set of internal location ids (required). Example [123, 456].", required = true)
    internalLocationIds: Set<Long>,
  ): Set<InternalLocationEvents> {
    require(date.isBefore(LocalDate.now().plusDays(61))) {
      "Supply a date up to 60 days in the future"
    }
    return internalLocationService.getInternalLocationEvents(
      prisonCode,
      internalLocationIds,
      date,
      timeSlot,
    )
  }
}
