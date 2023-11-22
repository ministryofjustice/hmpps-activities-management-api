package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityScheduleSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityState
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AllAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceHistory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EligibilityRule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventOrganiser
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerScheduledActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.RolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Slot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityMinimumEducationLevelCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

internal fun activityModel(activity: Activity) = transform(activity)

const val moorlandPrisonCode = "MDI"
const val pentonvillePrisonCode = "PVI"
const val risleyPrisonCode = "RSI"

val eligibilityRuleOver21 = EligibilityRule(eligibilityRuleId = 1, code = "OVER_21", "The prisoner must be over 21 to attend")
val eligibilityRuleFemale = EligibilityRule(eligibilityRuleId = 2, code = "FEMALE_ONLY", "The prisoner must be female to attend")

val lowPayBand = prisonPayBandsLowMediumHigh()[0]
val mediumPayBand = prisonPayBandsLowMediumHigh()[1]

val activeAllocation = activityEntity().schedules().first().allocations().first()

internal fun activityEntity(
  category: ActivityCategory = activityCategory(),
  tier: EventTier = eventTier(),
  organiser: EventOrganiser = eventOrganiser(),
  timestamp: LocalDateTime = LocalDate.now().atStartOfDay(),
  activityId: Long = 1L,
  prisonCode: String = "MDI",
  summary: String = "Maths",
  description: String = "Maths basic",
  startDate: LocalDate = timestamp.toLocalDate(),
  endDate: LocalDate? = null,
  noSchedules: Boolean = false,
  noEligibilityRules: Boolean = false,
  noPayBands: Boolean = false,
  noMinimumEducationLevels: Boolean = false,
  inCell: Boolean = false,
  onWing: Boolean = false,
  riskLevel: String = "high",
  paid: Boolean = true,
) =
  Activity(
    activityId = activityId,
    prisonCode = prisonCode,
    activityCategory = category,
    activityTier = tier,
    summary = summary,
    description = description,
    riskLevel = riskLevel,
    minimumIncentiveNomisCode = "BAS",
    minimumIncentiveLevel = "Basic",
    startDate = startDate,
    createdTime = timestamp,
    createdBy = "test",
    inCell = inCell,
    onWing = onWing,
    paid = paid,
  ).apply {
    this.organiser = organiser
    this.endDate = endDate
    if (!noEligibilityRules) {
      this.addEligibilityRule(eligibilityRuleOver21)
    }
    if (!noSchedules) {
      this.addSchedule(activitySchedule(this, activityScheduleId = activityId, timestamp))
    }
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
        studyAreaCode = "ENGLA",
        studyAreaDescription = "English Language",
      )
    }
  }

internal fun activitySummary(
  category: ActivityCategory = activityCategory(),
  timestamp: LocalDateTime = LocalDate.now().atStartOfDay(),
  activityId: Long = 1L,
  prisonCode: String = "MDI",
  activityName: String = "Maths",
  capacity: Int = 20,
  allocated: Int = 10,
  waitlisted: Int = 2,
  activityState: ActivityState = ActivityState.LIVE,
) =
  ActivitySummary(
    id = activityId,
    prisonCode = prisonCode,
    activityName = activityName,
    capacity = capacity,
    allocated = allocated,
    waitlisted = waitlisted,
    createdTime = timestamp,
    activityState = activityState,
    activityCategory = category,
  )

internal fun activityCategory(code: String = "category code") =
  ActivityCategory(
    activityCategoryId = 1,
    code = code,
    name = "category name",
    description = "category description",
  )

internal fun activityCategory2(code: String = "category code 2") =
  ActivityCategory(
    activityCategoryId = 2,
    code = code,
    name = "category name 2",
    description = "category description 2",
  )

internal fun schedule(prisonCode: String = moorlandPrisonCode) = activityEntity(prisonCode = prisonCode).schedules().first()

