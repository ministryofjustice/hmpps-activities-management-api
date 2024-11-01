package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.appointment

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSet
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSetDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSetCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.CaseloadHeader
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AppointmentSetService
import java.security.Principal

@RestController
@RequestMapping("/appointment-set", produces = [MediaType.APPLICATION_JSON_VALUE])
class AppointmentSetController(
  private val appointmentSetService: AppointmentSetService,
) {
  @GetMapping(value = ["/{appointmentSetId}/details"])
  @ResponseBody
  @Operation(
    summary = "Get the details of an appointment set for display purposes by its id",
    description = "Returns the displayable details of an appointment set by its unique identifier.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Appointment set found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = AppointmentSetDetails::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The appointment set for this id was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @CaseloadHeader
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN')")
  fun getAppointmentSetDetailsById(@PathVariable("appointmentSetId") appointmentSetId: Long): AppointmentSetDetails =
    appointmentSetService.getAppointmentSetDetailsById(appointmentSetId)

  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping()
  @Operation(
    summary = "Create a set of appointments",
    description =
    """
    Create a set of appointments that start on the same day and add the associated prisoner as the appointment attendee.
    """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "The appointment set was created.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = AppointmentSet::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @CaseloadHeader
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN')")
  fun createAppointmentSet(
    principal: Principal,
    @Valid
    @RequestBody
    @Parameter(
      description = "The create request with the new appointment set details",
      required = true,
    )
    request: AppointmentSetCreateRequest,
  ): AppointmentSet = appointmentSetService.createAppointmentSet(request, principal)
}
