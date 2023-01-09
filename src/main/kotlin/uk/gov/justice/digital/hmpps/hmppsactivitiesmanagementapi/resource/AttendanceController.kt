package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AttendanceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AttendancesService

@RestController
@RequestMapping("/attendances", produces = [MediaType.APPLICATION_JSON_VALUE])
class AttendanceController(private val attendancesService: AttendancesService) {

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
    ]
  )
  @PreAuthorize("hasAnyRole('ACTIVITY_ADMIN')")
  fun markAttendances(@RequestBody attendances: List<AttendanceUpdateRequest>): ResponseEntity<Any> =
    attendancesService.mark(attendances).let { ResponseEntity.noContent().build() }
}
