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
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocationEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.LocationEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentCategoryService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.InternalLocationService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ScheduledEventService
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/scheduled-events")
class ScheduledEventController(
  private val scheduledEventService: ScheduledEventService,
  private val appointmentCategoryService: AppointmentCategoryService,
  private val internalLocationService: InternalLocationService,
) {
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
    @PathVariable @Parameter(description = "The 3-character prison code.")
    prisonCode: String,
    @RequestParam(value = "date", required = true)
    @Parameter(description = "The exact date to return events for (required) in format YYYY-MM-DD")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    date: LocalDate,
    @RequestParam(value = "timeSlot", required = false)
    @Parameter(description = "Time slot of the events (optional). If supplied, one of AM, PM or ED.")
    timeSlot: TimeSlot?,
    @RequestParam(value = "includeExternalMovements", required = false, defaultValue = "false")
    @Parameter(description = "Determines whether to include external activities (TAPs) in the response.")
    includeExternalMovements: Boolean = false,
    @RequestBody(required = true)
    @Parameter(description = "Set of prisoner numbers (required). Example ['G11234YI', 'B5234YI'].", required = true)
    prisonerNumbers: Set<String>,
  ): PrisonerScheduledEvents? = scheduledEventService.getScheduledEventsForMultiplePrisoners(
    prisonCode,
    prisonerNumbers,
    date,
    timeSlot,
    appointmentCategoryService.getAll(),
    includeExternalMovements,
  )

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
      Note that activities are only scheduled 60 days in advance. Appointments may be scheduled for any date in the future.
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
  @Deprecated(message = "Will be replaced by getScheduledEventsForMultipleLocationsByDPSLocationsIds")
  fun getScheduledEventsForMultipleLocations(
    @PathVariable @Parameter(description = "The 3-character prison code.")
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
  ): Set<InternalLocationEvents> = internalLocationService.getInternalLocationEvents(
    prisonCode,
    internalLocationIds,
    date,
    timeSlot,
  )

  @GetMapping(
    value = ["/prison/{prisonCode}/location-events"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @ResponseBody
  @Operation(
    summary = "Get a list of scheduled events for a DPS location, a date and optional time slot",
    description = """
      Returns scheduled events for the DPS location, date and optional time slot.
      This endpoint only returns activities and appointments from the local database.
      This endpoint supports the creation of movement lists.
      Activities are only scheduled 60 days in advance. Appointments may be scheduled for any date in the future.
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
  fun getScheduledEventsByDPSLocationsId(
    @PathVariable @Parameter(description = "The 3-character prison code.")
    prisonCode: String,
    @RequestParam(value = "date", required = true)
    @Parameter(description = "The exact date to return events for (required) in format YYYY-MM-DD")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    date: LocalDate,
    @RequestParam(value = "dpsLocationId", required = true)
    @Parameter(description = "The internal DPS Location UUID. Example b7602cc8-e769-4cbb-8194-62d8e655992a")
    dpsLocationId: UUID,
    @RequestParam(value = "timeSlot", required = false)
    @Parameter(description = "Time slot of the events (optional). If supplied, one of AM, PM or ED.")
    timeSlot: TimeSlot?,
  ): InternalLocationEvents = internalLocationService.getLocationEvents(prisonCode, dpsLocationId, date, timeSlot)

  @Deprecated("Use the GET /prison/{prisonCode}/scheduled-external-movements endpoint that returns a single LocationEvents object")
  @GetMapping(
    value = ["/prison/{prisonCode}/external-movements"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @ResponseBody
  @Operation(
    summary = "Get a list of external movements (TAPs) for a given prison code, date and an optional time slot",
    description = """
      Returns external movements fetched from the External Movements API for the given prison,
      date and optional time slot. This endpoint supports the creation of movement lists.
    """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful call - zero or more external movements found",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = LocationEvents::class)),
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
    ],
  )
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN')")
  fun getExternalMovements(
    @PathVariable @Parameter(description = "The 3-character prison code.")
    prisonCode: String,
    @RequestParam(value = "date", required = true)
    @Parameter(description = "The exact date to return movements for (required) in format YYYY-MM-DD")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    date: LocalDate,
    @RequestParam(value = "timeSlot", required = false)
    @Parameter(description = "Time slot of the movements (optional). If supplied, one of AM, PM or ED.")
    timeSlot: TimeSlot?,
  ): Set<LocationEvents> = scheduledEventService.getExternalMovements(
    prisonCode,
    date,
    timeSlot,
  )

  @GetMapping(
    value = ["/prison/{prisonCode}/scheduled-external-movements"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @ResponseBody
  @Operation(
    summary = "Get external movements (TAPs) as a single LocationEvents object for a given prison code, date and an optional time slot",
    description = """
      Returns external movements fetched from the External Movements API for the given prison,
      date and optional time slot as a single LocationEvents object. This endpoint supports the creation of movement lists.
    """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful call - zero or more external movements found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = LocationEvents::class),
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
    ],
  )
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN')")
  fun getScheduledExternalMovements(
    @PathVariable @Parameter(description = "The 3-character prison code.")
    prisonCode: String,
    @RequestParam(value = "date", required = true)
    @Parameter(description = "The exact date to return movements for (required) in format YYYY-MM-DD")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    date: LocalDate,
    @RequestParam(value = "timeSlot", required = false)
    @Parameter(description = "Time slot of the movements (optional). If supplied, one of AM, PM or ED.")
    timeSlot: TimeSlot?,
  ): LocationEvents = scheduledEventService.getScheduledExternalMovements(prisonCode, date, timeSlot)
}
