package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import java.time.LocalDateTime

class PrisonerAcceptedFromWaitingListEvent(
  val activityId: Long,
  val activityName: String,
  val prisonerNumber: String,
  val prisonerFirstName: String,
  val prisonerLastName: String,
  createdAt: LocalDateTime,

) : AuditableEvent(createdAt), HmppsAuditable {

  override fun type() = AuditEventType.PRISONER_ACCEPTED_FROM_WAITING_LIST

  override fun toString() = "Prisoner $prisonerNumber $prisonerLastName, $prisonerFirstName was accepted onto " +
    "activity '$activityName'($activityId) from the waiting list. ${super.toString()}"
}
