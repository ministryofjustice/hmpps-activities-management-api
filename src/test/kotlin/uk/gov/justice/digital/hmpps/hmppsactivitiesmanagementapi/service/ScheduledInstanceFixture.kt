package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ScheduledInstanceFixture {
  companion object {
    fun instance(
      id: Long = 1,
      activityScheduleId: Long = 1000 + id,
      activityId: Long = 2000 + id,
      activityCategoryId: Long = 4000 + id,
      activityTierId: Long = 6000 + id,
      date: LocalDate = LocalDate.of(2022, 10, 1),
      startTime: LocalTime = LocalTime.of(12, 0, 0),
      endTime: LocalTime = LocalTime.of(13, 0, 0),
      prisonCode: String = "MDI",
      locationId: Int,
    ): ScheduledInstance = ScheduledInstance(
      id,
      ActivitySchedule(
        activityScheduleId,
        Activity(
          activityId,
          prisonCode,
          ActivityCategory(
            activityCategoryId,
            "ACTIVITY CATEGORY CODE " + activityCategoryId,
            "ACTIVITY CATEGORY DESCRIPTION " + activityCategoryId
          ),
          ActivityTier(
            activityTierId,
            "ACTIVITY TIER CODE " + activityTierId,
            "ACTIVITY TIER DESCRIPTION " + activityTierId
          ),
          mutableListOf(),
          mutableListOf(),
          mutableListOf(),
          null,
          "ACTIVITY SUMMARY " + activityId,
          "ACTIVITY DESCRIPTION " + activityId,
          LocalDate.of(2022, 10, 1),
          null,
          true,
          LocalDateTime.of(2022, 10, 1, 12, 0, 0),
          "CREATED BY"
        ),
        mutableListOf(),
        mutableListOf(),
        mutableListOf(),
        "DESCRIPTION " + activityScheduleId,
        startTime,
        endTime,
        locationId,
        "LOCATION_CODE " + locationId,
        "LOCATION_DESCRIPTION " + locationId,
        10,
        true,
        false,
        true,
        false,
        false,
        false,
        false,
      ),
      mutableListOf(),
      date,
      startTime,
      endTime,
      false,
      null,
      null
    )
  }
}
