package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.UncancelScheduledInstanceRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AttendancesService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ScheduledInstanceService

// TODO add pre-auth annotations to enforce roles when we have them
// TODO - Combine this with ActivityScheduleInstanceController - all /scheduled-instances endpoints.

@RestController
@RequestMapping("/scheduled-instances", produces = [MediaType.APPLICATION_JSON_VALUE])
class ScheduledInstanceController(
  private val scheduledInstanceService: ScheduledInstanceService,
  private val attendancesService: AttendancesService,
) {

  @GetMapping(value = ["/{instanceId}"])
  @ResponseBody
  @Operation(
    summary = "Get a scheduled instance by ID",
    description = "Returns a scheduled instance.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Scheduled instance found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ActivityScheduleInstance::class),
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
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The scheduled instance was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getScheduledInstanceById(
    @PathVariable("instanceId") instanceId: Long,
  ): ActivityScheduleInstance = scheduledInstanceService.getActivityScheduleInstanceById(instanceId)

  @GetMapping(value = ["/{instanceId}/attendances"])
  @ResponseBody
  @Operation(
    summary = "Get a list of attendances for a scheduled instance",
    description = "Returns one or more attendance records for a particular scheduled activity for a given scheduled instance.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Attendance records found",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = Attendance::class)),
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
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The scheduled instance was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getAttendancesByScheduledInstance(
    @PathVariable("instanceId") instanceId: Long,
  ): List<Attendance> = attendancesService.findAttendancesByScheduledInstance(instanceId)

  @PutMapping(value = ["/{instanceId}/uncancel"])
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @ResponseBody
  @Operation(
    summary = "Un-cancels a scheduled instance.",
    description = "Un-cancels a previously cancelled scheduled instance.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "The scheduled instance was successfully un cancelled.",
      ),
      ApiResponse(
        responseCode = "400",
        description = "The scheduled instance is not cancelled or it is in the past",
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
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Not Found, the scheduled instance does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun uncancelScheduledInstance(
    @PathVariable("instanceId") instanceId: Long,
    @Valid
    @RequestBody
    @Parameter(
      description = "The uncancel request with the user details",
      required = true,
    )
    request: UncancelScheduledInstanceRequest,
  ) {
    scheduledInstanceService.uncancelScheduledInstance(instanceId)
  }
}
