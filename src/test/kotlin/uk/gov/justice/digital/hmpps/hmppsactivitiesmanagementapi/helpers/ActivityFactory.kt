package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityEligibility
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityPay
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityScheduleSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EligibilityRule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerWaiting
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.RolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform
import java.time.LocalDate
import java.time.LocalDateTime

internal fun activityModel(activity: Activity) = transform(activity)

const val moorlandPrisonCode = "MDI"
const val pentonvillePrisonCode = "PVI"

internal fun activityEntity(
  category: ActivityCategory = activityCategory(),
  tier: ActivityTier = activityTier(),
  timestamp: LocalDateTime = LocalDate.now().atStartOfDay(),
  activityId: Long = 1L,
  prisonCode: String = "123",
  summary: String = "Maths",
  description: String = "Maths basic",
  startDate: LocalDate = timestamp.toLocalDate(),
  endDate: LocalDate? = null,
  noSchedules: Boolean = false
) =
  Activity(
    activityId = activityId,
    prisonCode = prisonCode,
    activityCategory = category,
    activityTier = tier,
    summary = summary,
    description = description,
    riskLevel = "High",
    minimumIncentiveLevel = "Basic",
    startDate = startDate,
    endDate = endDate,
    createdTime = timestamp,
    createdBy = "test"
  ).apply {
    eligibilityRules.add(activityEligibilityRule(this))
    if (!noSchedules) {
      this.addSchedule(activitySchedule(this, activityScheduleId = 1, timestamp))
    }
    waitingList.add(activityWaiting(this, timestamp))
    activityPay.add(activityPay(this))
  }

internal fun activityCategory() =
  ActivityCategory(
    activityCategoryId = 1,
    code = "category code",
    name = "category name",
    description = "category description"
  )

internal fun schedule() = activityEntity().schedules().first()

internal fun attendanceReasons() = mapOf(
  "ABS" to AttendanceReason(1, "ABS", "Absent"),
  "ACCAB" to AttendanceReason(2, "ACCAB", "Acceptable absence"),
  "ATT" to AttendanceReason(3, "ATT", "Attended"),
  "CANC" to AttendanceReason(4, "CANC", "Cancelled"),
  "NREQ" to AttendanceReason(5, "NREQ", "Not required"),
  "SUS" to AttendanceReason(6, "SUS", "Suspend"),
  "UNACAB" to AttendanceReason(7, "UNACAB", "Unacceptable absence"),
  "REST" to AttendanceReason(8, "REST", "Rest day (no pay)"),
)

internal fun activityTier() = ActivityTier(activityTierId = 1, code = "T1", description = "Tier 1")

internal fun activityEligibilityRule(activity: Activity): ActivityEligibility {
  val eligibilityRule = EligibilityRule(eligibilityRuleId = 1, code = "code", "rule description")

  return ActivityEligibility(
    activityEligibilityId = 1,
    activity = activity,
    eligibilityRule = eligibilityRule
  )
}

internal fun activitySchedule(
  activity: Activity,
  activityScheduleId: Long = 1,
  timestamp: LocalDateTime = LocalDate.now().atStartOfDay(),
  description: String = "schedule description",
  monday: Boolean = true,
  tuesday: Boolean = false,
  wednesday: Boolean = false,
  thursday: Boolean = false,
  friday: Boolean = false,
  saturday: Boolean = false,
  sunday: Boolean = false,
  runsOnBankHolidays: Boolean = false,
  startDate: LocalDate? = null,
  noSlots: Boolean = false,
  noAllocations: Boolean = false
) =
  ActivitySchedule(
    activityScheduleId = activityScheduleId,
    activity = activity,
    description = description,
    capacity = 1,
    internalLocationId = 1,
    internalLocationCode = "EDU-ROOM-1",
    internalLocationDescription = "Education - R1",
    startDate = startDate ?: activity.startDate
  ).apply {
    this.instances.add(
      ScheduledInstance(
        scheduledInstanceId = 1,
        activitySchedule = this,
        sessionDate = timestamp.toLocalDate(),
        startTime = timestamp.toLocalTime(),
        endTime = timestamp.toLocalTime()
      ).apply {
        this.attendances.add(
          Attendance(
            attendanceId = 1,
            scheduledInstance = this,
            prisonerNumber = "A11111A",
            posted = false
          )
        )
      }
    )
    if (!noAllocations) {
      this.allocations.add(
        Allocation(
          allocationId = 1,
          activitySchedule = this,
          prisonerNumber = "A1234AA",
          bookingId = 10001,
          payBand = prisonPayBands().first(),
          startDate = timestamp.toLocalDate(),
          endDate = null,
          allocatedTime = timestamp,
          allocatedBy = "Mr Blogs",
        )
      )
    }
    if (!noSlots) {
      this.addSlot(
        ActivityScheduleSlot(
          activityScheduleSlotId = 1,
          activitySchedule = this,
          startTime = timestamp.toLocalTime(),
          endTime = timestamp.toLocalTime().plusHours(1),
          mondayFlag = monday,
          tuesdayFlag = tuesday,
          wednesdayFlag = wednesday,
          thursdayFlag = thursday,
          fridayFlag = friday,
          saturdayFlag = saturday,
          sundayFlag = sunday,
          runsOnBankHoliday = runsOnBankHolidays
        )
      )
    }
  }

private fun activityWaiting(
  activity: Activity,
  timestamp: LocalDateTime
) =
  PrisonerWaiting(
    prisonerWaitingId = 1,
    activity = activity,
    prisonerNumber = "A1234AA",
    priority = 1,
    createdTime = timestamp,
    createdBy = "test"
  )

private fun activityPay(activity: Activity) =
  ActivityPay(
    activityPayId = 1,
    activity = activity,
    incentiveLevel = "Basic",
    payBand = prisonPayBands().first(),
    rate = 30,
    pieceRate = 40,
    pieceRateItems = 50
  )

fun rolloutPrison() = RolloutPrison(1, "PVI", "HMP Pentonville", true, LocalDate.of(2022, 12, 22))

// TODO remove offset, this is a hack to work with JSON file test data being used across multiple tests.
fun prisonPayBands(prisonCode: String = moorlandPrisonCode, offset: Long = 0) = listOf(
  PrisonPayBand(
    prisonPayBandId = 1 + offset,
    prisonCode = prisonCode,
    displaySequence = 1,
    payBandAlias = "Low",
    payBandDescription = "Pay band 1 $prisonCode description (lowest)",
    nomisPayBand = 1
  ),
  PrisonPayBand(
    prisonPayBandId = 2 + offset,
    prisonCode = prisonCode,
    displaySequence = 2,
    payBandAlias = "Medium",
    payBandDescription = "Pay band 2 $prisonCode description",
    nomisPayBand = 2
  ),
  PrisonPayBand(
    prisonPayBandId = 3 + offset,
    prisonCode = prisonCode,
    displaySequence = 3,
    payBandAlias = "High",
    payBandDescription = "Pay band 3 $prisonCode description (highest)",
    nomisPayBand = 2
  )
)
