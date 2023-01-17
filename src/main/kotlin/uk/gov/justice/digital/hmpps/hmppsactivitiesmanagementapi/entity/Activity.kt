package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.hibernate.Hibernate
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.between
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityLite
import java.time.LocalDate
import java.time.LocalDateTime
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PayPerSession as ModelPayPerSession

@Entity
@Table(name = "activity")
data class Activity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val activityId: Long? = null,

  val prisonCode: String,

  @OneToOne
  @JoinColumn(name = "activity_category_id", nullable = false)
  val activityCategory: ActivityCategory,

  @OneToOne
  @JoinColumn(name = "activity_tier_id")
  val activityTier: ActivityTier?,

  @OneToMany(mappedBy = "activity", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  val eligibilityRules: MutableList<ActivityEligibility> = mutableListOf(),

  @OneToMany(mappedBy = "activity", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  val schedules: MutableList<ActivitySchedule> = mutableListOf(),

  @OneToMany(mappedBy = "activity", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  val waitingList: MutableList<PrisonerWaiting> = mutableListOf(),

  @OneToMany(mappedBy = "activity", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  var activityPay: MutableList<ActivityPay> = mutableListOf(),

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

  var minimumIncentiveLevel: String? = null,

  val createdTime: LocalDateTime,

  val createdBy: String
) {
  fun isActive(date: LocalDate): Boolean =
    if (endDate != null) date.between(startDate, endDate) else (date.isEqual(startDate) || date.isAfter(startDate))

  fun getSchedulesOnDay(day: LocalDate, includeSuspended: Boolean = true): List<ActivitySchedule> {
    val byDayOfWeek = this.schedules.filter { schedule ->
      schedule.slots.any { day.dayOfWeek in it.getDaysOfWeek() }
    }
    return if (includeSuspended) {
      byDayOfWeek
    } else {
      byDayOfWeek.filter {
        it.suspensions.none { suspension ->
          !day.isBefore(suspension.suspendedFrom) &&
            (suspension.suspendedUntil == null || !day.isAfter(suspension.suspendedUntil))
        }
      }
    }
  }

  fun toModelLite() = ActivityLite(
    id = activityId!!,
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
    minimumIncentiveLevel = minimumIncentiveLevel,
  )

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Activity

    return activityId != null && activityId == other.activityId
  }

  override fun hashCode(): Int = activityId.hashCode()

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(activityId = $activityId )"
  }

  fun addSchedule(
    description: String,
    internalLocationId: Int,
    internalLocationCode: String,
    internalLocationDescription: String,
    capacity: Int,
    startDate: LocalDate,
    endDate: LocalDate? = null
  ): ActivitySchedule {
    // TODO need to validate e.g. dates, duplicate schedule, schedule description
    schedules.add(
      ActivitySchedule(
        activity = this,
        description = description,
        internalLocationId = internalLocationId,
        internalLocationCode = internalLocationCode,
        internalLocationDescription = internalLocationDescription,
        capacity = capacity,
        startDate = startDate
      ).apply {
        this.endDate = endDate
      }
    )

    return schedules.last()
  }
}

fun List<Activity>.toModelLite() = map { it.toModelLite() }

enum class PayPerSession { H, F }
