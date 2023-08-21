package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AllocationMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.MigrateActivityService

@RestController
@Validated
@RequestMapping("/migrate", produces = [MediaType.APPLICATION_JSON_VALUE])
class MigrateActivityController(
  private val migrateActivityService: MigrateActivityService,
) {

  @PostMapping(value = ["/activity"])
  @Operation(
    summary = "Migrate an activity",
    description = "Migrate an activity. Requires the role 'ROLE_NOMIS_ACTIVITIES'",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The activity was migrated.",
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
  @PreAuthorize("hasAnyRole('NOMIS_ACTIVITIES')")
  fun migrateActivity(
    @Valid
    @RequestBody
    @Parameter(description = "Activity migration request", required = true)
    activityMigrateRequest: ActivityMigrateRequest,
  ) = migrateActivityService.migrateActivity(activityMigrateRequest)

  @PostMapping(value = ["/allocation"])
  @Operation(
    summary = "Migrate an allocation",
    description = "Migrate an allocation from NOMIS. Requires the role 'ROLE_NOMIS_ACTIVITIES'",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The allocation was migrated.",
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
  @PreAuthorize("hasAnyRole('NOMIS_ACTIVITIES')")
  fun migrateAllocation(
    @Valid
    @RequestBody
    @Parameter(description = "Allocation migration request", required = true)
    allocationMigrateRequest: AllocationMigrateRequest,
  ) = migrateActivityService.migrateAllocation(allocationMigrateRequest)

  @DeleteMapping(value = ["/delete-activity/prison/{prisonCode}/id/{activityId}"])
  @Operation(
    summary = "Delete an activity with cascade.",
    description = """
      Deletes an activity and all its child entities including schedule, slots, pay, instances, attendances and allocations.
      Only for use via by migration services to undo a failed migration.
      Requires the role 'ROLE_NOMIS_ACTIVITIES'.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The activity was deleted.",
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
  @PreAuthorize("hasAnyRole('NOMIS_ACTIVITIES')")
  fun deleteActivity(
    @Parameter(description = "The prison code where this activity exists", required = true)
    @PathVariable("prisonCode")
    prisonCode: String,

    @Parameter(description = "The activity ID to remove", required = true)
    @PathVariable("activityId")
    activityId: Long,
  ) = migrateActivityService.deleteActivityCascade(prisonCode, activityId)
}
