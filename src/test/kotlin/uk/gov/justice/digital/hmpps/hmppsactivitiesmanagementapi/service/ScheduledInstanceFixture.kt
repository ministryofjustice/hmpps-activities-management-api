package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object ScheduledInstanceFixture {
  fun instance(
    id: Long = 1,
    activityScheduleId: Long = 1000 + id,
    activityId: Long = 2000 + id,
    activityCategoryId: Long = 4000 + id,
    activityCategoryCode: String = "ACTIVITY CATEGORY CODE $activityCategoryId",
    activityTierId: Long = 6000 + id,
    date: LocalDate = LocalDate.of(2022, 10, 1),
    startTime: LocalTime = LocalTime.of(12, 0, 0),
    endTime: LocalTime = LocalTime.of(13, 0, 0),
    prisonCode: String = "MDI",
    locationId: Int,
  ) = ScheduledInstance(
    scheduledInstanceId = id,
    ActivitySchedule(
      activityScheduleId = activityScheduleId,
      Activity(
        activityId = activityId,
        prisonCode = prisonCode,
        activityCategory = ActivityCategory(
          activityCategoryId,
          activityCategoryCode,
          "ACTIVITY CATEGORY NAME  $activityCategoryId",
          "ACTIVITY CATEGORY DESCRIPTION  $activityCategoryId",
        ),
        activityTier = ActivityTier(
          activityTierId,
          "ACTIVITY TIER CODE $activityTierId",
          "ACTIVITY TIER DESCRIPTION $activityTierId",
        ),
        summary = "ACTIVITY SUMMARY $activityId",
        description = "ACTIVITY DESCRIPTION $activityId",
        startDate = LocalDate.of(2022, 10, 1),
        createdTime = LocalDateTime.of(2022, 10, 1, 12, 0, 0),
        createdBy = "CREATED BY"
      ),
      description = "DESCRIPTION $activityScheduleId",
      startTime = startTime,
      endTime = endTime,
      internalLocationId = locationId,
      internalLocationCode = "LOCATION_CODE $locationId",
      internalLocationDescription = "LOCATION_DESCRIPTION $locationId",
      capacity = 10,
      mondayFlag = true,
      wednesdayFlag = true,
    ),
    sessionDate = date,
    startTime = startTime,
    endTime = endTime,
    cancelled = false,
  )
}
