package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.*
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(name = "appointment_occurrence")
data class AppointmentOccurrence (
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val appointmentOccurrenceId: Long = -1,

  @ManyToOne
  @JoinColumn(name = "appointment_id", nullable = false)
  val appointment: Appointment,

  val internalLocationId: Int?,

  val inCell: Boolean,

  val startDate: LocalDate,

  val startTime: LocalTime,

  val endTime: LocalTime?,

  val comment: String,

  val cancelled: Boolean,

  val updated: LocalDateTime?,

  val updatedBy: String?,

  @OneToMany(mappedBy = "appointmentOccurrence", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  val allocations: MutableList<AppointmentOccurrenceAllocation> = mutableListOf()
) {
  fun toModel() = uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrence(
    id = appointmentOccurrenceId,
    internalLocationId = internalLocationId,
    inCell = inCell,
    startDate = startDate,
    startTime = startTime,
    endTime = endTime,
    comment = comment,
    cancelled = cancelled,
    updated = updated,
    updatedBy = updatedBy,
    allocations = allocations.toModel()
  )
}

fun List<AppointmentOccurrence>.toModel() = map { it.toModel() }