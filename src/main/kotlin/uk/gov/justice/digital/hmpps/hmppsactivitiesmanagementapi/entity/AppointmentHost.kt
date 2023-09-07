package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentHost as ModelAppointmentHost

@Entity
@Table(name = "appointment_host")
data class AppointmentHost(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val appointmentHostId: Long = 0,

  val description: String,

) {
  fun toModel() = ModelAppointmentHost(
    id = appointmentHostId,
    description = description,
  )
}
