package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledInstanceAttendanceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ScheduleInstanceCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.UncancelScheduledInstanceRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ScheduledAttendee
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AttendancesService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ScheduledInstanceService
import java.time.LocalDate

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
  @CaseloadHeader
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN', 'NOMIS_ACTIVITIES')")
  fun getScheduledInstanceById(
    @PathVariable("instanceId") instanceId: Long,
  ): ActivityScheduleInstance = scheduledInstanceService.getActivityScheduleInstanceById(instanceId)

  @PostMapping
  @ResponseBody
  @Operation(
    summary = "Get scheduled instances by their ids",
    description = "Returns a list of scheduled instances.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The scheduled instances found",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = ActivityScheduleInstance::class)),
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
    ],
  )
  @CaseloadHeader
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN', 'NOMIS_ACTIVITIES')")
  fun getScheduledInstancesByIds(
    @RequestBody
    @Parameter(
      description = "The scheduled instance ids",
      required = true,
    )
    instanceIds: List<Long>,
  ): List<ActivityScheduleInstance> = scheduledInstanceService.getActivityScheduleInstancesByIds(instanceIds)

  @GetMapping(value = ["/{instanceId}/scheduled-attendees"])
  @ResponseBody
  @Operation(
    summary = "Get a list of scheduled attendees for a scheduled instance",
    description = "Returns a list of prisoners who are scheduled to attend a given scheduled instance.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Scheduled attendees found",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = ScheduledAttendee::class)),
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
  @CaseloadHeader
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN')")
  fun getScheduledAttendeesByScheduledInstance(
    @PathVariable("instanceId") instanceId: Long,
  ): List<ScheduledAttendee> = scheduledInstanceService.getAttendeesForScheduledInstance(instanceId)

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
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN')")
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

  @PutMapping(value = ["/{instanceId}/cancel"])
  @ResponseBody
  @Operation(
    summary = "Cancel a scheduled instance",
    description = "Cancels scheduled instance and associated attendance records",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "Scheduled instance successfully cancelled",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
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
        description = "The scheduled instance was not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN')")
  fun cancelScheduledInstance(
    @PathVariable("instanceId")
    instanceId: Long,
    @Valid
    @RequestBody
    @Parameter(
      description = "The scheduled instance cancellation request",
      required = true,
    )
    scheduleInstanceCancelRequest: ScheduleInstanceCancelRequest,
  ): ResponseEntity<Any> = scheduledInstanceService.cancelScheduledInstance(instanceId, scheduleInstanceCancelRequest).let { ResponseEntity.noContent().build() }

  @GetMapping(value = ["/attendance-summary"])
  @ResponseBody
  @Operation(
    summary = "Attendance summary of activity sessions for a given date",
    description = "Attendance summary of activity sessions for a given date",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Attendance summary",
        content = [Content(mediaType = "application/json", array = ArraySchema(schema = Schema(implementation = ScheduledInstanceAttendanceSummary::class)))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
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
        description = "The scheduled instance was not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @CaseloadHeader
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN')")
  fun attendanceSummary(
    @RequestParam("prisonCode", required = true)
    @Parameter(description = "The prison code of the prison to return an attendance summary for", example = "MDI")
    prisonCode: String,
    @RequestParam(value = "date", required = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "The date of the attendance summary. Format, YYYY-MM-DD.", example = "2023-09-20")
    date: LocalDate,
  ): List<ScheduledInstanceAttendanceSummary> = scheduledInstanceService.attendanceSummary(prisonCode, date)
}
