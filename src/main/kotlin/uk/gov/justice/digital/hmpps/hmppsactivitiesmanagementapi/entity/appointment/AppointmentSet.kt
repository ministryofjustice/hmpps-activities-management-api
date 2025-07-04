package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment

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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventOrganiser
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSetDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSetSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.LocationService.LocationDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.toAppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentName
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelEventOrganiser
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelEventTier
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSet as AppointmentSetModel

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
  val appointmentTier: EventTier?,

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

  var dpsLocationId: UUID? = null,
) {
  @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @JoinTable(
    name = "appointment_set_appointment_series",
    joinColumns = [JoinColumn(name = "appointment_set_id")],
    inverseJoinColumns = [JoinColumn(name = "appointment_series_id")],
  )
  private val appointmentSeries: MutableList<AppointmentSeries> = mutableListOf()

  @OneToOne
  @JoinColumn(name = "appointment_organiser_id")
  var appointmentOrganiser: EventOrganiser? = null
    set(value) {
      require(value == null || appointmentTier?.isTierTwo() == true) { "Cannot add organiser unless appointment set is Tier 2." }

      field = value
    }

  fun appointmentSeries() = appointmentSeries.toList()

  fun appointments(includeDeleted: Boolean = false) = appointmentSeries().flatMap { series -> series.appointments(includeDeleted) }.sortedWith(compareBy<Appointment> { it.startDate }.thenBy { it.startTime })

  fun addAppointmentSeries(appointmentSeries: AppointmentSeries) = this.appointmentSeries.add(appointmentSeries)

  fun prisonerNumbers() = appointmentSeries().flatMap { appointmentSeries -> appointmentSeries.appointments(true).flatMap { it.prisonerNumbers() } }.distinct()

  fun usernames() = listOfNotNull(createdBy, updatedBy).union(appointments(true).flatMap { appointment -> appointment.usernames() }).distinct()

  fun toModel() = AppointmentSetModel(
    id = this.appointmentSetId,
    prisonCode = prisonCode,
    categoryCode = categoryCode,
    tier = appointmentTier?.toModelEventTier(),
    organiser = appointmentOrganiser?.toModelEventOrganiser(),
    customName = customName,
    internalLocationId = internalLocationId,
    dpsLocationId = dpsLocationId,
    inCell = inCell,
    startDate = startDate,
    appointments = this.appointmentSeries().flatMap { it.appointments() }.toModel(),
    createdTime = createdTime,
    createdBy = createdBy,
  )

  fun toSummary() = AppointmentSetSummary(
    id = this.appointmentSetId,
    appointmentCount = this.appointmentSeries().flatMap { it.appointments(true) }.size,
    scheduledAppointmentCount = this.appointmentSeries().flatMap { it.scheduledAppointments() }.size,
  )

  fun toDetails(
    prisonerMap: Map<String, Prisoner>,
    referenceCodeMap: Map<String, ReferenceCode>,
    locationMap: Map<Long, LocationDetails>,
  ): AppointmentSetDetails = AppointmentSetDetails(
    appointmentSetId,
    prisonCode,
    referenceCodeMap[categoryCode].toAppointmentName(categoryCode, customName),
    referenceCodeMap[categoryCode].toAppointmentCategorySummary(categoryCode),
    customName,
    if (inCell) {
      null
    } else {
      locationMap[internalLocationId].toAppointmentLocationSummary(internalLocationId!!, dpsLocationId, prisonCode)
    },
    inCell,
    startDate,
    appointmentSeries().flatMap { it.appointmentDetails(prisonerMap, referenceCodeMap, locationMap) }.filterNot { it.attendees.isEmpty() },
    createdTime,
    createdBy,
    updatedTime,
    updatedBy,
  )
}
