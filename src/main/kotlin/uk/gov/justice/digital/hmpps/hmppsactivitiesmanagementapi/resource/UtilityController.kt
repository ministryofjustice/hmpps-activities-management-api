package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PublishEventUtilityModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityLocationService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.MigrateCaseNotesUUIDService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService

// These endpoints are secured in the ingress rather than the app so that they can be called from
// within the namespace without requiring authentication

@RestController
@ProtectedByIngress
@Validated
@RequestMapping("/utility", produces = [MediaType.TEXT_PLAIN_VALUE])
class UtilityController(
  private val outboundEventsService: OutboundEventsService,
  private val activityLocationService: ActivityLocationService,
  private val migrateCaseNotesUUIDService: MigrateCaseNotesUUIDService,
) {

  @PostMapping(value = ["/publish-events"])
  @Operation(
    summary = "Publish an event to the domain events SNS topic.",
    description = "Can only be accessed from within the ingress. Requests from elsewhere will result in a 401 response code.",
  )
  @ResponseBody
  @ResponseStatus(HttpStatus.CREATED)
  fun publishDomainEvent(
    @Valid
    @RequestBody
    publishEventUtilityModel: PublishEventUtilityModel,
  ): String {
    publishEventUtilityModel.identifiers!!.toSet().forEach {
      outboundEventsService.send(publishEventUtilityModel.outboundEvent!!, it)
    }

    return "Domain event ${publishEventUtilityModel.outboundEvent} published"
  }

  @GetMapping(value = ["invalid-activity-locations"], produces = [MediaType.TEXT_PLAIN_VALUE])
  @ResponseBody
  @Operation(
    summary = "Get any activities with invalid locations",
    description = "Return any activities with invalid locations.",
  )
  fun getInvalidActivityLocations(): String {
    val builder = StringBuilder()
    builder.appendLine("Prison Code,Activity ID,Activity Description,Internal Location ID,Internal Location Code,Internal Location Description,DPS Location ID")
    activityLocationService.getInvalidActivityLocations().forEach {
      builder.appendLine("${it.getPrisonCode()},${it.getActivityId()},${it.getActivityDescription()},${it.getInternalLocationId()},${it.getInternalLocationCode()},${it.getInternalLocationDescription()},${it.getDpsLocationId()}")
    }
    return builder.toString()
  }

  @PostMapping(value = ["/update-case-note-uuid"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Update case note UUID for all the existing case notes.",
    description = "Can only be accessed from within the ingress. Requests from elsewhere will result in a 401 response code.",
  )
  @ResponseBody
  @ResponseStatus(HttpStatus.OK)
  fun updateCaseNoteUUID() = migrateCaseNotesUUIDService.updateCaseNoteUUID()
}
