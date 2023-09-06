package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentTier as ModelAppointmentTier

@Entity
@Table(name = "appointment_tier")
data class AppointmentTier(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val appointmentTierId: Long = 0,

  val description: String,
) {
  fun toModel() = ModelAppointmentTier(
    appointmentTierId = appointmentTierId,
    description = description,
  )
}
