package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentInstance as AppointmentInstanceModel

@Entity
@Table(name = "appointment_instance")
data class AppointmentInstance(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val appointmentInstanceId: Long = 0,

  @ManyToOne
  @JoinColumn(name = "appointment_occurrence_id", nullable = false)
  val appointmentOccurrence: AppointmentOccurrence,

  @OneToOne
  @JoinColumn(name = "appointment_category_id", nullable = false)
  var category: AppointmentCategory,

  val prisonCode: String,

  var internalLocationId: Long?,

  var inCell: Boolean,

  val prisonerNumber: String,

  val bookingId: Long,

  var appointmentDate: LocalDate,

  var startTime: LocalTime,

  var endTime: LocalTime?,

  var comment: String? = null,

  var attended: Boolean? = null,

  var cancelled: Boolean = false,
) {
  fun toModel() = AppointmentInstanceModel(
    id = appointmentInstanceId,
    category = category.toModel(),
    prisonCode = prisonCode,
    internalLocationId = internalLocationId,
    inCell = inCell,
    prisonerNumber = prisonerNumber,
    bookingId = bookingId,
    appointmentDate = appointmentDate,
    startTime = startTime,
    endTime = endTime,
    comment = comment,
    attended = attended,
    cancelled = cancelled,
  )
}

fun List<AppointmentInstance>.toModel() = map { it.toModel() }
