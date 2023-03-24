package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentInstance as AppointmentInstanceModel

@Entity
@Table(name = "appointment_instance")
data class AppointmentInstance(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val appointmentInstanceId: Long = 0,

  @ManyToOne
  @JoinColumn(name = "appointment_id", nullable = false)
  val appointment: Appointment,

  @ManyToOne
  @JoinColumn(name = "appointment_occurrence_id", nullable = false)
  val appointmentOccurrence: AppointmentOccurrence,

  @OneToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
  @JoinColumn(name = "appointment_occurrence_allocation_id", nullable = false)
  var appointmentOccurrenceAllocation: AppointmentOccurrenceAllocation,

  var categoryCode: String,

  val prisonCode: String,

  var internalLocationId: Long?,

  var inCell: Boolean,

  val prisonerNumber: String,

  val bookingId: Long,

  var appointmentDate: LocalDate,

  var startTime: LocalTime,

  var endTime: LocalTime?,

  var comment: String? = null,

  var cancelled: Boolean = false,

  val created: LocalDateTime = LocalDateTime.now(),

  val createdBy: String,

  var updated: LocalDateTime? = null,

  var updatedBy: String? = null,
) {
  fun toModel() = AppointmentInstanceModel(
    id = appointmentInstanceId,
    categoryCode = categoryCode,
    prisonCode = prisonCode,
    internalLocationId = internalLocationId,
    inCell = inCell,
    prisonerNumber = prisonerNumber,
    bookingId = bookingId,
    appointmentDate = appointmentDate,
    startTime = startTime,
    endTime = endTime,
    comment = comment,
    cancelled = cancelled,
  )
}

fun List<AppointmentInstance>.toModel() = map { it.toModel() }
