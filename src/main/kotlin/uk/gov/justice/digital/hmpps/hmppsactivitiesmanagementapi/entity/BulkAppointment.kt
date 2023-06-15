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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.UserDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.BulkAppointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.BulkAppointmentSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toSummary
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.BulkAppointment as BulkAppointmentModel

@Entity
@Table(name = "bulk_appointment")
data class BulkAppointment(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val bulkAppointmentId: Long = 0,

  val created: LocalDateTime = LocalDateTime.now(),

  val createdBy: String,
) {
  @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @JoinTable(
    name = "bulk_appointment_appointment",
    joinColumns = [JoinColumn(name = "bulkAppointmentId")],
    inverseJoinColumns = [JoinColumn(name = "appointmentId")],
  )
  private val appointments: MutableList<Appointment> = mutableListOf()

  fun appointments() = appointments.toList()

  fun addAppointment(appointment: Appointment) = appointments.add(appointment)

  fun prisonCode() = appointments().map { appointment -> appointment.prisonCode }.distinct().first()

  fun prisonerNumbers() = appointments().map { appointment -> appointment.prisonerNumbers() }.flatten().distinct()

  fun usernames() = listOf(createdBy).union(appointments().map { appointment -> appointment.usernames() }.flatten()).distinct()

  fun occurrences() = appointments().map { appointment -> appointment.occurrences() }.flatten().sortedWith(compareBy<AppointmentOccurrence> { it.startDate }.thenBy { it.startTime })

  fun toModel() = BulkAppointmentModel(
    id = this.bulkAppointmentId,
    appointments = this.appointments().toModel(),
    created = created,
    createdBy = createdBy,
  )

  fun toSummary() = BulkAppointmentSummary(
    id = this.bulkAppointmentId,
    appointmentCount = this.appointments().size,
  )

  fun toDetails(
    prisonerMap: Map<String, Prisoner>,
    referenceCodeMap: Map<String, ReferenceCode>,
    locationMap: Map<Long, Location>,
    userMap: Map<String, UserDetail>,
  ): BulkAppointmentDetails {
    val appointment = appointments().first().toDetails(emptyList(), referenceCodeMap, locationMap, userMap)
    return BulkAppointmentDetails(
      bulkAppointmentId,
      // TODO: add these as new properties and columns in the database
      appointment.category,
      appointment.appointmentDescription,
      appointment.internalLocation,
      appointment.inCell,
      appointment.startDate,
      appointments().map { it.occurrenceDetails(prisonerMap, referenceCodeMap, locationMap, userMap) }.flatten(),
      created,
      userMap[createdBy].toSummary(createdBy),
    )
  }
}
