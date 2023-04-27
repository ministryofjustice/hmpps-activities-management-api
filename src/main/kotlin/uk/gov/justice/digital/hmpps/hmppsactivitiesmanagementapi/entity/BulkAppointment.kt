package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.BulkAppointment as BulkAppointmentModel

@Entity
@Table(name = "bulk_appointment")
data class BulkAppointment(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val bulkAppointmentId: Long = 0,

  @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
  @JoinTable(
    name = "bulk_appointment_appointment",
    joinColumns = [JoinColumn(name = "bulkAppointmentId")],
    inverseJoinColumns = [JoinColumn(name = "appointmentId")],
  )
  val appointments: List<Appointment>,

  val created: LocalDateTime = LocalDateTime.now(),

  val createdBy: String,

) {

  fun toModel() = BulkAppointmentModel(
    bulkAppointmentId = this.bulkAppointmentId,
    appointments = this.appointments.toModel(),
    created = created,
    createdBy = createdBy,
  )
}
