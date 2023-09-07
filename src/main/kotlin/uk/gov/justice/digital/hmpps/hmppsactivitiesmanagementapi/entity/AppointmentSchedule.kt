package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentRepeat
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentRepeatPeriod as AppointmentRepeatPeriodModel

@Entity
@Table(name = "appointment_series_schedule")
data class AppointmentSchedule(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "appointment_series_schedule_id")
  val appointmentScheduleId: Long = 0,

  @OneToOne(mappedBy = "schedule", fetch = FetchType.EAGER)
  val appointmentSeries: AppointmentSeries,

  @Enumerated(EnumType.STRING)
  @Column(name = "frequency")
  var repeatPeriod: AppointmentRepeatPeriod,

  @Column(name = "number_of_appointments")
  var repeatCount: Int,
) {
  fun toRepeat() = AppointmentRepeat(
    AppointmentRepeatPeriodModel.valueOf(repeatPeriod.toString()),
    repeatCount,
  )

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as AppointmentSchedule

    if (appointmentScheduleId != other.appointmentScheduleId) return false
    if (repeatPeriod != other.repeatPeriod) return false
    if (repeatCount != other.repeatCount) return false

    return true
  }

  override fun hashCode(): Int {
    var result = appointmentScheduleId.hashCode()
    result = 31 * result + repeatPeriod.hashCode()
    result = 31 * result + repeatCount
    return result
  }

  override fun toString(): String {
    return "AppointmentSchedule(appointmentScheduleId=$appointmentScheduleId, repeatPeriod=$repeatPeriod, repeatCount=$repeatCount)"
  }
}

class AppointmentScheduleIterator(
  var startDate: LocalDate,
  private val repeatPeriod: AppointmentRepeatPeriod,
  private val repeatCount: Int,
) : Iterator<LocalDate> {
  private var count = 0

  override fun hasNext() = count < repeatCount

  override fun next(): LocalDate {
    count++
    return repeatPeriod.occurrenceDate(startDate, count)
  }
}