internal fun attendanceReasons() = mapOf(
  "SICK" to AttendanceReason(1, AttendanceReasonEnum.SICK, "Sick", false, true, true, false, false, false, true, 1, "Maps to ACCAB in NOMIS"),
  "REFUSED" to AttendanceReason(2, AttendanceReasonEnum.REFUSED, "Refused to attend", false, false, false, true, true, false, true, 2, "Maps to UNACAB in NOMIS"),
  "NOT_REQUIRED" to AttendanceReason(3, AttendanceReasonEnum.NOT_REQUIRED, "Not required or excused", false, false, false, false, false, false, true, 3, "Maps to ACCAB in NOMIS"),
  "REST" to AttendanceReason(4, AttendanceReasonEnum.REST, "Rest day", false, true, false, false, false, false, true, 4, "Maps to ACCAB in NOMIS"),
  "CLASH" to AttendanceReason(5, AttendanceReasonEnum.CLASH, "Prisoner's schedule shows another activity", false, false, false, false, false, false, true, 5, "Maps to ACCAB in NOMIS"),
  "OTHER" to AttendanceReason(6, AttendanceReasonEnum.OTHER, "Other absence reason not listed", false, true, false, false, false, true, true, 6, "Maps to UNACAB in NOMIS"),
  "SUSPENDED" to AttendanceReason(7, AttendanceReasonEnum.SUSPENDED, "Suspended", false, false, false, false, false, false, true, null, "Maps to ACCAB in NOMIS"),
  "CANCELLED" to AttendanceReason(8, AttendanceReasonEnum.CANCELLED, "Cancelled", false, false, false, false, false, false, true, null, "Maps to ACCAB in NOMIS"),
  "ATTENDED" to AttendanceReason(9, AttendanceReasonEnum.ATTENDED, "Attended", true, false, false, false, false, false, false, null, "Maps to ATT in NOMIS"),
)

internal fun attendanceReason(reason: AttendanceReasonEnum = AttendanceReasonEnum.ATTENDED) = attendanceReasons()[reason.name]!!

internal fun eventTier(
  eventTierId: Long = 2,
  code: String = "TIER_2",
  description: String = "Tier 2",
) = EventTier(eventTierId = eventTierId, code = code, description = description)

internal fun foundationTier() = eventTier(
  eventTierId = 3,
  code = "FOUNDATION",
  description = "Routine activities also called \"Foundation\"",
)

internal fun eventOrganiser(
  eventOrganiserId: Long = 1,
  code: String = "PRISON_STAFF",
  description: String = "Prison staff",
) = EventOrganiser(eventOrganiserId = eventOrganiserId, code = code, description = description)

internal fun activitySchedule(
  activity: Activity,
  activityScheduleId: Long = 1,
  timestamp: LocalDateTime = LocalDate.now().atStartOfDay(),
  description: String = "schedule description",
  scheduleWeeks: Int = 1,
  daysOfWeek: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY),
  runsOnBankHolidays: Boolean = false,
  startDate: LocalDate? = null,
  endDate: LocalDate? = null,
  noSlots: Boolean = false,
  noAllocations: Boolean = false,
  noInstances: Boolean = false,
  noExclusions: Boolean = false,
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
    scheduleWeeks = scheduleWeeks,
  ).apply {
    this.endDate = endDate ?: activity.endDate
    if (!noAllocations) {
      this.allocatePrisoner(
        prisonerNumber = "A1234AA".toPrisonerNumber(),
        bookingId = 10001,
        payBand = lowPayBand,
        allocatedBy = "Mr Blogs",
        startDate = startDate ?: activity.startDate,
      )
    }
    if (!noSlots) {
      val slot = this.addSlot(1, timestamp.toLocalTime(), timestamp.toLocalTime().plusHours(1), daysOfWeek)
      if (!noAllocations && !noExclusions) {
        this.allocatePrisoner(
          prisonerNumber = "A1111BB".toPrisonerNumber(),
          bookingId = 20002,
          payBand = lowPayBand,
          allocatedBy = "Mr Blogs",
          startDate = startDate ?: activity.startDate,
        ).apply { this.updateExclusion(slot, daysOfWeek) }
      }
    }
    if (!noInstances && !noSlots) {
      this.addInstance(
        sessionDate = this.startDate,
        slot = this.slots().first(),
      ).apply {
        this.attendances.add(
          Attendance(
            attendanceId = 1,
            scheduledInstance = this,
            prisonerNumber = "A1234AA",
            recordedBy = "Joe Bloggs",
            recordedTime = LocalDate.now().atStartOfDay(),
          ).apply {
            this.addHistory(
              AttendanceHistory(
                attendance = this,
                attendanceHistoryId = 1,
                attendanceReason = AttendanceReason(
                  9,
                  AttendanceReasonEnum.ATTENDED,
                  "Previous Desc",
                  false,
                  true,
                  true,
                  false,
                  false,
                  false,
                  true,
                  1,
                  "some note",
                ),
                comment = "previous comment",
                recordedBy = "Joe Bloggs",
                recordedTime = LocalDate.now().atStartOfDay(),
              ),
            )
          },
        )
      }
    }
  }

