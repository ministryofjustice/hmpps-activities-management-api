package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerAllocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.CapacityAndAllocated
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityScheduleService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.CapacityService
import javax.validation.Valid

// TODO add pre-auth annotations to enforce roles when we have them

@RestController
@RequestMapping("/schedules", produces = [MediaType.APPLICATION_JSON_VALUE])
class ActivityScheduleController(
  private val scheduleService: ActivityScheduleService,
  private val capacityService: CapacityService,
) {

  @Operation(
    summary = "Get the capacity and number of allocated slots in an activity schedule",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Activity schedule capacity",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CapacityAndAllocated::class)
          )
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [
          Content(
            mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)
          )
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [
          Content(
            mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)
          )
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Schedule ID not found",
        content = [
          Content(
            mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)
          )
        ],
      )
    ]
  )
  @GetMapping(value = ["/{activityScheduleId}/capacity"])
  @ResponseBody
  fun getActivityScheduleCapacity(@PathVariable("activityScheduleId") activityScheduleId: Long): CapacityAndAllocated =
    capacityService.getActivityScheduleCapacityAndAllocated(activityScheduleId)

  @GetMapping(value = ["/{scheduleId}/allocations"])
  @ResponseBody
  @Operation(
    summary = "Get a list of activity schedule allocations",
    description = "Returns zero or more activity schedule allocations.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The allocations for an activity schedule",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = Allocation::class))
          )
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Schedule ID not found",
        content = [
          Content(
            mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)
          )
        ],
      )
    ]
  )
  fun getAllocationsBy(
    @PathVariable("scheduleId") scheduleId: Long,
    @RequestParam(
      value = "activeOnly",
      required = false
    ) @Parameter(description = "If true will only return active allocations. Defaults to true.") activeOnly: Boolean?,
  ) = scheduleService.getAllocationsBy(scheduleId, activeOnly ?: true)

  @GetMapping(value = ["/{scheduleId}"])
  @ResponseBody
  @Operation(
    summary = "Get an activity schedule by its id",
    description = "Returns a single activity schedule by its unique identifier.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Activity found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ActivitySchedule::class)
          )
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The activity for this ID was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ],
      )
    ]
  )
  fun getScheduleId(@PathVariable("scheduleId") scheduleId: Long) =
    scheduleService.getScheduleById(scheduleId)

  @PostMapping(value = ["/{scheduleId}/allocations"])
  @Operation(
    summary = "Allocate offender to schedule",
    description = "Allocates the supplied offender allocation request to the activity schedule.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "The allocation was created and added to the schedule.",
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
        description = "The activity schedule for this ID was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ],
      )
    ]
  )
  fun allocate(
    @RequestBody @Parameter(
      description = "The prisoner allocation request details",
      required = true
    ) @Valid prisonerAllocationRequest: PrisonerAllocationRequest
  ): ResponseEntity<Any> = scheduleService.allocatePrisoner(prisonerAllocationRequest).let { ResponseEntity.noContent().build() }
}
