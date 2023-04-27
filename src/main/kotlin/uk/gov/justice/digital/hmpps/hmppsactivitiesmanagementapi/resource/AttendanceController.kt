package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AllAttendanceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AttendanceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AttendancesService
import java.security.Principal
import java.time.LocalDate

@RestController
@RequestMapping("/attendances", produces = [MediaType.APPLICATION_JSON_VALUE])
class AttendanceController(private val attendancesService: AttendancesService) {

  @GetMapping(value = ["/{attendanceId}"])
  @ResponseBody
  @Operation(
    summary = "Get an attendance by ID",
    description = "Returns an attendance.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Attendance found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = Attendance::class),
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
        description = "The attendance was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getAttendanceById(
    @PathVariable("attendanceId") instanceId: Long,
  ): Attendance = attendancesService.getAttendanceById(instanceId)

  @GetMapping(value = ["/summary/{prisonCode}/{sessionDate}"])
  @ResponseBody
  @Operation(
    summary = "Get a daily summary of attendances",
    description = "Returns an attendance summary.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Attendance Summary found",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = AllAttendanceSummary::class)),
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
  fun getAttendanceSummaryByDate(
    @PathVariable("prisonCode") prisonCode: String,
    @PathVariable("sessionDate") sessionDate: LocalDate,
  ): List<AllAttendanceSummary> = attendancesService.getAttendanceSummaryByDate(prisonCode, sessionDate)

  @PutMapping
  @ResponseBody
  @Operation(
    summary = "Updates attendance records.",
    description = "Updates the given attendance records with the supplied update request details. Requires the 'ACTIVITY_ADMIN' role.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "The attendance records were updated.",
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
    ],
  )
  @PreAuthorize("hasAnyRole('ACTIVITY_ADMIN')")
  fun markAttendances(
    principal: Principal,
    @RequestBody attendances: List<AttendanceUpdateRequest>,
  ): ResponseEntity<Any> =
    attendancesService.mark(principal.name ?: "", attendances).let { ResponseEntity.noContent().build() }
}