internal fun allocation(startDate: LocalDate? = null) =
  startDate
    ?.let { activitySchedule(activityEntity(startDate = it)).allocations().first() }
    ?: activitySchedule(activityEntity()).allocations().first()

internal fun deallocation(endDate: LocalDate? = null) =
  endDate
    ?.let { activitySchedule(activityEntity(endDate = it)).allocations().first() }
    ?: activitySchedule(activityEntity()).allocations().first()

fun rolloutPrison() = RolloutPrison(
  1,
  pentonvillePrisonCode,
  "HMP Pentonville",
  true,
  LocalDate.of(2022, 12, 22),
  true,
  LocalDate.of(2022, 12, 23),
)

fun prisonRegime(
  prisonCode: String = pentonvillePrisonCode,
) = PrisonRegime(
  1,
  prisonCode,
  LocalTime.of(9, 0),
  LocalTime.of(12, 0),
  LocalTime.of(13, 0),
  LocalTime.of(16, 30),
  LocalTime.of(18, 0),
  LocalTime.of(20, 0),
  1,
)

fun prisonPayBandsLowMediumHigh(prisonCode: String = moorlandPrisonCode) = listOf(
  PrisonPayBand(
    prisonPayBandId = 1,
    prisonCode = prisonCode,
    displaySequence = 1,
    payBandAlias = "Low",
    payBandDescription = "Pay band 1 $prisonCode description (lowest)",
    nomisPayBand = 1,
  ),
  PrisonPayBand(
    prisonPayBandId = 2,
    prisonCode = prisonCode,
    displaySequence = 2,
    payBandAlias = "Medium",
    payBandDescription = "Pay band 2 $prisonCode description",
    nomisPayBand = 2,
  ),
  PrisonPayBand(
    prisonPayBandId = 3,
    prisonCode = prisonCode,
    displaySequence = 3,
    payBandAlias = "High",
    payBandDescription = "Pay band 3 $prisonCode description (highest)",
    nomisPayBand = 2,
  ),
)

internal fun attendance() = schedule().instances().first().attendances.first()

internal fun attendanceList() = listOf(
  AllAttendance(
    attendanceId = 1,
    prisonCode = pentonvillePrisonCode,
    sessionDate = LocalDate.now(),
    timeSlot = "AM",
    status = "WAITING",
    attendanceReasonCode = null,
    issuePayment = null,
    prisonerNumber = "A11111A",
    scheduledInstanceId = 1,
    activityId = 1,
    summary = "Maths Level 1",
    categoryName = "Education",
    recordedTime = null,
  ),
)

