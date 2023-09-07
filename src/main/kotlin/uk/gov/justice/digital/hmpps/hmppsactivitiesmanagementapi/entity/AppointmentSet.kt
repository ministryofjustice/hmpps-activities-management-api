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
data class AppointmentSet(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val appointmentSetId: Long = 0,

  val prisonCode: String,

  var categoryCode: String,

  var customName: String? = null,

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "appointment_tier_id")
  var appointmentTier: AppointmentTier,

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "appointment_host_id")
  var appointmentHost: AppointmentHost? = null,

  var internalLocationId: Long?,

  val customLocation: String? = null,

  val inCell: Boolean = false,

  val onWing: Boolean = false,

  val offWing: Boolean = true,

  var startDate: LocalDate,

  val createdTime: LocalDateTime = LocalDateTime.now(),

  val createdBy: String,

  var updatedTime: LocalDateTime? = null,

  var updatedBy: String? = null,
) {
  @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @JoinTable(
    name = "appointment_set_appointment_series",
    joinColumns = [JoinColumn(name = "appointment_set_id")],
    inverseJoinColumns = [JoinColumn(name = "appointment_series_id")],
  )
  private val appointmentSeries: MutableList<AppointmentSeries> = mutableListOf()

  fun appointmentSeries() = appointmentSeries.toList()

  fun addAppointmentSeries(appointmentSeries: AppointmentSeries) = this.appointmentSeries.add(appointmentSeries)

  fun prisonerNumbers() = appointmentSeries().map { appointment -> appointment.prisonerNumbers() }.flatten().distinct()

  fun usernames() = listOf(createdBy).union(appointmentSeries().map { appointment -> appointment.usernames() }.flatten()).distinct()

  fun appointments() = appointmentSeries().map { series -> series.appointments() }.flatten().sortedWith(compareBy<Appointment> { it.startDate }.thenBy { it.startTime })

  fun toModel() = BulkAppointmentModel(
    id = this.appointmentSetId,
    prisonCode = prisonCode,
    categoryCode = categoryCode,
    appointmentDescription = customName,
    internalLocationId = internalLocationId,
    inCell = inCell,
    startDate = startDate,
    appointments = this.appointmentSeries().toModel(),
    created = createdTime,
    createdBy = createdBy,
  )

  fun toSummary() = BulkAppointmentSummary(
    id = this.appointmentSetId,
    appointmentCount = this.appointmentSeries().size,
  )

  fun toDetails(
    prisonerMap: Map<String, Prisoner>,
    referenceCodeMap: Map<String, ReferenceCode>,
    locationMap: Map<Long, Location>,
    userMap: Map<String, UserDetail>,
  ): BulkAppointmentDetails {
    return BulkAppointmentDetails(
      appointmentSetId,
      prisonCode,
      referenceCodeMap[categoryCode].toAppointmentName(categoryCode, customName),
      referenceCodeMap[categoryCode].toAppointmentCategorySummary(categoryCode),
      customName,
      if (inCell) {
        null
      } else {
        locationMap[internalLocationId].toAppointmentLocationSummary(internalLocationId!!, prisonCode)
      },
      inCell,
      startDate,
      appointmentSeries().map { it.appointmentDetails(prisonerMap, referenceCodeMap, locationMap, userMap) }.flatten(),
      createdTime,
      userMap[createdBy].toSummary(createdBy),
    )
  }
}
