package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.CapacityAndAllocated
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityScheduleService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.CapacityService
import java.time.LocalDate

// TODO add pre-auth annotations to enforce roles when we have them

@RestController
@RequestMapping("/prison", produces = [MediaType.APPLICATION_JSON_VALUE])
class PrisonController(
  private val capacityService: CapacityService,
  private val activityService: ActivityService,
  private val scheduleService: ActivityScheduleService,
) {

  @Operation(
    summary = "Get list of activities at a specified prison",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Activities",
        content = [Content(mediaType = "application/json", array = ArraySchema(schema = Schema(implementation = ActivityLite::class)))],
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
      )
    ]
  )
  @GetMapping(value = ["/{prisonCode}/activities"])
  @ResponseBody
  fun getActivities(
    @PathVariable("prisonCode") prisonCode: String,
  ): List<ActivityLite> = activityService.getActivitiesInPrison(prisonCode)

  @Operation(
    summary = "Get list of activities within a category at a specified prison",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Activities within the category",
        content = [Content(mediaType = "application/json", array = ArraySchema(schema = Schema(implementation = ActivityLite::class)))],
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
        description = "Category ID not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      )
    ]
  )
  @GetMapping(value = ["/{prisonCode}/activity-categories/{categoryId}/activities"])
  @ResponseBody
  fun getActivitiesInCategory(
    @PathVariable("prisonCode") prisonCode: String,
    @PathVariable("categoryId") categoryId: Long,
  ): List<ActivityLite> = activityService.getActivitiesByCategoryInPrison(prisonCode, categoryId)

  @Operation(
    summary = "Get the capacity and number of allocated slots in an activity category within a prison",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Activity category capacity",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = CapacityAndAllocated::class))],
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
        description = "Category ID not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      )
    ]
  )
  @GetMapping(value = ["/{prisonCode}/activity-categories/{categoryId}/capacity"])
  @ResponseBody
  fun getActivityCategoryCapacity(
    @PathVariable("prisonCode") prisonCode: String,
    @PathVariable("categoryId") categoryId: Long,
  ): CapacityAndAllocated = capacityService.getActivityCategoryCapacityAndAllocated(prisonCode, categoryId)

  @GetMapping(value = ["/{prisonCode}/locations"])
  @ResponseBody
  @Operation(
    summary = "Get scheduled prison locations",
    description = "Returns a list of zero or more scheduled prison locations for the supplied criteria.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Locations found",
        content = [Content(mediaType = "application/json", array = ArraySchema(schema = Schema(implementation = InternalLocation::class)))],
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
  fun getScheduledPrisonLocations(
    @PathVariable("prisonCode")
    prisonCode: String,
    @RequestParam(value = "date", required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "Date of activity, default today")
    date: LocalDate?,
    @RequestParam(value = "timeSlot", required = false)
    @Parameter(description = "AM, PM or ED")
    timeSlot: TimeSlot?
  ): List<InternalLocation> =
    scheduleService.getScheduledInternalLocations(prisonCode, date ?: LocalDate.now(), timeSlot)

  @GetMapping(value = ["/{prisonCode}/schedules"])
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
      ),
    ]
  )
  fun getSchedulesByPrisonCode(
    @PathVariable("prisonCode") prisonCode: String,
    @RequestParam(
      value = "date",
      required = false
    ) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Parameter(description = "Date of activity, default today") date: LocalDate?,
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
