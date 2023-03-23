package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse

@RestController
@RequestMapping("/appointment-categories", produces = [MediaType.APPLICATION_JSON_VALUE])
class AppointmentCategoryController(private val appointmentCategoryService: AppointmentCategoryService) {

  @Operation(
    summary = "Get the list of top-level appointment categories",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Appointment categories found",
        content = [Content(mediaType = "application/json", array = ArraySchema(schema = Schema(implementation = AppointmentCategory::class)))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @GetMapping
  @ResponseBody
  fun getAppointmentCategories(
    @RequestParam(
      value = "includeInactive",
      required = false,
    )
    @Parameter(description = "If true will return all appointment categories otherwise only active categories will be returned. Defaults to false.")
    includeInactive: Boolean?,
  ): List<AppointmentCategory> = appointmentCategoryService.getAll(includeInactive ?: false)
}
