package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import java.time.LocalDateTime

class PrisonerAcceptedFromWaitingListEvent(
  val activityId: Long,
  val activityName: String,
  val prisonerNumber: String,
  createdAt: LocalDateTime,

) : AuditableEvent(
  auditEventType = AuditEventType.PRISONER_ACCEPTED_FROM_WAITING_LIST,
  details = "Prisoner $prisonerNumber was accepted onto " +
    "activity '$activityName'($activityId) from the waiting list",
  createdAt = createdAt,
),
  HmppsAuditable
