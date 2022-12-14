package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.CapacityAndAllocated
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.CapacityService
import java.security.Principal
import javax.validation.Valid

// TODO add pre-auth annotations to enforce roles when we have them

@RestController
@Validated
@RequestMapping("/activities", produces = [MediaType.APPLICATION_JSON_VALUE])
class ActivityController(
  private val activityService: ActivityService,
  private val capacityService: CapacityService,
) {

  @GetMapping(value = ["/{activityId}"])
  @ResponseBody
  @Operation(
    summary = "Get an activity by its id",
    description = "Returns a single activity and its details by its unique identifier.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Activity found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = Activity::class)
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
  fun getActivityById(@PathVariable("activityId") activityId: Long): Activity =
    activityService.getActivityById(activityId)

  @PostMapping
  @Operation(
    summary = "Create an activity",
    description = "Create an activity. Requires any one of the following roles ['ACTIVITY_HUB', 'ACTIVITY_HUB_LEAD', 'ACTIVITY_ADMIN'].",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "The activity was created.",
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
    ]
  )
  @PreAuthorize("hasAnyRole('ACTIVITY_HUB', 'ACTIVITY_HUB_LEAD', 'ACTIVITY_ADMIN')")
  fun create(
    principal: Principal,
    @Valid @RequestBody @Parameter(
      description = "The create request with the new activity details",
      required = true
    ) activity: ActivityCreateRequest
  ): Activity = activityService.createActivity(activity, principal.name)

  @Operation(
    summary = "Get the capacity and number of allocated slots in an activity",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Activity capacity",
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
        description = "Activity ID not found",
        content = [
          Content(
            mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)
          )
        ],
      )
    ]
  )
  @GetMapping(value = ["/{activityId}/capacity"])
  @ResponseBody
  fun getActivityCapacity(@PathVariable("activityId") activityId: Long): CapacityAndAllocated =
    capacityService.getActivityCapacityAndAllocated(activityId)

  @Operation(
    summary = "Get the capacity and number of allocated slots in an activity",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Activity schedules",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ActivityScheduleLite::class)
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
        description = "Activity ID not found",
        content = [
          Content(
            mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)
          )
        ],
      )
    ]
  )
  @GetMapping(value = ["/{activityId}/schedules"])
  @ResponseBody
  fun getActivitySchedules(@PathVariable("activityId") activityId: Long): List<ActivityScheduleLite> =
    activityService.getSchedulesForActivity(activityId)
}
