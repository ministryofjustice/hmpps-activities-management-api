package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.UserDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentName
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toSummary
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment as AppointmentModel

@Entity
@Table(name = "appointment_series")
data class AppointmentSeries(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val appointmentSeriesId: Long = 0,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinTable(
    name = "appointment_set_appointment_series",
    joinColumns = [JoinColumn(name = "appointment_series_id")],
    inverseJoinColumns = [JoinColumn(name = "appointment_set_id")],
  )
  val appointmentSet: AppointmentSet? = null,

  @Enumerated(EnumType.STRING)
  val appointmentType: AppointmentType,

  val prisonCode: String,

  val categoryCode: String,

  val customName: String? = null,

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "appointment_tier_id")
  var appointmentTier: AppointmentTier,

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "appointment_host_id")
  var appointmentHost: AppointmentHost? = null,

  val internalLocationId: Long?,

  val customLocation: String? = null,

  val inCell: Boolean = false,

  val onWing: Boolean = false,

  val offWing: Boolean = true,

  val startDate: LocalDate,

  val startTime: LocalTime,

  val endTime: LocalTime?,

  @OneToOne(fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @JoinColumn(name = "appointment_series_schedule_id")
  var schedule: AppointmentSeriesSchedule? = null,

  var unlockNotes: String? = null,

  val extraInformation: String? = null,

  val createdTime: LocalDateTime = LocalDateTime.now(),

  val createdBy: String,

  var updatedTime: LocalDateTime? = null,

  var updatedBy: String? = null,

  val isMigrated: Boolean = false,
) {
  fun scheduleIterator() =
    schedule?.let { AppointmentSeriesScheduleIterator(startDate, schedule!!.frequency, schedule!!.numberOfAppointments) }
      ?: AppointmentSeriesScheduleIterator(startDate, AppointmentFrequency.DAILY, 1)

  @OneToMany(mappedBy = "appointmentSeries", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @OrderBy("sequenceNumber ASC")
  private val appointments: MutableList<Appointment> = mutableListOf()

  fun appointments() = appointments.filterNot { it.isDeleted() }.toList()

  fun scheduledAppointments() = appointments().filter { it.isScheduled() }.toList()

  fun scheduledAppointmentsAfter(startDateTime: LocalDateTime) = scheduledAppointments().filter { it.startDateTime() > startDateTime }.toList()

  fun applyToAppointments(appointment: Appointment, applyTo: ApplyTo, action: String): List<Appointment> {
    require(!appointment.isExpired()) {
      "Cannot $action a past appointment"
    }

    require(!appointment.isCancelled()) {
      "Cannot $action a cancelled appointment"
    }

    require(!appointment.isDeleted()) {
      "Cannot $action a deleted appointment"
    }

    return when (applyTo) {
      ApplyTo.THIS_AND_ALL_FUTURE_OCCURRENCES -> listOf(appointment).union(
        scheduledAppointmentsAfter(appointment.startDateTime()),
      ).toList()
      ApplyTo.ALL_FUTURE_OCCURRENCES -> scheduledAppointments()
      else -> listOf(appointment)
    }
  }

  fun appointmentDetails(
    prisonerMap: Map<String, Prisoner>,
    referenceCodeMap: Map<String, ReferenceCode>,
    locationMap: Map<Long, Location>,
    userMap: Map<String, UserDetail>,
  ) = appointments().toDetails(prisonerMap, referenceCodeMap, locationMap, userMap)

  fun addAppointment(occurrence: Appointment) = appointments.add(occurrence)

  fun removeAppointment(occurrence: Appointment) = appointments.remove(occurrence)

  fun internalLocationIds() =
    listOf(internalLocationId).union(appointments().map { appointment -> appointment.internalLocationId }).filterNotNull()

  fun prisonerNumbers(): List<String> {
    val orderedAppointments = appointments().sortedBy { it.startDateTime() }
    if (orderedAppointments.isEmpty()) return emptyList()
    return (orderedAppointments.firstOrNull { !it.isExpired() } ?: orderedAppointments.last()).prisonerNumbers()
  }

  fun usernames() =
    listOf(createdBy, updatedBy).union(appointments().flatMap { appointment -> appointment.usernames() }).filterNotNull()

  fun toModel() = AppointmentModel(
    id = appointmentSeriesId,
    appointmentType = appointmentType,
    prisonCode = prisonCode,
    categoryCode = categoryCode,
    appointmentDescription = customName,
    internalLocationId = internalLocationId,
    inCell = inCell,
    startDate = startDate,
    startTime = startTime,
    endTime = endTime,
    repeat = schedule?.toRepeat(),
    comment = extraInformation,
    created = createdTime,
    createdBy = createdBy,
    updated = updatedTime,
    updatedBy = updatedBy,
    occurrences = appointments().toModel(),
  )

  fun toDetails(
    prisoners: List<Prisoner>,
    referenceCodeMap: Map<String, ReferenceCode>,
    locationMap: Map<Long, Location>,
    userMap: Map<String, UserDetail>,
  ) =
    AppointmentDetails(
      appointmentSeriesId,
      appointmentType,
      prisonCode,
      referenceCodeMap[categoryCode].toAppointmentName(categoryCode, customName),
      prisoners.toSummary(),
      referenceCodeMap[categoryCode].toAppointmentCategorySummary(categoryCode),
      customName,
      if (inCell) {
        null
      } else {
        locationMap[internalLocationId].toAppointmentLocationSummary(internalLocationId!!, prisonCode)
      },
      inCell,
      startDate,
      startTime,
      endTime,
      schedule?.toRepeat(),
      extraInformation,
      createdTime,
      userMap[createdBy].toSummary(createdBy),
      updatedTime,
      if (updatedBy == null) {
        null
      } else {
        userMap[updatedBy].toSummary(updatedBy!!)
      },
      appointments().toSummary(referenceCodeMap, locationMap, userMap),
    )
}

fun List<AppointmentSeries>.toModel() = map { it.toModel() }

enum class AppointmentType {
  INDIVIDUAL,
  GROUP,
}
