package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityPay
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityScheduleSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EligibilityRule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerWaiting
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.RolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

internal fun activityModel(activity: Activity) = transform(activity)

const val moorlandPrisonCode = "MDI"
const val pentonvillePrisonCode = "PVI"

val eligibilityRuleOver21 =
  EligibilityRule(eligibilityRuleId = 1, code = "OVER_21", "The prisoner must be over 21 to attend")
val eligibilityRuleFemale =
  EligibilityRule(eligibilityRuleId = 2, code = "FEMALE_ONLY", "The prisoner must be female to attend")

val lowPayBand = prisonPayBandsLowMediumHigh()[0]
val mediumPayBand = prisonPayBandsLowMediumHigh()[1]

val activeAllocation = activityEntity().schedules().first().allocations().first()

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
  noSchedules: Boolean = false,
  noEligibilityRules: Boolean = false,
  noPayBands: Boolean = false,
  noMinimumEducationLevels: Boolean = false,
) =
  Activity(
    activityId = activityId,
    prisonCode = prisonCode,
    activityCategory = category,
    activityTier = tier,
    summary = summary,
    description = description,
    riskLevel = "High",
    minimumIncentiveNomisCode = "BAS",
    minimumIncentiveLevel = "Basic",
    startDate = startDate,
    endDate = endDate,
    createdTime = timestamp,
    createdBy = "test",
  ).apply {
    if (!noEligibilityRules) {
      this.addEligibilityRule(eligibilityRuleOver21)
    }
    if (!noSchedules) {
      this.addSchedule(activitySchedule(this, activityScheduleId = 1, timestamp))
    }
    waitingList.add(activityWaiting(this, timestamp))
    if (!noPayBands) {
      this.addPay(
        incentiveNomisCode = "BAS",
        incentiveLevel = "Basic",
        payBand = lowPayBand,
        rate = 30,
        pieceRate = 40,
        pieceRateItems = 50,
      )
    }
    if (!noMinimumEducationLevels) {
      this.addMinimumEducationLevel(
        educationLevelCode = "1",
        educationLevelDescription = "Reading Measure 1.0",
      )
    }
  }

internal fun activityCategory() =
  ActivityCategory(
    activityCategoryId = 1,
    code = "category code",
    name = "category name",
    description = "category description",
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
  noAllocations: Boolean = false,
  noInstances: Boolean = false,
) =
  ActivitySchedule(
    activityScheduleId = activityScheduleId,
    activity = activity,
    description = description,
    capacity = 1,
    internalLocationId = 1,
    internalLocationCode = "EDU-ROOM-1",
    internalLocationDescription = "Education - R1",
    startDate = startDate ?: activity.startDate,
    runsOnBankHoliday = runsOnBankHolidays,
  ).apply {
    if (!noAllocations) {
      this.allocatePrisoner(
        prisonerNumber = "A1234AA".toPrisonerNumber(),
        bookingId = 10001,
        payBand = lowPayBand,
        allocatedBy = "Mr Blogs",
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
        ),
      )
    }
    if (!noInstances && !noSlots) {
      this.addInstance(
        sessionDate = timestamp.toLocalDate(),
        slot = this.slots().first(),
      ).apply {
        this.attendances.add(
          Attendance(
            attendanceId = 1,
            scheduledInstance = this,
            prisonerNumber = "A11111A",
            posted = false,
          ),
        )
      }
    }
  }

internal fun allocation() = activitySchedule(activityEntity()).allocations().first()

private fun activityWaiting(
  activity: Activity,
  timestamp: LocalDateTime,
) =
  PrisonerWaiting(
    prisonerWaitingId = 1,
    activity = activity,
    prisonerNumber = "A1234AA",
    priority = 1,
    createdTime = timestamp,
    createdBy = "test",
  )

private fun activityPay(activity: Activity) =
  ActivityPay(
    activityPayId = 1,
    activity = activity,
    incentiveNomisCode = "BAS",
    incentiveLevel = "Basic",
    payBand = lowPayBand,
    rate = 30,
    pieceRate = 40,
    pieceRateItems = 50,
  )

fun rolloutPrison() = RolloutPrison(1, pentonvillePrisonCode, "HMP Pentonville", true, LocalDate.of(2022, 12, 22))

fun prisonRegime() = PrisonRegime(
  1,
  "PVI",
  LocalTime.of(9, 0),
  LocalTime.of(12, 0),
  LocalTime.of(13, 0),
  LocalTime.of(16, 30),
  LocalTime.of(18, 0),
  LocalTime.of(20, 0),
)

// TODO remove offset, this is a hack to work with JSON file test data being used across multiple tests.
fun prisonPayBandsLowMediumHigh(prisonCode: String = moorlandPrisonCode, offset: Long = 0) = listOf(
  PrisonPayBand(
    prisonPayBandId = 1 + offset,
    prisonCode = prisonCode,
    displaySequence = 1,
    payBandAlias = "Low",
    payBandDescription = "Pay band 1 $prisonCode description (lowest)",
    nomisPayBand = 1,
  ),
  PrisonPayBand(
    prisonPayBandId = 2 + offset,
    prisonCode = prisonCode,
    displaySequence = 2,
    payBandAlias = "Medium",
    payBandDescription = "Pay band 2 $prisonCode description",
    nomisPayBand = 2,
  ),
  PrisonPayBand(
    prisonPayBandId = 3 + offset,
    prisonCode = prisonCode,
    displaySequence = 3,
    payBandAlias = "High",
    payBandDescription = "Pay band 3 $prisonCode description (highest)",
    nomisPayBand = 2,
  ),
)
