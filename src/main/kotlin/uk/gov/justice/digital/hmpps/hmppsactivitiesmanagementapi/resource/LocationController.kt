package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.LocationGroup
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.whereabouts.LocationPrefixDto
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.LocationGroupServiceSelector
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.LocationService

@RestController
@RequestMapping("/locations")
class LocationController(
  private val locationService: LocationService,
  private val locationGroupServiceSelector: LocationGroupServiceSelector,
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
            array = ArraySchema(schema = Schema(implementation = Location::class))
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
            array = ArraySchema(schema = Schema(implementation = LocationGroup::class))
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
  fun getLocationGroups(
    @PathVariable("prisonCode") prisonCode: String
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
            schema = Schema(implementation = LocationPrefixDto::class)
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
  fun getLocationPrefixForGroup(
    @PathVariable("prisonCode") prisonCode: String,
    @RequestParam(value = "groupName", required = true) groupName: String,
  ): LocationPrefixDto? = locationService.getLocationPrefixFromGroup(prisonCode, groupName)
}
