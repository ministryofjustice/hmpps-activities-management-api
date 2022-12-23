package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import java.time.LocalDate
import java.time.LocalTime

/*
 * Projection from a native query joining scheduled instance, allocation, activity schedule and activity
 */

interface ActivityForPrisonerProjection {
  val scheduledInstanceId: Long
  val sessionDate: LocalDate
  val startTime: LocalTime
  val endTime: LocalTime
  val prisonerNumber: String
  val bookingId: Int?
  val internalLocationId: Int?
  val internalLocationCode: String?
  val internalLocationDescription: String?
  val scheduleDescription: String
  val activityId: Int
  val activityCategory: String
  val activitySummary: String
  val activityDescription: String
}
