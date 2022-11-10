package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.hibernate.Hibernate
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import java.time.LocalDate
import java.time.LocalDateTime
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.Table

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
  @JoinColumn(name = "activity_tier_id", nullable = false)
  val activityTier: ActivityTier,

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

  val summary: String,

  val description: String,

  val startDate: LocalDate,

  var endDate: LocalDate? = null,

  @Deprecated(message = "To be removed. Use start and end dates in its place")
  var active: Boolean = true,

  val createdTime: LocalDateTime,

  val createdBy: String
) {
  fun isActiveOn(date: LocalDate) = active && startDate <= date && (endDate == null || date.isBefore(endDate))

  fun getSchedulesOnDay(day: LocalDate, includeSuspended: Boolean = true): List<ActivitySchedule> {
    val byDayOfWeek = this.schedules.filter { day.dayOfWeek in it.getDaysOfWeek() }
    return if (includeSuspended) {
      byDayOfWeek
    } else {
      byDayOfWeek.filter {
        it.suspensions.none { suspension ->
          !day.isBefore(suspension.suspendedFrom) &&
            (suspension.suspendedUntil === null || !day.isAfter(suspension.suspendedUntil))
        }
      }
    }
  }

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
}
