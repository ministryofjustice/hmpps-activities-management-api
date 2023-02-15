package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.between
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityLite
import java.time.LocalDate
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PayPerSession as ModelPayPerSession

@Entity
@Table(name = "activity")
data class Activity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val activityId: Long = -1,

  val prisonCode: String,

  @OneToOne
  @JoinColumn(name = "activity_category_id", nullable = false)
  val activityCategory: ActivityCategory,

  @OneToOne
  @JoinColumn(name = "activity_tier_id")
  val activityTier: ActivityTier?,

  @OneToMany(mappedBy = "activity", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  val waitingList: MutableList<PrisonerWaiting> = mutableListOf(),

  var attendanceRequired: Boolean = true,

  var inCell: Boolean = false,

  var pieceWork: Boolean = false,

  var outsideWork: Boolean = false,

  @Enumerated(EnumType.STRING)
  var payPerSession: PayPerSession = PayPerSession.H,

  val summary: String,

  val description: String?,

  val startDate: LocalDate,

  var endDate: LocalDate? = null,

  var riskLevel: String? = null,

  var minimumIncentiveNomisCode: String,

  var minimumIncentiveLevel: String,

  val createdTime: LocalDateTime,

  val createdBy: String
) {

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

  fun isActive(date: LocalDate): Boolean =
    if (endDate != null) date.between(startDate, endDate) else (date.isEqual(startDate) || date.isAfter(startDate))

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

  fun toModelLite() = ActivityLite(
    id = activityId,
    prisonCode = prisonCode,
    attendanceRequired = attendanceRequired,
    inCell = inCell,
    pieceWork = pieceWork,
    outsideWork = outsideWork,
    payPerSession = ModelPayPerSession.valueOf(payPerSession.name),
    summary = summary,
    description = description,
    category = activityCategory.toModel(),
    riskLevel = riskLevel,
    minimumIncentiveNomisCode = minimumIncentiveNomisCode,
    minimumIncentiveLevel = minimumIncentiveLevel,
  )

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Activity

    return activityId == other.activityId
  }

  override fun hashCode(): Int = activityId.hashCode()

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(activityId = $activityId )"
  }

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
    pieceRateItems: Int?
  ) {
    activityPay.add(
      ActivityPay(
        activity = this,
        incentiveNomisCode = incentiveNomisCode,
        incentiveLevel = incentiveLevel,
        payBand = payBand,
        rate = rate,
        pieceRate = pieceRate,
        pieceRateItems = pieceRateItems
      )
    )
  }

  fun addSchedule(
    description: String,
    internalLocationId: Int,
    internalLocationCode: String,
    internalLocationDescription: String,
    capacity: Int,
    startDate: LocalDate,
    endDate: LocalDate? = null
  ) =
    addSchedule(
      ActivitySchedule.valueOf(
        activity = this,
        description = description,
        internalLocationId = internalLocationId,
        internalLocationCode = internalLocationCode,
        internalLocationDescription = internalLocationDescription,
        capacity = capacity,
        startDate = startDate,
        endDate = endDate
      )
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

    if (this.endDate != null && startDate.isBefore(this.endDate).not()) {
      throw IllegalArgumentException("The schedule start date '$startDate' must be before the activity end date ${this.endDate}")
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
}

fun List<Activity>.toModelLite() = map { it.toModelLite() }

enum class PayPerSession { H, F }
