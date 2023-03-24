package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import java.time.LocalDateTime

class PrisonerAddedToWaitingListEvent(
  val activityId: Long,
  val activityName: String,
  val prisonerNumber: String,
  val prisonerFirstName: String,
  val prisonerLastName: String,
  createdAt: LocalDateTime,

) : AuditableEvent(
  auditEventType = AuditEventType.PRISONER_ADDED_TO_WAITING_LIST,
  details = "Prisoner $prisonerNumber $prisonerLastName, $prisonerFirstName was added to the waiting list for " +
    "activity '$activityName'($activityId)",
  createdAt = createdAt,
),
  HmppsAuditable
