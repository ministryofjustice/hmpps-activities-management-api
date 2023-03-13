package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "appointment_schedule")
data class AppointmentSchedule(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val appointmentScheduleId: Long = -1,

  @Enumerated(EnumType.STRING)
  var repeatPeriod: AppointmentRepeatPeriod,

  var repeatCount: Int,
)
