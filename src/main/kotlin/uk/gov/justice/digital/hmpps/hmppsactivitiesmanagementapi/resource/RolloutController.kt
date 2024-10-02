package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RolloutPrisonPlan
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.PrisonRegimeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService
import java.time.DayOfWeek
import java.time.LocalTime

@Schema(description = "prison regime slots for a day of the week")
data class PrisonRegimeSlot(
  val dayOfWeek: DayOfWeek,
  val amStart: LocalTime,
  val amFinish: LocalTime,
  val pmStart: LocalTime,
  val pmFinish: LocalTime,
  val edStart: LocalTime,
  val edFinish: LocalTime,
)

@RestController
@RequestMapping("/rollout", produces = [MediaType.APPLICATION_JSON_VALUE])
class RolloutController(
  private val rolloutService: RolloutPrisonService,
  private val prisonRegimeService: PrisonRegimeService,
) {

  @GetMapping(value = ["/{prisonCode}"])
  @Operation(
    summary = "Get a prison's rollout plan by prison code",
    description = "Returns a single prison and its activities management service rollout plan by its unique code.",
  )
  @PreAuthorize("hasRole('ACTIVITY_ADMIN')")
  @ResponseBody
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Prison rollout plan found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = RolloutPrisonPlan::class),
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
  fun getPrisonByCode(@PathVariable("prisonCode") prisonCode: String): RolloutPrisonPlan =
    rolloutService.getByPrisonCode(prisonCode)

  @Operation(
    summary = "Get all rollout prisons",
    description = "Returns a list of all rolled out prisons.",
  )
  @GetMapping
  @PreAuthorize("hasRole('ACTIVITY_ADMIN')")
  @ResponseBody
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "List of prisons that are rolled out",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = RolloutPrisonPlan::class)),
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
  fun getRolledOutPrisons(): List<RolloutPrisonPlan> =
    rolloutService.getRolloutPrisons()

  @Operation(
    summary = "Creates a prison regime for a given prison",
    description = "If a regine exists it will overwrite it.  Called via migration dashboard only",
  )
  @PostMapping("/prison-regime/{agencyId}")
  @PreAuthorize("hasRole('MIGRATE_ACTIVITIES')")
  @ResponseStatus(HttpStatus.CREATED)
  fun setPrisonRegimeSlots(
    @PathVariable("agencyId") agencyId: String,
    @RequestBody slots: List<PrisonRegimeSlot>,
  ): List<PrisonRegime> = prisonRegimeService.setPrisonRegime(agencyId = agencyId, slots = slots)
}
