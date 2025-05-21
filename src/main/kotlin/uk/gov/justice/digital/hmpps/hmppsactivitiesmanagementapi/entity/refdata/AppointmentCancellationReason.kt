package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.envers.Audited
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentCancellationReason as AppointmentCancellationReasonModel

@Entity
@Audited
@Table(name = "appointment_cancellation_reason")
data class AppointmentCancellationReason(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val appointmentCancellationReasonId: Long = 0,

  val description: String,

  val isDelete: Boolean,
) {
  fun toModel() = AppointmentCancellationReasonModel(
    appointmentCancellationReasonId = appointmentCancellationReasonId,
    description = description,
    isDelete = isDelete,
  )
}
