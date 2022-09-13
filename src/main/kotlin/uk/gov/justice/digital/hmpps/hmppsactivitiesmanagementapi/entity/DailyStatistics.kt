package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import java.time.LocalDate
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "daily_statistics")
data class DailyStatistics(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val dailyStatisticsId: Int = -1,

  val statisticsDate: LocalDate,

  @ManyToOne
  @JoinColumn(name = "rollout_prison_id", nullable = false)
  val rolloutPrison: RolloutPrison,

  var unemployed: Int? = null,

  var longTermSick: Int? = null,

  var shortTermSick: Int? = null,

  var activitiesWithAllocations: Int? = null,

  var sessionsCancelled: Int? = null,

  var sessionsRunToday: Int? = null,

  var attendanceExpected: Int? = null,

  var attendanceReceived: Int? = null,

  var peopleInWork: Int? = null,

  var peopleInEducation: Int? = null,

  var vacancies: Int? = null
)
