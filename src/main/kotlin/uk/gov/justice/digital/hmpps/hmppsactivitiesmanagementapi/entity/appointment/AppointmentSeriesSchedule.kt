package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentFrequency as AppointmentFrequencyModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeriesSchedule as AppointmentSeriesScheduleModel

@Entity
@Table(name = "appointment_series_schedule")
data class AppointmentSeriesSchedule(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val appointmentSeriesScheduleId: Long = 0,

  @OneToOne(mappedBy = "schedule", fetch = FetchType.EAGER)
  val appointmentSeries: AppointmentSeries,

  @Enumerated(EnumType.STRING)
  var frequency: AppointmentFrequency,

  var numberOfAppointments: Int,
) {
  fun toModel() = AppointmentSeriesScheduleModel(
    AppointmentFrequencyModel.valueOf(frequency.toString()),
    numberOfAppointments,
  )

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as AppointmentSeriesSchedule

    if (appointmentSeriesScheduleId != other.appointmentSeriesScheduleId) return false
    if (frequency != other.frequency) return false
    if (numberOfAppointments != other.numberOfAppointments) return false

    return true
  }

  override fun hashCode(): Int {
    var result = appointmentSeriesScheduleId.hashCode()
    result = 31 * result + frequency.hashCode()
    result = 31 * result + numberOfAppointments
    return result
  }

  override fun toString(): String {
    return "AppointmentSeriesSchedule(appointmentSeriesScheduleId=$appointmentSeriesScheduleId, frequency=$frequency, numberOfAppointments=$numberOfAppointments)"
  }
}

class AppointmentSeriesScheduleIterator(
  var startDate: LocalDate,
  private val frequency: AppointmentFrequency,
  private val numberOfAppointments: Int,
) : Iterator<LocalDate> {
  private var count = 0

  override fun hasNext() = count < numberOfAppointments

  override fun next(): LocalDate {
    count++
    return frequency.appointmentDate(startDate, count)
  }
}
