package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Where
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.UserDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.OffenderMergeDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentAttendeeSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toSummary
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentAttendee as AppointmentAttendeeModel

@Entity
@Table(name = "appointment_attendee")
@Where(clause = "NOT is_deleted")
data class AppointmentAttendee(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val appointmentAttendeeId: Long = 0,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "appointment_id", nullable = false)
  val appointment: Appointment,

  val prisonerNumber: String,

  val bookingId: Long,

  var addedTime: LocalDateTime? = null,

  var addedBy: String? = null,

  var attended: Boolean? = null,

  var attendanceRecordedTime: LocalDateTime? = null,

  var attendanceRecordedBy: String? = null,
) {
  var removedTime: LocalDateTime? = null

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "removal_reason_id")
  var removalReason: AppointmentAttendeeRemovalReason? = null

  var removedBy: String? = null

  fun isRemoved() = removedTime != null && !isDeleted

  var isDeleted: Boolean = false

  fun remove(removedTime: LocalDateTime = LocalDateTime.now(), removalReason: AppointmentAttendeeRemovalReason, removedBy: String?): AppointmentAttendee {
    this.removedTime = removedTime
    this.removalReason = removalReason
    this.removedBy = removedBy
    isDeleted = removalReason.isDelete
    return this
  }

  fun usernames() = listOfNotNull(addedBy, attendanceRecordedBy, removedBy).distinct()

  fun toModel() = AppointmentAttendeeModel(
    id = appointmentAttendeeId,
    prisonerNumber = prisonerNumber,
    bookingId = bookingId,
    addedTime = addedTime,
    addedBy = addedBy,
    attended = attended,
    attendanceRecordedTime = attendanceRecordedTime,
    attendanceRecordedBy = attendanceRecordedBy,
    removedTime = removedTime,
    removalReasonId = removalReason?.appointmentAttendeeRemovalReasonId,
    removedBy = removedBy,
  )

  fun toSummary(prisonerMap: Map<String, Prisoner>, userMap: Map<String, UserDetail>) = AppointmentAttendeeSummary(
    id = appointmentAttendeeId,
    prisoner = prisonerMap[prisonerNumber].toSummary(prisonerNumber, bookingId),
    attended = attended,
    attendanceRecordedTime = attendanceRecordedTime,
    attendanceRecordedBy = if (attendanceRecordedBy == null) {
      null
    } else {
      userMap[attendanceRecordedBy].toSummary(attendanceRecordedBy!!)
    },
  )

  fun merge(offenderMergeDetails: OffenderMergeDetails) {
    // TODO to be implemented
  }
}

fun List<AppointmentAttendee>.toModel() = map { it.toModel() }

fun List<AppointmentAttendee>.toSummary(prisonerMap: Map<String, Prisoner>, userMap: Map<String, UserDetail>) =
  map { it.toSummary(prisonerMap, userMap) }
