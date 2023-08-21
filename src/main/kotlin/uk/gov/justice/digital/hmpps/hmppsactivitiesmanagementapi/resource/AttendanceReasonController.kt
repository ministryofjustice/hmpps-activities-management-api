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
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AttendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AttendanceReasonService

@RestController
@RequestMapping("/attendance-reasons", produces = [MediaType.APPLICATION_JSON_VALUE])
class AttendanceReasonController(private val attendanceReasonService: AttendanceReasonService) {

  @Operation(
    summary = "Get the list of attendance reasons",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Attendance reasons found",
        content = [Content(mediaType = "application/json", array = ArraySchema(schema = Schema(implementation = AttendanceReason::class)))],
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
  @GetMapping
  @ResponseBody
  @PreAuthorize("hasAnyRole('PRISON', 'ACTIVITY_ADMIN')")
  fun getAttendanceReasons(): List<AttendanceReason> = attendanceReasonService.getAll()
}
