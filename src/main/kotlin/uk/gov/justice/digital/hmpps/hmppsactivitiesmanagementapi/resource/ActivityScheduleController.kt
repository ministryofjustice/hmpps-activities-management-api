package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityScheduleService
import java.time.LocalDate

// TODO add pre-auth annotations to enforce roles when we have them

@RestController
@RequestMapping("/schedules", produces = [MediaType.APPLICATION_JSON_VALUE])
class ActivityScheduleController(private val scheduleService: ActivityScheduleService) {

  @GetMapping(value = ["/{prisonCode}"])
  @ResponseBody
  @Operation(
    summary = "Get a list of activity schedules on a given date",
    description = "Returns zero or more activity schedules at a given prison on a particular date if there are any.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Activity schedules found",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = ActivitySchedule::class))
          )
        ],
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
        description = "The activity schedules for this prison were not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      )
    ]
  )
  fun getSchedulesByPrisonCode(
    @PathVariable("prisonCode") prisonCode: String,
    @RequestParam(
      value = "date",
      required = false
    ) @DateTimeFormat(iso = ISO.DATE) @Parameter(description = "Date of activity, default today") date: LocalDate?,
    @RequestParam(
      value = "timeSlot",
      required = false
    ) @Parameter(description = "AM, PM or ED") timeSlot: TimeSlot?,
    @RequestParam(
      value = "locationId",
      required = false
    ) @Parameter(description = "The location id of the activity") locationId: Long?,
  ): List<ActivitySchedule> =
    // TODO location ID is currently ignored.  This WIP.
    scheduleService.getActivitySchedulesByPrisonCode(
      prisonCode = prisonCode,
      date = date ?: LocalDate.now(),
      timeSlot = timeSlot
    )

  @GetMapping(value = ["/{prisonCode}/locations"])
  @ResponseBody
  @Operation(
    summary = "Get a list of activity schedule locations for a given prison",
    description = "Returns zero or more locations at a given prison on a particular date if there are any.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Activity schedule locations found",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = Location::class))
          )
        ],
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
        description = "The locations for this prison were not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      )
    ]
  )
  // TODO this is just a stub which returns some fake locations
  fun getScheduleLocationsByPrisonCode(
    @PathVariable("prisonCode") prisonCode: String,
    @RequestParam(
      value = "date",
      required = false
    ) @DateTimeFormat(iso = ISO.DATE) @Parameter(description = "Date of activity, default today") date: LocalDate?,
    @RequestParam(
      value = "timeSlot",
      required = false
    ) @Parameter(description = "AM, PM or ED") timeSlot: TimeSlot?
  ): List<Location> = listOf(
    Location(1, "EDU-ROOM-1", "Education - R1"),
    Location(2, "EDU-ROOM-2", "Education - R2"),
    Location(3, "EDU-ROOM-3", "Education - R3"),
  )
}
