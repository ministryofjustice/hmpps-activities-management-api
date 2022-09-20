package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityService

@RestController
@RequestMapping("/activities", produces = [MediaType.APPLICATION_JSON_VALUE])
class ActivityController(private val activityService: ActivityService) {

  @GetMapping(value = ["/id/{activityId}"])
  fun getActivityById(@PathVariable("activityId") activityId: Long): Activity =
    activityService.getActivityById(activityId)
}
