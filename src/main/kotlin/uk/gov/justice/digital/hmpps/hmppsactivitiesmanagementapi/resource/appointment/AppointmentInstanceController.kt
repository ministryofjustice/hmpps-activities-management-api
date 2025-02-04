package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.appointment

import io.swagger.v3.oas.annotations.Operation
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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.CaseloadHeader
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AppointmentInstanceService

@RestController
@RequestMapping("/appointment-instances", produces = [MediaType.APPLICATION_JSON_VALUE])
class AppointmentInstanceController(
  private val appointmentInstanceService: AppointmentInstanceService,
) {
  @GetMapping(value = ["/{appointmentInstanceId}"])
  @ResponseBody
  @Operation(
    summary = "Get an appointment instance by its id",
    description = "Returns an appointment instance and its details by its unique identifier.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Appointment instance found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = AppointmentInstance::class),
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
        description = "The appointment instance for this ID was not found.",
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
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN', 'NOMIS_ACTIVITIES')")
  fun getAppointmentInstanceById(@PathVariable("appointmentInstanceId") appointmentInstanceId: Long): AppointmentInstance = appointmentInstanceService.getAppointmentInstanceById(appointmentInstanceId)
}
