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
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.LocationGroup
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocationEventsSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.whereabouts.LocationPrefixDto
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.InternalLocationService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.LocationGroupServiceSelector
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.LocationService
import java.time.LocalDate

@RestController
@RequestMapping("/locations")
class LocationController(
  private val locationService: LocationService,
  private val locationGroupServiceSelector: LocationGroupServiceSelector,
  private val internalLocationService: InternalLocationService,
) {
  @GetMapping(
    value = ["/prison/{prisonCode}"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @ResponseBody
  @Operation(
    summary = "List of cell locations for a prison group supplied as a query parameter",
    description = "List of cell locations for a prison group supplied as a query parameter",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful call - zero or more cell locations found",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = Location::class)),
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
  fun getCellLocationsForGroup(
    @PathVariable("prisonCode") prisonCode: String,
    @RequestParam(value = "groupName", required = true) groupName: String,
  ): List<Location>? = locationService.getCellLocationsForGroup(prisonCode, groupName)

  @GetMapping(
    value = ["/prison/{prisonCode}/location-groups"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @ResponseBody
  @Operation(
    summary = "List of all available location groups defined at a prison",
    description = "List of all available location groups defined at a prison",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful call - zero or more location groups found",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = LocationGroup::class)),
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
  fun getLocationGroups(
    @PathVariable("prisonCode") prisonCode: String,
  ): List<LocationGroup>? = locationGroupServiceSelector.getLocationGroups(prisonCode)

  @GetMapping(
    value = ["/prison/{prisonCode}/location-prefix"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @ResponseBody
  @Operation(
    summary = "Get the location prefix for a location group supplied as a query parameter",
    description = "Get location prefix for a location group name supplied as a query parameter",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful call - Location prefix found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = LocationPrefixDto::class),
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
  fun getLocationPrefixForGroup(
    @PathVariable("prisonCode") prisonCode: String,
    @RequestParam(value = "groupName", required = true) groupName: String,
  ): LocationPrefixDto? = locationService.getLocationPrefixFromGroup(prisonCode, groupName)

  @GetMapping(
    value = ["/prison/{prisonCode}/events-summaries"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @ResponseBody
  @Operation(
    summary = """
      Get a list of internal locations that have events scheduled to take place on the specified date and optional time slot.
      """,
    description = """
      Returns internal locations that have events scheduled to take place on the specified date and optional time slot.
      Will contain summary information about the events taking place at the location as well as the total number of
      prisoners due to arrive at the location. This endpoint supports the creation of movement lists allowing
      users to select from a sublist of only the internal locations that have events scheduled there.
      Note that activities are only scheduled 60 days in advance. Appointments may be scheduled for any date in the future.
    """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful call - zero or more internal locations with scheduled events found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = InternalLocationEventsSummary::class),
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
  fun getInternalLocationEventsSummary(
    @PathVariable("prisonCode")
    @Parameter(description = "The 3-digit prison code.")
    prisonCode: String,
    @RequestParam(value = "date", required = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "Date of scheduled events (required). Format YYYY-MM-DD")
    date: LocalDate,
    @RequestParam(value = "timeSlot", required = false)
    @Parameter(description = "Time slot for the scheduled events (optional). If supplied, one of AM, PM or ED.")
    timeSlot: TimeSlot?,
  ): Set<InternalLocationEventsSummary> {
    return internalLocationService.getInternalLocationEventsSummaries(
      prisonCode,
      date,
      timeSlot,
    )
  }
}
