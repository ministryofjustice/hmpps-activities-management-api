package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment

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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventOrganiser
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeriesDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeriesSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.LocationService.LocationDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.toAppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentName
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelEventOrganiser
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelEventTier
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeries as AppointmentSeriesModel

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
  val appointmentTier: EventTier?,

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

  var cancelledBy: String? = null,

  var cancelledTime: LocalDateTime? = null,

  var cancellationStartDate: LocalDate? = null,

  var cancellationStartTime: LocalTime? = null,

  var dpsLocationId: UUID? = null,
) {
  @OneToOne
  @JoinColumn(name = "appointment_organiser_id")
  var appointmentOrganiser: EventOrganiser? = null
    set(value) {
      require(value == null || appointmentTier?.isTierTwo() == true) { "Cannot add organiser unless appointment series is Tier 2." }

      field = value
    }

  fun scheduleIterator() = schedule?.let { AppointmentSeriesScheduleIterator(startDate, schedule!!.frequency, schedule!!.numberOfAppointments) }
    ?: AppointmentSeriesScheduleIterator(startDate, AppointmentFrequency.DAILY, 1)

  @OneToMany(mappedBy = "appointmentSeries", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @OrderBy("sequenceNumber ASC")
  private val appointments: MutableList<Appointment> = mutableListOf()

  fun appointments(includeDeleted: Boolean = false) = appointments.filter { !it.isDeleted || includeDeleted }.toList()

  fun scheduledAppointments() = appointments().filter { it.isScheduled() }.toList()

  fun cancelledAppointments() = appointments().filter { it.isCancelled() && !it.isExpired() }.toList()

  fun cancelledAppointmentsAfter(startDateTime: LocalDateTime) = cancelledAppointments().filter { it.startDateTime() > startDateTime }.toList()

  fun scheduledAppointmentsAfter(startDateTime: LocalDateTime) = scheduledAppointments().filter { it.startDateTime() > startDateTime }.toList()

  fun applyToAppointments(appointment: Appointment, applyTo: ApplyTo, action: String, cancelled: Boolean): List<Appointment> {
    require(appointment.isEditable()) {
      "Cannot $action an appointment more than 5 days ago"
    }

    if (!cancelled) {
      require(!appointment.isCancelled()) {
        "Cannot $action a cancelled appointment"
      }
    } else {
      require(appointment.isCancelled()) {
        "Cannot $action an uncancelled appointment"
      }
    }

    require(!appointment.isDeleted) {
      "Cannot $action a deleted appointment"
    }

    return when (applyTo) {
      ApplyTo.THIS_AND_ALL_FUTURE_APPOINTMENTS -> {
        if (cancelled) {
          listOf(appointment).union(
            cancelledAppointmentsAfter(appointment.startDateTime()),
          ).toList()
        } else {
          listOf(appointment).union(
            scheduledAppointmentsAfter(appointment.startDateTime()),
          ).toList()
        }
      }
      ApplyTo.ALL_FUTURE_APPOINTMENTS -> {
        if (cancelled) {
          cancelledAppointments()
        } else {
          scheduledAppointments()
        }
      }
      else -> listOf(appointment)
    }
  }

  fun uncancel(updatedTime: LocalDateTime?, updatedBy: String?) {
    this.cancelledTime = null
    this.cancelledBy = null
    this.cancellationStartDate = null
    this.cancellationStartTime = null
    this.updatedTime = updatedTime
    this.updatedBy = updatedBy
  }
  fun cancel(cancelledTime: LocalDateTime = LocalDateTime.now(), cancelledBy: String, cancellationStartDate: LocalDate, cancellationStartTime: LocalTime) {
    this.cancelledTime = cancelledTime
    this.cancelledBy = cancelledBy
    this.cancellationStartDate = cancellationStartDate
    this.cancellationStartTime = cancellationStartTime
  }

  fun appointmentDetails(
    prisonerMap: Map<String, Prisoner>,
    referenceCodeMap: Map<String, ReferenceCode>,
    locationMap: Map<Long, LocationDetails>,
  ) = appointments(true).toDetails(prisonerMap, referenceCodeMap, locationMap)

  fun addAppointment(appointment: Appointment) = appointments.add(appointment)

  fun usernames() = listOfNotNull(createdBy, updatedBy).distinct()

  fun toModel() = AppointmentSeriesModel(
    id = appointmentSeriesId,
    appointmentType = appointmentType,
    prisonCode = prisonCode,
    categoryCode = categoryCode,
    tier = appointmentTier?.toModelEventTier(),
    organiser = appointmentOrganiser?.toModelEventOrganiser(),
    customName = customName,
    internalLocationId = internalLocationId,
    dpsLocationId = dpsLocationId,
    inCell = inCell,
    startDate = startDate,
    startTime = startTime,
    endTime = endTime,
    schedule = schedule?.toModel(),
    extraInformation = extraInformation,
    createdTime = createdTime,
    createdBy = createdBy,
    updatedTime = updatedTime,
    updatedBy = updatedBy,
    appointments = appointments(true).toModel(),
  )

  fun toSummary() = AppointmentSeriesSummary(
    id = appointmentSeriesId,
    schedule = schedule?.toModel(),
    appointmentCount = appointments(true).size,
    scheduledAppointmentCount = scheduledAppointments().size,
  )

  fun toDetails(
    referenceCodeMap: Map<String, ReferenceCode>,
    locationMap: Map<Long, LocationDetails>,
  ) = AppointmentSeriesDetails(
    appointmentSeriesId,
    appointmentType,
    prisonCode,
    referenceCodeMap[categoryCode].toAppointmentName(categoryCode, customName),
    referenceCodeMap[categoryCode].toAppointmentCategorySummary(categoryCode),
    appointmentTier?.toModelEventTier(),
    appointmentOrganiser?.toModelEventOrganiser(),
    customName,
    if (inCell) {
      null
    } else {
      locationMap[internalLocationId].toAppointmentLocationSummary(internalLocationId!!, dpsLocationId, prisonCode)
    },
    inCell,
    startDate,
    startTime,
    endTime,
    schedule?.toModel(),
    extraInformation,
    createdTime,
    createdBy,
    updatedTime,
    updatedBy,
    appointments(true).toSummary(),
  )
}

fun List<AppointmentSeries>.toModel() = map { it.toModel() }

enum class AppointmentType {
  INDIVIDUAL,
  GROUP,
}
