package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import java.time.LocalDateTime

class PrisonerRejectedFromWaitingListEvent(
  val activityId: Long,
  val activityName: String,
  val prisonerNumber: String,
  val prisonerFirstName: String,
  val prisonerLastName: String,
  createdAt: LocalDateTime,

) : AuditableEvent(createdAt), HmppsAuditable {

  override fun type() = AuditEventType.PRISONER_REJECTED_FROM_WAITING_LIST

  override fun toString() = "Prisoner $prisonerNumber $prisonerLastName, $prisonerFirstName was rejected from the waiting list for " +
    "activity '$activityName'($activityId). ${super.toString()}"
}
