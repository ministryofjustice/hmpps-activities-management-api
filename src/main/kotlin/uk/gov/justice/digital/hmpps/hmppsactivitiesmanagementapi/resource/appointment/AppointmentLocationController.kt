package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.appointment

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.LocationService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentLocation

@RestController
@RequestMapping("/appointment-locations", produces = [MediaType.APPLICATION_JSON_VALUE])
class AppointmentLocationController(private val locationService: LocationService) {
  @Operation(
    summary = "Get the list of appointment locations",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Appointment locations found",
        content = [Content(mediaType = "application/json", array = ArraySchema(schema = Schema(implementation = AppointmentLocationSummary::class)))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @GetMapping(
    value = ["/{prisonCode}"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @ResponseBody
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN')")
  fun getAppointmentLocations(
    @PathVariable("prisonCode") prisonCode: String,
  ): List<AppointmentLocationSummary> =
    locationService.getLocationsForAppointments(prisonCode).toAppointmentLocation()
}
