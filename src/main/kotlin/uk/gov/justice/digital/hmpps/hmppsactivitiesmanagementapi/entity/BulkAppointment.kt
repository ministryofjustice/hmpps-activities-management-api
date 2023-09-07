package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
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

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "appointment_tier_id")
  var appointmentTier: AppointmentTier,

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "appointment_host_id")
  var appointmentHost: AppointmentHost? = null,

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
    joinColumns = [JoinColumn(name = "appointment_set_id")],
    inverseJoinColumns = [JoinColumn(name = "appointment_series_id")],
  )
  private val appointments: MutableList<AppointmentSeries> = mutableListOf()

  fun appointments() = appointments.toList()

  fun addAppointment(appointmentSeries: AppointmentSeries) = appointments.add(appointmentSeries)

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
