package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.*
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.UserDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.BulkAppointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.BulkAppointmentSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentName
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toSummary
import java.time.LocalDate
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.BulkAppointment as BulkAppointmentModel

@Entity
@Table(name = "appointment_set")
data class BulkAppointment(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "appointment_set_id")
  val bulkAppointmentId: Long = 0,

  val prisonCode: String,

  var categoryCode: String,

  @Column(name = "custom_name")
  var appointmentDescription: String?,

  var internalLocationId: Long?,

  var inCell: Boolean,

  var startDate: LocalDate,

  @Column(name = "created_time")
  val created: LocalDateTime = LocalDateTime.now(),

  val createdBy: String,
) {
  @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @JoinTable(
    name = "appointment_set_appointment_series",
    joinColumns = [JoinColumn(name = "bulkAppointmentId")],
    inverseJoinColumns = [JoinColumn(name = "appointmentId")],
  )
  private val appointments: MutableList<Appointment> = mutableListOf()

  fun appointments() = appointments.toList()

  fun addAppointment(appointment: Appointment) = appointments.add(appointment)

  fun prisonerNumbers() = appointments().map { appointment -> appointment.prisonerNumbers() }.flatten().distinct()

  fun usernames() = listOf(createdBy).union(appointments().map { appointment -> appointment.usernames() }.flatten()).distinct()

  fun occurrences() = appointments().map { appointment -> appointment.occurrences() }.flatten().sortedWith(compareBy<AppointmentOccurrence> { it.startDate }.thenBy { it.startTime })

  fun toModel() = BulkAppointmentModel(
    id = this.bulkAppointmentId,
    prisonCode = prisonCode,
    categoryCode = categoryCode,
    appointmentDescription = appointmentDescription,
    internalLocationId = internalLocationId,
    inCell = inCell,
    startDate = startDate,
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
    return BulkAppointmentDetails(
      bulkAppointmentId,
      prisonCode,
      referenceCodeMap[categoryCode].toAppointmentName(categoryCode, appointmentDescription),
      referenceCodeMap[categoryCode].toAppointmentCategorySummary(categoryCode),
      appointmentDescription,
      if (inCell) {
        null
      } else {
        locationMap[internalLocationId].toAppointmentLocationSummary(internalLocationId!!, prisonCode)
      },
      inCell,
      startDate,
      appointments().map { it.occurrenceDetails(prisonerMap, referenceCodeMap, locationMap, userMap) }.flatten(),
      created,
      userMap[createdBy].toSummary(createdBy),
    )
  }
}
