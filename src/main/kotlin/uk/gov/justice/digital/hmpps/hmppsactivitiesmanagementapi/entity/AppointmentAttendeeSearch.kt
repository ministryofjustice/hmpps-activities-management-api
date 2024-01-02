package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.SQLRestriction
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentAttendeeSearchResult

@Entity
@Immutable
@Table(name = "appointment_attendee")
@SQLRestriction("NOT is_deleted")
data class AppointmentAttendeeSearch(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val appointmentAttendeeId: Long = 0,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "appointment_id", nullable = false)
  val appointmentSearch: AppointmentSearch,

  val prisonerNumber: String,

  val bookingId: Long,
) {
  fun toResult() = AppointmentAttendeeSearchResult(
    appointmentAttendeeId = appointmentAttendeeId,
    prisonerNumber = prisonerNumber,
    bookingId = bookingId,
  )
}

fun List<AppointmentAttendeeSearch>.toResult() = map { it.toResult() }
