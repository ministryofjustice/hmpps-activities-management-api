package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class PrisonerAllocatedEvent(
  val activityId: Long,
  val activityName: String,
  val prisonerNumber: String,
  val scheduleId: Long,
  val date: LocalDate,
  val startTime: LocalTime,
  val endTime: LocalTime,
  createdAt: LocalDateTime,

) : AuditableEvent(
  auditEventType = AuditEventType.PRISONER_ALLOCATED,
  details = "Prisoner $prisonerNumber was allocated to " +
    "activity '$activityName'($activityId) scheduled on $date between $startTime and $endTime (scheduleId = $scheduleId)",
  createdAt = createdAt,
),
  HmppsAuditable
