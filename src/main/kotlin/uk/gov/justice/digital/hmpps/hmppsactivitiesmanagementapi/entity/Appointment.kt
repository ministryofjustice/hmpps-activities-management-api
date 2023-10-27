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
import org.springframework.data.domain.AbstractAggregateRoot
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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment as AppointmentModel

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
) : AbstractAggregateRoot<Appointment>() {
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

  @OneToMany(mappedBy = "appointment", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  private val attendees: MutableList<AppointmentAttendee> = mutableListOf()

  fun attendees() = attendees.filterNot { it.isDeleted }.toList()

  fun findAttendee(prisonerNumber: String) = attendees().filter { it.prisonerNumber == prisonerNumber }

  fun findAttendees(prisonerNumbers: List<String>) = attendees().filter { prisonerNumbers.contains(it.prisonerNumber) }

  // Should only be used when creating appointments initially. Adding new attendees after creation should use the function below
  fun addAttendee(attendee: AppointmentAttendee) {
    failIfIndividualAppointmentAlreadyAllocated()
    attendees.add(attendee)
  }

  fun addAttendee(prisonerNumber: String, bookingId: Long, addedTime: LocalDateTime? = LocalDateTime.now(), addedBy: String?) {
    // Soft delete any existing removed attendee records for the prisoner
    findAttendee(prisonerNumber).filter { it.isRemoved() }.forEach { it.isDeleted = true }
    // Add attendee if no non-soft deleted attendee records for the prisoner exist
    findAttendee(prisonerNumber).let {
      if (it.isEmpty()) {
        failIfIndividualAppointmentAlreadyAllocated()
        attendees.add(
          AppointmentAttendee(
            appointment = this,
            prisonerNumber = prisonerNumber,
            bookingId = bookingId,
            addedTime = addedTime,
            addedBy = addedBy,
          ),
        )
      }
    }
  }

  fun removeAttendee(prisonerNumber: String, removedTime: LocalDateTime = LocalDateTime.now(), removalReason: AppointmentAttendeeRemovalReason, removedBy: String?) {
    findAttendee(prisonerNumber).forEach {
      it.remove(removedTime, removalReason, removedBy)
    }
  }

  fun markPrisonerAttendance(attendedPrisonNumbers: List<String>, nonAttendedPrisonNumbers: List<String>, attendanceRecordedTime: LocalDateTime = LocalDateTime.now(), attendanceRecordedBy: String) {
    require(!isCancelled()) {
      "Cannot mark attendance for a cancelled appointment"
    }

    val event = AppointmentAttendanceMarkedEvent(
      appointmentId = appointmentId,
      prisonCode = prisonCode,
      attendanceRecordedTime = attendanceRecordedTime,
      attendanceRecordedBy = attendanceRecordedBy,
    )

    findAttendees(attendedPrisonNumbers).forEach {
      event.attendedPrisonNumbers.add(it.prisonerNumber)
      if (it.attended != null) event.attendanceChangedPrisonNumbers.add(it.prisonerNumber)

      it.attended = true
      it.attendanceRecordedTime = attendanceRecordedTime
      it.attendanceRecordedBy = attendanceRecordedBy
    }

    findAttendees(nonAttendedPrisonNumbers).forEach {
      event.nonAttendedPrisonNumbers.add(it.prisonerNumber)
      if (it.attended != null) event.attendanceChangedPrisonNumbers.add(it.prisonerNumber)

      it.attended = false
      it.attendanceRecordedTime = attendanceRecordedTime
      it.attendanceRecordedBy = attendanceRecordedBy
    }

    registerEvent(event)
  }

  fun prisonerNumbers() = attendees().map { attendee -> attendee.prisonerNumber }.distinct()

  fun startDateTime(): LocalDateTime = LocalDateTime.of(startDate, startTime)

  fun isScheduled() = !isExpired() && !isCancelled() && !isDeleted

  fun isEdited() = updatedTime != null

  fun isCancelled() = cancelledTime != null && !isDeleted

  fun isExpired() = startDateTime() < LocalDateTime.now()

  fun usernames() = listOfNotNull(createdBy, updatedBy, cancelledBy).distinct()

  fun attendeeUsernames() = attendees().flatMap { it.usernames() }.distinct()

  fun toModel() = AppointmentModel(
    id = appointmentId,
    sequenceNumber = sequenceNumber,
    prisonCode = prisonCode,
    categoryCode = categoryCode,
    customName = customName,
    internalLocationId = internalLocationId,
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
    attendees = attendees().toModel(),
  )

  fun toSummary() =
    AppointmentSummary(
      appointmentId,
      sequenceNumber,
      startDate,
      startTime,
      endTime,
      isEdited = isEdited(),
      isCancelled = isCancelled(),
    )

  fun toDetails(
    prisonerMap: Map<String, Prisoner>,
    referenceCodeMap: Map<String, ReferenceCode>,
    locationMap: Map<Long, Location>,
    userMap: Map<String, UserDetail>,
  ) =
    AppointmentDetails(
      appointmentId,
      if (appointmentSeries.appointmentSet == null) appointmentSeries.toSummary() else null,
      appointmentSeries.appointmentSet?.toSummary(),
      appointmentSeries.appointmentType,
      sequenceNumber,
      prisonCode,
      referenceCodeMap[categoryCode].toAppointmentName(categoryCode, customName),
      attendees().map { it.toSummary(prisonerMap, userMap) },
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
      isExpired(),
      extraInformation,
      appointmentSeries.createdTime,
      userMap[appointmentSeries.createdBy].toSummary(appointmentSeries.createdBy),
      isEdited(),
      updatedTime,
      if (updatedBy == null) {
        null
      } else {
        userMap[updatedBy].toSummary(updatedBy!!)
      },
      isCancelled(),
      cancelledTime,
      if (cancelledBy == null) {
        null
      } else {
        userMap[cancelledBy].toSummary(cancelledBy!!)
      },
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
  locationMap: Map<Long, Location>,
  userMap: Map<String, UserDetail>,
) = map { it.toDetails(prisonerMap, referenceCodeMap, locationMap, userMap) }

data class AppointmentAttendanceMarkedEvent(
  val appointmentId: Long,

  val prisonCode: String,

  val attendedPrisonNumbers: MutableList<String> = mutableListOf(),

  val nonAttendedPrisonNumbers: MutableList<String> = mutableListOf(),

  val attendanceChangedPrisonNumbers: MutableList<String> = mutableListOf(),

  val attendanceRecordedTime: LocalDateTime,

  val attendanceRecordedBy: String,
)
