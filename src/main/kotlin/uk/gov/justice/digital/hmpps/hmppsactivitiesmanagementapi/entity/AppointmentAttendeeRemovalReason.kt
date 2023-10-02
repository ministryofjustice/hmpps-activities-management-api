package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "appointment_attendee_removal_reason")
data class AppointmentAttendeeRemovalReason(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val appointmentAttendeeRemovalReasonId: Long = 0,

  val description: String,

  val isDelete: Boolean,
)
