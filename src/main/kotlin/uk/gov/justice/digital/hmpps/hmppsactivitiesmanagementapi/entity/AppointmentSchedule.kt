package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "appointment_schedule")
data class AppointmentSchedule(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val appointmentScheduleId: Long = -1,

  @OneToOne(mappedBy = "schedule")
  val appointment: Appointment,

  @Enumerated(EnumType.STRING)
  var repeatPeriod: AppointmentRepeatPeriod,

  var repeatCount: Int,
) {
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
}

class AppointmentScheduleIterator(
  val startDate: LocalDate,
  private val repeatPeriod: AppointmentRepeatPeriod,
  private val repeatCount: Int,
) : Iterator<LocalDate> {
  private var date = startDate
  private var count = 0

  override fun hasNext() = count < repeatCount

  override fun next(): LocalDate {
    val next = date
    date = repeatPeriod.nextDate(date)
    count++
    return next
  }
}
