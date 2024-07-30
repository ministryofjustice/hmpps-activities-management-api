package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.between
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.onOrBefore
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.ActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EligibilityRule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventOrganiser
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.PrisonPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityLite
import java.time.LocalDate
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PayPerSession as ModelPayPerSession

@Entity
@Table(name = "activity")
@EntityListeners(AuditableEntityListener::class)
data class Activity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val activityId: Long = 0,

  val prisonCode: String,

  @OneToOne
  @JoinColumn(name = "activity_category_id", nullable = false)
  var activityCategory: ActivityCategory,

  @OneToOne
  @JoinColumn(name = "activity_tier_id")
  var activityTier: EventTier,

  var attendanceRequired: Boolean = true,

  var inCell: Boolean = false,

  var onWing: Boolean = false,

  var offWing: Boolean = false,

  var pieceWork: Boolean = false,

  var outsideWork: Boolean = false,

  @Enumerated(EnumType.STRING)
  var payPerSession: PayPerSession = PayPerSession.H,

  var summary: String,

  var description: String?,

  var startDate: LocalDate,

  var riskLevel: String,

  val createdTime: LocalDateTime,

  val createdBy: String,

  var updatedTime: LocalDateTime? = null,

  var updatedBy: String? = null,

  @Transient
  private val isPaid: Boolean,
) {
  var paid: Boolean = isPaid
    set(value) {
      if (field == value) return // no op, ignore

      if (schedules().any { it.allocations(true).isNotEmpty() }) {
        throw IllegalArgumentException("Paid attribute cannot be updated for allocated activity '$activityId'")
      }

      field = value.also { if (!it) removePay() }
    }

  var endDate: LocalDate? = null
    set(value) {
      require(value == null || value >= startDate) { "Activity end date cannot be before activity start date." }

      field = value
    }

  @OneToOne
  @JoinColumn(name = "activity_organiser_id")
  var organiser: EventOrganiser? = null
    set(value) {
      require(value == null || activityTier.isTierTwo() == true) { "Cannot add activity organiser unless activity is Tier 2." }

      field = value
    }

  @OneToMany(mappedBy = "activity", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  private val activityMinimumEducationLevel: MutableList<ActivityMinimumEducationLevel> = mutableListOf()

  @OneToMany(mappedBy = "activity", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  private val activityPay: MutableList<ActivityPay> = mutableListOf()

  @OneToMany(mappedBy = "activity", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  private val eligibilityRules: MutableList<ActivityEligibility> = mutableListOf()

  @OneToMany(mappedBy = "activity", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  private val schedules: MutableList<ActivitySchedule> = mutableListOf()

  fun schedules() = schedules.toList()

  fun eligibilityRules() = eligibilityRules.toList()

  fun activityPay() = activityPay.toList()

  fun activityPayFor(payBand: PrisonPayBand, incentiveLevelCode: String): ActivityPay? =
    activityPay()
      .filter {
        it.payBand == payBand && it.incentiveNomisCode == incentiveLevelCode &&
          (it.startDate.onOrBefore(LocalDate.now()) || it.startDate == null)
      }
      .sortedBy { it.startDate }
      .lastOrNull()

  fun activityMinimumEducationLevel() = activityMinimumEducationLevel.toList()

  fun isActive(date: LocalDate): Boolean =
    if (endDate != null) date.between(startDate, endDate) else (date.isEqual(startDate) || date.isAfter(startDate))

  fun state(vararg status: ActivityState) = status.any { it == getActivityState() }

  fun isUnemployment() = activityCategory.isNotInWork()

  fun getSchedulesOnDay(day: LocalDate, includeSuspended: Boolean = true): List<ActivitySchedule> {
    val byDayOfWeek = this.schedules
      .filter { it.isActiveOn(day) }
      .filter { schedule -> schedule.slots().any { day.dayOfWeek in it.getDaysOfWeek() } }
    return if (includeSuspended) {
      byDayOfWeek
    } else {
      byDayOfWeek.filter { it.isSuspendedOn(day).not() }
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Activity

    return activityId == other.activityId
  }

  override fun hashCode(): Int = activityId.hashCode()

  fun addEligibilityRule(rule: EligibilityRule) {
    failIfRuleAlreadyPresent(rule)

    eligibilityRules.add(ActivityEligibility(eligibilityRule = rule, activity = this))
  }

  private fun failIfRuleAlreadyPresent(rule: EligibilityRule) {
    if (eligibilityRules.any { it.eligibilityRule.eligibilityRuleId == rule.eligibilityRuleId }) {
      throw IllegalArgumentException("Eligibility rule '${rule.code}' already present on activity")
    }
  }

  fun addPay(
    incentiveNomisCode: String,
    incentiveLevel: String,
    payBand: PrisonPayBand,
    rate: Int?,
    pieceRate: Int?,
    pieceRateItems: Int?,
    startDate: LocalDate?,
  ) {
    require(paid) {
      "Unpaid activity '$summary' cannot have pay rates added to it"
    }

    require(activityPay().none { it.payBand == payBand && it.incentiveNomisCode == incentiveNomisCode && it.startDate == startDate }) {
      "The pay band, incentive level and start date combination must be unique for each pay rate"
    }

    activityPay.add(
      ActivityPay(
        activity = this,
        incentiveNomisCode = incentiveNomisCode,
        incentiveLevel = incentiveLevel,
        payBand = payBand,
        rate = rate,
        pieceRate = pieceRate,
        pieceRateItems = pieceRateItems,
        startDate = startDate,
      ),
    )
  }

  fun removePay() {
    activityPay.clear()
  }

  fun addMinimumEducationLevel(
    educationLevelCode: String,
    educationLevelDescription: String,
    studyAreaCode: String,
    studyAreaDescription: String,
  ) {
    activityMinimumEducationLevel.add(
      ActivityMinimumEducationLevel(
        activity = this,
        educationLevelCode = educationLevelCode,
        educationLevelDescription = educationLevelDescription,
        studyAreaCode = studyAreaCode,
        studyAreaDescription = studyAreaDescription,
      ),
    )
  }

  fun removeMinimumEducationLevel(minimumEducationLevel: ActivityMinimumEducationLevel) {
    activityMinimumEducationLevel.remove(minimumEducationLevel)
  }

  fun addSchedule(
    description: String,
    internalLocation: Location? = null,
    capacity: Int,
    startDate: LocalDate,
    endDate: LocalDate? = null,
    runsOnBankHoliday: Boolean,
    scheduleWeeks: Int,
  ) =
    addSchedule(
      ActivitySchedule.valueOf(
        activity = this,
        description = description,
        internalLocationId = internalLocation?.locationId?.toInt(),
        internalLocationCode = internalLocation?.internalLocationCode,
        internalLocationDescription = internalLocation?.description,
        capacity = capacity,
        startDate = startDate,
        endDate = endDate,
        runsOnBankHoliday = runsOnBankHoliday,
        scheduleWeeks = scheduleWeeks,
      ),
    )

  fun addSchedule(schedule: ActivitySchedule): ActivitySchedule {
    failIfScheduleDatesClashWithActivityDates(schedule.startDate, schedule.endDate)
    failIfScheduleWithDescriptionAlreadyPresentOnActivity(schedule.description)
    failIfScheduleBelongsToDifferentActivity(schedule)

    schedules.add(schedule)

    return schedules.last()
  }

  private fun failIfScheduleDatesClashWithActivityDates(startDate: LocalDate, endDate: LocalDate?) {
    if (startDate.isBefore(this.startDate)) {
      throw IllegalArgumentException("The schedule start date '$startDate' cannot be before the activity start date ${this.startDate}")
    }

    if (endDate != null && this.endDate != null && endDate.isAfter(this.endDate)) {
      throw IllegalArgumentException("The schedule end date '$endDate' cannot be after the activity end date ${this.endDate}")
    }
  }

  private fun failIfScheduleWithDescriptionAlreadyPresentOnActivity(description: String) {
    if (schedules.any { it.description.trim().uppercase() == description.trim().uppercase() }) {
      throw IllegalArgumentException("A schedule with the description '$description' already exists.")
    }
  }

  private fun failIfScheduleBelongsToDifferentActivity(schedule: ActivitySchedule) {
    if (schedule.activity.activityId != activityId) {
      throw IllegalArgumentException("Can only add schedules that belong to this activity.")
    }
  }

  fun ends(date: LocalDate) = date == endDate

  fun toModelLite() = ActivityLite(
    id = activityId,
    prisonCode = prisonCode,
    attendanceRequired = attendanceRequired,
    inCell = inCell,
    onWing = onWing,
    offWing = offWing,
    pieceWork = pieceWork,
    outsideWork = outsideWork,
    payPerSession = ModelPayPerSession.valueOf(payPerSession.name),
    summary = summary,
    description = description,
    category = activityCategory.toModel(),
    riskLevel = riskLevel,
    minimumEducationLevel = activityMinimumEducationLevel().toModel(),
    capacity = schedules().sumOf { schedule -> schedule.capacity },
    allocated = schedules().sumOf { schedule ->
      schedule.allocations().filterNot { it.status(PrisonerStatus.ENDED) }.size
    },
    endDate = endDate,
    createdTime = createdTime,
    activityState = getActivityState(),
    paid = paid,
  )

  fun isPaid() = paid

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(activityId = $activityId )"
  }

  private fun getActivityState(): ActivityState {
    return if (endDate != null && endDate!! < LocalDate.now()) {
      ActivityState.ARCHIVED
    } else {
      ActivityState.LIVE
    }
  }
}

fun List<Activity>.toModelLite() = map { it.toModelLite() }

enum class PayPerSession(val label: String) { H("Half day"), F("Full day") }

enum class ActivityState {
  ARCHIVED,
  LIVE,
}
