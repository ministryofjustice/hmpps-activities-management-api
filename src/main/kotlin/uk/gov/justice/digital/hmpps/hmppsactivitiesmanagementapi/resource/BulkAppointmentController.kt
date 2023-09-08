package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSet
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSetCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentSeriesService
import java.security.Principal

@RestController
@RequestMapping("/bulk-appointments", produces = [MediaType.APPLICATION_JSON_VALUE])
class BulkAppointmentController(
  private val appointmentSeriesService: AppointmentSeriesService,
) {
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping()
  @Operation(
    summary = "Bulk create a set of appointments",
    description =
    """
    Create a list of appointments and allocate the supplied prisoner or prisoners to them.
    """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "The appointments were created.",
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
  fun bulkCreateAppointment(
    principal: Principal,
    @Valid
    @RequestBody
    @Parameter(
      description = "The create request containing the new appointments",
      required = true,
    )
    request: AppointmentSetCreateRequest,
  ): AppointmentSet = appointmentSeriesService.createAppointmentSet(request, principal)
}
