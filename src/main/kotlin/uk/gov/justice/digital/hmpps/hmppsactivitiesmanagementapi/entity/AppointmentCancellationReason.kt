package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentCancellationReason as ModelAppointmentCancellationReason

@Entity
@Table(name = "appointment_cancellation_reason")
data class AppointmentCancellationReason(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val appointmentCancellationReasonId: Long = -1,

  val description: String,

  val isDelete: Boolean,
) {

  fun toModel() = ModelAppointmentCancellationReason(
    appointmentCancellationReasonId = appointmentCancellationReasonId,
    description = description,
    isDelete = isDelete,
  )
}
