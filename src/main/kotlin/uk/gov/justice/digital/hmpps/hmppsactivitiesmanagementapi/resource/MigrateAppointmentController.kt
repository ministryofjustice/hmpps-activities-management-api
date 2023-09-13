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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentSeriesService
import java.security.Principal

@RestController
@RequestMapping("/migrate-appointment", produces = [MediaType.APPLICATION_JSON_VALUE])
class MigrateAppointmentController(
  private val appointmentSeriesService: AppointmentSeriesService,
) {
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping()
  @Operation(
    summary = "Migrate an appointment from NOMIS",
    description =
    """
    Migrate an appointment creating an appointment series with one appointment that has the supplied prisoner allocated.
    """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "The appointment was migrated.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = AppointmentInstance::class),
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
  @PreAuthorize("hasRole('NOMIS_APPOINTMENTS')")
  fun migrateAppointment(
    principal: Principal,
    @Valid
    @RequestBody
    @Parameter(
      description = "The migration request with the appointment details",
      required = true,
    )
    request: AppointmentMigrateRequest,
  ): AppointmentInstance = appointmentSeriesService.migrateAppointment(request, principal)
}