internal fun activityCreateRequest(
  prisonCode: String = moorlandPrisonCode,
  educationLevel: ReferenceCode? = null,
  studyArea: ReferenceCode? = null,
  eligibilityRules: Set<EligibilityRule> = setOf(eligibilityRuleOver21),
  paid: Boolean = true,
) =
  ActivityCreateRequest(
    prisonCode = prisonCode,
    attendanceRequired = true,
    inCell = false,
    pieceWork = false,
    outsideWork = false,
    payPerSession = null,
    summary = "Test activity",
    description = "Test activity",
    categoryId = activityCategory().activityCategoryId,
    tierCode = eventTier().code,
    organiserCode = eventOrganiser().code,
    eligibilityRuleIds = eligibilityRules.map { it.eligibilityRuleId },
    pay = emptyList(),
    riskLevel = "high",
    minimumIncentiveNomisCode = "BAS",
    minimumIncentiveLevel = "Basic",
    startDate = TimeSource.tomorrow(),
    endDate = null,
    minimumEducationLevel = listOf(
      ActivityMinimumEducationLevelCreateRequest(
        educationLevelCode = educationLevel?.code ?: "LEVEL_CODE",
        educationLevelDescription = educationLevel?.description ?: "LEVEL DESCRIPTION",
        studyAreaCode = studyArea?.code ?: "STUDY_CODE",
        studyAreaDescription = studyArea?.description ?: "STUDY DESCRIPTION",
      ),
    ),
    locationId = 1,
    capacity = 1,
    scheduleWeeks = 1,
    slots = listOf(Slot(weekNumber = 1, timeSlot = "AM", monday = true)),
    onWing = false,
    offWing = false,
    paid = paid,
  )

internal fun ActivityScheduleSlot.runEveryDayOfWeek() {
  mondayFlag = true
  tuesdayFlag = true
  wednesdayFlag = true
  thursdayFlag = true
  fridayFlag = true
  saturdayFlag = true
  sundayFlag = true
}

fun waitingList(
  waitingListId: Long = 1,
  prisonCode: String = pentonvillePrisonCode,
  prisonerNumber: String = "123456",
  initialStatus: WaitingListStatus = WaitingListStatus.DECLINED,
  applicationDate: LocalDate = TimeSource.today(),
  requestedBy: String = "Fred",
  comments: String? = null,
  allocated: Boolean = false,
): WaitingList {
  val schedule = activityEntity(activityId = waitingListId, prisonCode = prisonCode).schedules().first()
    .apply {
      if (allocated) {
        allocatePrisoner(
          prisonerNumber = prisonerNumber.toPrisonerNumber(),
          bookingId = 10001,
          payBand = lowPayBand,
          allocatedBy = "Mr Blogs",
        )
      }
    }

  val allocation = schedule.allocations().first()

  return WaitingList(
    waitingListId = waitingListId,
    prisonCode = prisonCode,
    activitySchedule = schedule,
    prisonerNumber = prisonerNumber,
    bookingId = 100L,
    applicationDate = applicationDate,
    requestedBy = requestedBy,
    comments = comments,
    createdBy = "Bob",
    initialStatus = initialStatus,
  ).apply {
    this.allocation = allocation
  }
}

internal fun activityFromDbInstance(
  scheduledInstanceId: Long = 1,
  allocationId: Long = 1,
  prisonCode: String = "MDI",
  sessionDate: LocalDate = LocalDate.of(2022, 12, 14),
  startTime: LocalTime? = LocalTime.of(10, 0),
  endTime: LocalTime? = LocalTime.of(11, 30),
  prisonerNumber: String = "G4793VF",
  bookingId: Long = 900001,
  inCell: Boolean = false,
  onWing: Boolean = false,
  offWing: Boolean = false,
  internalLocationId: Int? = 1,
  internalLocationCode: String? = "MDI-EDU_ROOM1",
  internalLocationDescription: String? = "Education room 1",
  scheduleDescription: String? = "HB1 AM",
  activityId: Int = 1,
  activityCategory: String = "Education",
  activitySummary: String? = "English level 1",
  cancelled: Boolean = false,
  suspended: Boolean = false,
) = PrisonerScheduledActivity(
  scheduledInstanceId = scheduledInstanceId,
  allocationId = allocationId,
  prisonCode = prisonCode,
  sessionDate = sessionDate,
  startTime = startTime,
  endTime = endTime,
  prisonerNumber = prisonerNumber,
  bookingId = bookingId,
  inCell = inCell,
  onWing = onWing,
  offWing = offWing,
  internalLocationId = internalLocationId,
  internalLocationCode = internalLocationCode,
  internalLocationDescription = internalLocationDescription,
  scheduleDescription = scheduleDescription,
  activityId = activityId,
  activityCategory = activityCategory,
  activitySummary = activitySummary,
  cancelled = cancelled,
  suspended = suspended,
)
