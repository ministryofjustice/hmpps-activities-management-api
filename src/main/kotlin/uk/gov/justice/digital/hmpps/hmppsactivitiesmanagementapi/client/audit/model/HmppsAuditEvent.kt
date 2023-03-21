package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.audit.model

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.SecurityUtils
import java.time.LocalDateTime

data class HmppsAuditEvent(
  val what: String,
  val details: String,
) {
  val who = SecurityUtils.getUserNameForLoggedInUser()
  val `when`: LocalDateTime = LocalDateTime.now()
  val service = "activities-api"
}
