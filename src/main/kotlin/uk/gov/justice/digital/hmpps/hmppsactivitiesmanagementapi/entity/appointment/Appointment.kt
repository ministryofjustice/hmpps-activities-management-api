package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.springframework.data.domain.AbstractAggregateRoot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AppointmentAttendeeRemovalReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AppointmentCancellationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventOrganiser
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSummary
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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment as AppointmentModel

@Entity
@Table(name = "appointment")
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
  var appointmentTier: EventTier? = null,

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

  var dpsLocationId: UUID? = null,
) : AbstractAggregateRoot<Appointment>() {
  @OneToOne
  @JoinColumn(name = "appointment_organiser_id")
  var appointmentOrganiser: EventOrganiser? = null
    set(value) {
      require(value == null || appointmentTier?.isTierTwo() == true) { "Cannot add organiser unless appointment is Tier 2." }

      field = value
    }

  var cancelledTime: LocalDateTime? = null

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "cancellation_reason_id")
  var cancellationReason: AppointmentCancellationReason? = null

  var cancelledBy: String? = null

  var isDeleted: Boolean = false

  fun cancel(cancelledTime: LocalDateTime = LocalDateTime.now(), cancellationReason: AppointmentCancellationReason, cancelledBy: String) {
    this.cancelledTime = cancelledTime
    this.cancellationReason = cancellationReason
    this.cancelledBy = cancelledBy
    this.isDeleted = cancellationReason.isDelete
  }

  fun uncancel(updateBy: String, updateTime: LocalDateTime = LocalDateTime.now()) {
    this.updatedBy = updateBy
    this.updatedTime = updateTime
    this.cancelledTime = null
    this.cancellationReason = null
    this.cancelledBy = null
  }

  @OneToMany(mappedBy = "appointment", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  private val attendees: MutableList<AppointmentAttendee> = mutableListOf()

  fun attendees() = attendees.filterNot { it.isDeleted }.toList()

  fun findAttendeeRecords(prisonerNumber: String) = attendees().filter { it.prisonerNumber == prisonerNumber }

  fun findAttendees(prisonerNumbers: Collection<String>) = attendees().filter { prisonerNumbers.contains(it.prisonerNumber) }

  // Should only be used when creating appointments initially. Adding new attendees after creation should use the function below
  fun addAttendee(attendee: AppointmentAttendee): AppointmentAttendee {
    failIfIndividualAppointmentAlreadyAllocated()
    attendees.add(attendee)
    return attendee
  }

  fun addAttendee(prisonerNumber: String, bookingId: Long, addedTime: LocalDateTime? = LocalDateTime.now(), addedBy: String?): AppointmentAttendee? {
    // Soft delete any existing removed attendee records for the prisoner
    findAttendeeRecords(prisonerNumber).filter { it.isRemoved() }.forEach { it.isDeleted = true }

    // Add attendee if no non-soft deleted attendee records for the prisoner exist
    if (findAttendeeRecords(prisonerNumber).isNotEmpty()) return null

    val attendee = AppointmentAttendee(
      appointment = this,
      prisonerNumber = prisonerNumber,
      bookingId = bookingId,
      addedTime = addedTime,
      addedBy = addedBy,
    )
    return addAttendee(attendee)
  }

  fun removeAttendee(prisonerNumber: String, removedTime: LocalDateTime = LocalDateTime.now(), removalReason: AppointmentAttendeeRemovalReason, removedBy: String?) = findAttendeeRecords(prisonerNumber).onEach {
    it.remove(removedTime, removalReason, removedBy)
  }

  fun prisonerNumbers() = attendees().map { attendee -> attendee.prisonerNumber }.distinct()

  fun startDateTime(): LocalDateTime = LocalDateTime.of(startDate, startTime)

  fun isScheduled() = startDateTime() >= LocalDateTime.now() && !isCancelled() && !isDeleted

  fun isEdited() = updatedTime != null

  fun isCancelled() = cancelledTime != null && !isDeleted

  fun isExpired() = startDateTime() < LocalDate.now().atStartOfDay()

  fun isEditable() = startDateTime() > LocalDateTime.now().minusDays(6)

  fun usernames() = listOfNotNull(createdBy, updatedBy, cancelledBy).distinct()

  fun toModel() = AppointmentModel(
    id = appointmentId,
    sequenceNumber = sequenceNumber,
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
    extraInformation = extraInformation,
    createdTime = createdTime,
    createdBy = createdBy,
    updatedTime = updatedTime,
    updatedBy = updatedBy,
    cancelledTime = cancelledTime,
    cancellationReasonId = cancellationReason?.appointmentCancellationReasonId,
    cancelledBy = cancelledBy,
    isDeleted = isDeleted,
    attendees = attendees().toModel(),
  )

  fun toSummary() = AppointmentSummary(
    appointmentId,
    sequenceNumber,
    startDate,
    startTime,
    endTime,
    isEdited = isEdited(),
    isCancelled = isCancelled(),
    isDeleted = isDeleted,
  )

  fun toDetails(
    prisonerMap: Map<String, Prisoner>,
    referenceCodeMap: Map<String, ReferenceCode>,
    locationMap: Map<Long, LocationDetails>,
  ) = AppointmentDetails(
    id = appointmentId,
    appointmentSeries = if (appointmentSeries.appointmentSet == null) appointmentSeries.toSummary() else null,
    appointmentSet = appointmentSeries.appointmentSet?.toSummary(),
    appointmentType = appointmentSeries.appointmentType,
    sequenceNumber = sequenceNumber,
    prisonCode = prisonCode,
    appointmentName = referenceCodeMap[categoryCode].toAppointmentName(categoryCode, customName),
    attendees = attendees().map { it.toSummary(prisonerMap) },
    category = referenceCodeMap[categoryCode].toAppointmentCategorySummary(categoryCode),
    tier = appointmentTier?.toModelEventTier(),
    organiser = appointmentOrganiser?.toModelEventOrganiser(),
    customName = customName,
    if (inCell) {
      null
    } else {
      locationMap[internalLocationId].toAppointmentLocationSummary(internalLocationId!!, dpsLocationId, prisonCode)
    },
    inCell = inCell,
    startDate = startDate,
    startTime = startTime,
    endTime = endTime,
    isExpired = isExpired(),
    extraInformation = extraInformation,
    createdTime = appointmentSeries.createdTime,
    createdBy = appointmentSeries.createdBy,
    isEdited = isEdited(),
    updatedTime = updatedTime,
    updatedBy = updatedBy,
    isCancelled = isCancelled(),
    isDeleted = isDeleted,
    cancelledTime = cancelledTime,
    cancelledBy = cancelledBy,
  )

  /**
   * Function exists for testing purposes. The AbstractAggregateRoot.domainEvents() function is protected so this
   * function supports testing the correct domain events have been registered
   */
  internal fun publishedDomainEvents() = this.domainEvents()

  private fun failIfIndividualAppointmentAlreadyAllocated() {
    if (appointmentSeries.appointmentType == AppointmentType.INDIVIDUAL && attendees().isNotEmpty()) {
      throw IllegalArgumentException("Cannot allocate multiple prisoners to an individual appointment")
    }
  }
}

fun List<Appointment>.toModel() = map { it.toModel() }

fun List<Appointment>.toSummary() = map { it.toSummary() }

fun List<Appointment>.toDetails(
  prisonerMap: Map<String, Prisoner>,
  referenceCodeMap: Map<String, ReferenceCode>,
  locationMap: Map<Long, LocationDetails>,
) = map { it.toDetails(prisonerMap, referenceCodeMap, locationMap) }

data class AppointmentAttendanceMarkedEvent(
  val appointmentId: Long,

  val prisonCode: String,

  val attendedPrisonNumbers: MutableList<String> = mutableListOf(),

  val nonAttendedPrisonNumbers: MutableList<String> = mutableListOf(),

  val attendanceChangedPrisonNumbers: MutableList<String> = mutableListOf(),

  val attendanceRecordedTime: LocalDateTime,

  val attendanceRecordedBy: String,
)
