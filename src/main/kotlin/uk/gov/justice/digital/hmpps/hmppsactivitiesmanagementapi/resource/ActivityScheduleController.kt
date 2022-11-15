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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.CapacityAndAllocated
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityScheduleService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.CapacityService
import java.time.LocalDate

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

  @GetMapping(value = ["/{prisonCode}"])
  @ResponseBody
  @Operation(
    summary = "Get a list of activity schedules at a given prison",
    description = "Returns zero or more activity schedules at a given prison.",
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
    ) @Parameter(description = "The internal NOMIS location id of the activity") locationId: Long?,
  ): List<ActivitySchedule> =
    scheduleService.getActivitySchedulesByPrisonCode(
      prisonCode = prisonCode,
      date = date ?: LocalDate.now(),
      timeSlot = timeSlot,
      locationId = locationId
    )
}
