package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "daily_statistics")
data class DailyStatistics(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val dailyStatisticsId: Long = 0,

  val statisticsDate: LocalDate,

  val prisonCode: String,

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

  var vacancies: Int? = null,
)
