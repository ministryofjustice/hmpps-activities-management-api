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
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityPayHistory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityService
import java.security.Principal
import java.time.LocalDate

@RestController
@Validated
@RequestMapping("/activities", produces = [MediaType.APPLICATION_JSON_VALUE])
class ActivityController(
  private val activityService: ActivityService,
) {

  @GetMapping(value = ["/{activityId}/filtered"])
  @ResponseBody
  @Operation(
    summary = "Get an activity by its ID with limited instances (by date)",
    description = "Returns a single activity by activity ID with limited instances.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Activity found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = Activity::class),
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
        description = "The activity for this ID was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN', 'NOMIS_ACTIVITIES')")
  fun getActivityByIdWithFilters(
    @PathVariable("activityId") activityId: Long,
    @RequestParam(value = "earliestSessionDate", required = false)
    @Parameter(description = "The date of the earliest scheduled instances to include. Defaults to newer than 1 month ago.")
    earliestSessionDate: LocalDate?,
  ) = activityService.getActivityByIdWithFilters(activityId, earliestSessionDate)

  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping
  @Operation(
    summary = "Create an activity",
    description = "Create an activity",
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
    ],
  )
  @PreAuthorize("hasAnyRole('ACTIVITY_HUB', 'ACTIVITY_ADMIN')")
  @CaseloadHeader
  fun create(
    principal: Principal,
    @Valid
    @RequestBody
    @Parameter(description = "The create request with the new activity details", required = true)
    activity: ActivityCreateRequest,
  ): Activity = activityService.createActivity(activity, principal.name)

  @Operation(
    summary = "Get the capacity and number of allocated slots in an activity",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Activity schedules",
        useReturnTypeSchema = true,
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = ActivityScheduleLite::class)),
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
        description = "Activity ID not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @GetMapping(value = ["/{activityId}/schedules"])
  @ResponseBody
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN')")
  fun getActivitySchedules(@PathVariable("activityId") activityId: Long): List<ActivityScheduleLite> = activityService.getSchedulesForActivity(activityId)

  @ResponseStatus(HttpStatus.ACCEPTED)
  @PatchMapping(value = ["/{prisonCode}/activityId/{activityId}"])
  @Operation(
    summary = "Update an activity",
    description = "Update an activity",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "202",
        description = "The activity was updated.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = Activity::class),
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
        description = "Activity ID not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ACTIVITY_HUB', 'ACTIVITY_ADMIN')")
  @CaseloadHeader
  fun update(
    @PathVariable("prisonCode") prisonCode: String,
    @PathVariable("activityId") activityId: Long,
    principal: Principal,
    @Valid
    @RequestBody
    @Parameter(
      description = "The update request with the new activity details",
      required = true,
    )
    activity: ActivityUpdateRequest,
  ): Activity = activityService.updateActivity(prisonCode, activityId, activity, principal.name)

  @GetMapping(value = ["/{activityId}/pay-history"])
  @ResponseBody
  @Operation(
    summary = "Get the complete pay rate history for an activity by its ID",
    description = "Returns full list of pay rate history for a single activity by activity ID",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Activity found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = Activity::class),
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
        description = "The activity for this ID was not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_HUB')")
  fun getActivityPayHistory(
    @PathVariable("activityId") activityId: Long,
  ): List<ActivityPayHistory> = activityService.getActivityPayHistory(activityId)
}
