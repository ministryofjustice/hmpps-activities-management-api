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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityService

@RestController
@Validated
@RequestMapping("/migrate", produces = [MediaType.APPLICATION_JSON_VALUE])
class MigrateActivityController(private val activityService: ActivityService) {

  @PostMapping(value = ["/activity"])
  @Operation(
    summary = "Migrate an activity",
    description = "Migrate an activity. Requires the role 'ACTIVITY_ADMIN'",
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
  @PreAuthorize("hasAnyRole('ACTIVITY_ADMIN')")
  fun migrate(
    @Valid
    @RequestBody
    @Parameter(description = "Activity migration request", required = true)
    activityMigrateRequest: ActivityMigrateRequest,
  ) = activityService.migrateActivity(activityMigrateRequest)
}
