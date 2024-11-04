package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivitySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.PrisonRegimeService

@RestController
@RequestMapping("/prison", produces = [MediaType.APPLICATION_JSON_VALUE])
class PrisonController(
  private val activityService: ActivityService,
  private val prisonRegimeService: PrisonRegimeService,
) {

  @Operation(
    summary = "Get list of activities running at a specified prison. " +
      "Optionally and by default, only currently LIVE activities are returned",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Activities",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = ActivitySummary::class)),
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
  @GetMapping(value = ["/{prisonCode}/activities"])
  @ResponseBody
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN', 'ACTIVITIES_MANAGEMENT__RO')")
  fun getActivities(
    @PathVariable("prisonCode") prisonCode: String,
    @RequestParam(value = "excludeArchived", required = false, defaultValue = "true") excludeArchived: Boolean,
  ): List<ActivitySummary> = activityService.getActivitiesInPrison(prisonCode, excludeArchived)

  @GetMapping(value = ["/{prisonCode}/prison-pay-bands"])
  @ResponseBody
  @Operation(
    summary = "Get a list of pay bands at a given prison",
    description = "Returns the pay bands at a given prison or a default list of values if none present.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Prison pay bands found",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = PrisonPayBand::class)),
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
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN')")
  fun getPrisonPayBands(
    @PathVariable("prisonCode")
    prisonCode: String,
  ): List<PrisonPayBand> = prisonRegimeService.getPayBandsForPrison(prisonCode)

  @GetMapping(value = ["/prison-regime/{prisonCode}"])
  @ResponseBody
  @Operation(
    summary = "Get a prison regime by its code",
    description = "Returns a single prison regime and its details by its unique prison code.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Prison regime found",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = PrisonRegime::class)),
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
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN')")
  fun getPrisonRegimeByPrisonCode(@PathVariable("prisonCode") prisonCode: String): List<PrisonRegime> =
    prisonRegimeService.getPrisonRegimeByPrisonCode(prisonCode)
}
