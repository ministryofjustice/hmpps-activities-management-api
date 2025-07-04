package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AdvanceAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.ActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventTier
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
    timeSlot: TimeSlot = TimeSlot.PM,
    startTime: LocalTime = LocalTime.of(12, 0, 0),
    endTime: LocalTime = LocalTime.of(13, 0, 0),
    prisonCode: String = "MDI",
    locationId: Int,
    cancelled: Boolean = false,
    cancelledBy: String? = null,
    cancelledReason: String? = null,
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
        activityTier = EventTier(
          activityTierId,
          "ACTIVITY TIER CODE $activityTierId",
          "ACTIVITY TIER DESCRIPTION $activityTierId",
        ),
        summary = "ACTIVITY SUMMARY $activityId",
        description = "ACTIVITY DESCRIPTION $activityId",
        startDate = LocalDate.of(2022, 10, 1),
        riskLevel = "high",
        createdTime = LocalDateTime.of(2022, 10, 1, 12, 0, 0),
        createdBy = "CREATED BY",
        isPaid = true,
      ),
      description = "DESCRIPTION $activityScheduleId",
      internalLocationId = locationId,
      internalLocationCode = "LOCATION_CODE $locationId",
      internalLocationDescription = "LOCATION_DESCRIPTION $locationId",
      capacity = 10,
      startDate = date,
      scheduleWeeks = 1,
    ),
    attendances = mutableListOf(
      AttendancesFixture.instance(),
      AttendancesFixture.instance(),
      AttendancesFixture.instance(prisonerNumber = "B1234BB"),
      AttendancesFixture.instance(prisonerNumber = "C1234CC"),
    ),
    advanceAttendances = mutableListOf(
      AdvanceAttendancesFixture.instance(),
      AdvanceAttendancesFixture.instance(prisonerNumber = "B1234BB"),
    ),
    sessionDate = date,
    timeSlot = timeSlot,
    startTime = startTime,
    endTime = endTime,
    cancelled = cancelled,
    cancelledBy = cancelledBy,
    cancelledReason = cancelledReason,
  )
}

object AttendancesFixture {
  fun instance(
    id: Long = 1,
    prisonerNumber: String = "A1234AA",
    date: LocalDate = LocalDate.of(2022, 10, 1),
    timeSlot: TimeSlot = TimeSlot.PM,
    startTime: LocalTime = LocalTime.of(12, 0, 0),
    endTime: LocalTime = LocalTime.of(13, 0, 0),
    prisonCode: String = "MDI",
    activityScheduleId: Long = 1000 + id,
    activityId: Long = 2000 + id,
    activityCategoryId: Long = 4000 + id,
    activityCategoryCode: String = "ACTIVITY CATEGORY CODE $activityCategoryId",
    activityTierId: Long = 6000 + id,
  ) = Attendance(
    scheduledInstance = ScheduledInstance(
      activitySchedule = ActivitySchedule(
        activity = Activity(
          prisonCode = prisonCode,
          activityCategory = ActivityCategory(
            activityCategoryId,
            activityCategoryCode,
            "ACTIVITY CATEGORY NAME  $activityCategoryId",
            "ACTIVITY CATEGORY DESCRIPTION  $activityCategoryId",
          ),
          activityTier = EventTier(
            activityTierId,
            "ACTIVITY TIER CODE $activityTierId",
            "ACTIVITY TIER DESCRIPTION $activityTierId",
          ),
          summary = "ACTIVITY SUMMARY $activityId",
          description = "ACTIVITY DESCRIPTION $activityId",
          startDate = date,
          riskLevel = "high",
          createdTime = LocalDateTime.of(2022, 10, 1, 12, 0, 0),
          createdBy = "CREATED BY",
          isPaid = true,
        ),
        description = "DESCRIPTION $activityScheduleId",
        capacity = 10,
        startDate = date,
        scheduleWeeks = 1,
      ),
      sessionDate = date,
      startTime = startTime,
      endTime = endTime,
      timeSlot = timeSlot,
    ),
    prisonerNumber = prisonerNumber,
  )
}

object AdvanceAttendancesFixture {
  fun instance(
    id: Long = 1,
    prisonerNumber: String = "A1234AA",
    date: LocalDate = LocalDate.of(2022, 10, 1),
    timeSlot: TimeSlot = TimeSlot.PM,
    startTime: LocalTime = LocalTime.of(12, 0, 0),
    endTime: LocalTime = LocalTime.of(13, 0, 0),
    prisonCode: String = "MDI",
    activityScheduleId: Long = 1000 + id,
    activityId: Long = 2000 + id,
    activityCategoryId: Long = 4000 + id,
    activityCategoryCode: String = "ACTIVITY CATEGORY CODE $activityCategoryId",
    activityTierId: Long = 6000 + id,
  ) = AdvanceAttendance(
    scheduledInstance = ScheduledInstance(
      activitySchedule = ActivitySchedule(
        activity = Activity(
          prisonCode = prisonCode,
          activityCategory = ActivityCategory(
            activityCategoryId,
            activityCategoryCode,
            "ACTIVITY CATEGORY NAME  $activityCategoryId",
            "ACTIVITY CATEGORY DESCRIPTION  $activityCategoryId",
          ),
          activityTier = EventTier(
            activityTierId,
            "ACTIVITY TIER CODE $activityTierId",
            "ACTIVITY TIER DESCRIPTION $activityTierId",
          ),
          summary = "ACTIVITY SUMMARY $activityId",
          description = "ACTIVITY DESCRIPTION $activityId",
          startDate = date,
          riskLevel = "high",
          createdTime = LocalDateTime.of(2022, 10, 1, 12, 0, 0),
          createdBy = "CREATED BY",
          isPaid = true,
        ),
        description = "DESCRIPTION $activityScheduleId",
        capacity = 10,
        startDate = date,
        scheduleWeeks = 1,
      ),
      sessionDate = date,
      startTime = startTime,
      endTime = endTime,
      timeSlot = timeSlot,
    ),
    prisonerNumber = prisonerNumber,
    issuePayment = true,
    recordedTime = LocalDateTime.of(2022, 10, 1, 12, 0, 0),
    recordedBy = "Joe Bloggs",
  )
}
