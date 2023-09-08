package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Where
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.UserDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentName
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toSummary
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment as AppointmentOccurrenceModel

@Entity
@Table(name = "appointment")
@Where(clause = "NOT is_deleted")
@EntityListeners(AppointmentEntityListener::class)
data class Appointment(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val appointmentId: Long = 0,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "appointment_series_id", nullable = false)
  val appointmentSeries: AppointmentSeries,

  val sequenceNumber: Int,

  val prisonCode: String,

  var categoryCode: String,

  var customName: String?,

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "appointment_tier_id")
  var appointmentTier: AppointmentTier,

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "appointment_host_id")
  var appointmentHost: AppointmentHost? = null,

  var internalLocationId: Long?,

  val customLocation: String? = null,

  var inCell: Boolean = false,

  var onWing: Boolean = false,

  var offWing: Boolean = true,

  var startDate: LocalDate,

  var startTime: LocalTime,

  var endTime: LocalTime?,

  var unlockNotes: String? = null,

  var extraInformation: String? = null,

  val createdTime: LocalDateTime,

  val createdBy: String,

  var updatedTime: LocalDateTime? = null,

  var updatedBy: String? = null,
) {
  var cancelledTime: LocalDateTime? = null

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "cancellation_reason_id")
  var cancellationReason: AppointmentCancellationReason? = null

  var cancelledBy: String? = null

  var isDeleted: Boolean = false

  @OneToMany(mappedBy = "appointment", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  private val attendees: MutableList<AppointmentAttendee> = mutableListOf()

  fun attendees() = attendees.toList()

  fun addAttendee(attendee: AppointmentAttendee) {
    failIfIndividualAppointmentAlreadyAllocated()
    attendees.add(attendee)
  }

  fun removeAttendee(attendee: AppointmentAttendee) = attendees.remove(attendee)

  fun prisonerNumbers() = attendees().map { attendee -> attendee.prisonerNumber }.distinct()

  fun startDateTime(): LocalDateTime = LocalDateTime.of(startDate, startTime)

  fun isScheduled() = !isExpired() && !isCancelled() && !isDeleted

  fun isEdited() = updatedTime != null

  fun isCancelled() = cancelledTime != null && !isDeleted

  fun isExpired() = startDateTime() < LocalDateTime.now()

  fun usernames() = listOfNotNull(createdBy, updatedBy, cancelledBy)

  fun toModel() = AppointmentOccurrenceModel(
    id = appointmentId,
    sequenceNumber = sequenceNumber,
    categoryCode = categoryCode,
    customName = customName,
    internalLocationId = internalLocationId,
    inCell = inCell,
    startDate = startDate,
    startTime = startTime,
    endTime = endTime,
    extraInformation = extraInformation,
    cancelledTime = cancelledTime,
    cancellationReasonId = cancellationReason?.appointmentCancellationReasonId,
    cancelledBy = cancelledBy,
    updatedTime = updatedTime,
    updatedBy = updatedBy,
    allocations = attendees().toModel(),
  )

  fun toSummary(
    referenceCodeMap: Map<String, ReferenceCode>,
    locationMap: Map<Long, Location>,
    userMap: Map<String, UserDetail>,
  ) =
    AppointmentSummary(
      appointmentId,
      sequenceNumber,
      referenceCodeMap[categoryCode].toAppointmentName(categoryCode, customName),
      referenceCodeMap[categoryCode].toAppointmentCategorySummary(categoryCode),
      customName,
      if (inCell) {
        null
      } else {
        locationMap[internalLocationId].toAppointmentLocationSummary(
          internalLocationId!!,
          prisonCode,
        )
      },
      inCell,
      startDate,
      startTime,
      endTime,
      extraInformation,
      isEdited = isEdited(),
      isCancelled = isCancelled(),
      updatedTime = updatedTime,
      updatedBy?.let { userMap[updatedBy].toSummary(updatedBy!!) },
    )

  fun toDetails(
    prisonerMap: Map<String, Prisoner>,
    referenceCodeMap: Map<String, ReferenceCode>,
    locationMap: Map<Long, Location>,
    userMap: Map<String, UserDetail>,
  ) =
    AppointmentDetails(
      appointmentId,
      appointmentSeries.appointmentSeriesId,
      appointmentSeries.appointmentSet?.toSummary(),
      appointmentSeries.appointmentType,
      sequenceNumber,
      prisonCode,
      referenceCodeMap[categoryCode].toAppointmentName(categoryCode, customName),
      attendees().map { prisonerMap[it.prisonerNumber].toSummary(prisonCode, it.prisonerNumber, it.bookingId) },
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
      extraInformation,
      appointmentSeries.schedule?.toRepeat(),
      isEdited(),
      isCancelled(),
      isExpired(),
      appointmentSeries.createdTime,
      userMap[appointmentSeries.createdBy].toSummary(appointmentSeries.createdBy),
      updatedTime,
      if (updatedBy == null) {
        null
      } else {
        userMap[updatedBy].toSummary(updatedBy!!)
      },
      cancelledTime,
      if (cancelledBy == null) {
        null
      } else {
        userMap[cancelledBy].toSummary(cancelledBy!!)
      },
    )

  private fun failIfIndividualAppointmentAlreadyAllocated() {
    if (appointmentSeries.appointmentType == AppointmentType.INDIVIDUAL && attendees().isNotEmpty()) {
      throw IllegalArgumentException("Cannot allocate multiple prisoners to an individual appointment")
    }
  }
}

fun List<Appointment>.toModel() = map { it.toModel() }

fun List<Appointment>.toSummary(
  referenceCodeMap: Map<String, ReferenceCode>,
  locationMap: Map<Long, Location>,
  userMap: Map<String, UserDetail>,
) = map { it.toSummary(referenceCodeMap, locationMap, userMap) }

fun List<Appointment>.toDetails(
  prisonerMap: Map<String, Prisoner>,
  referenceCodeMap: Map<String, ReferenceCode>,
  locationMap: Map<Long, Location>,
  userMap: Map<String, UserDetail>,
) = map { it.toDetails(prisonerMap, referenceCodeMap, locationMap, userMap) }
