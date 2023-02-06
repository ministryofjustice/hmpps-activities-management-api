package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrence as AppointmentOccurrenceModel

@Entity
@Table(name = "appointment_occurrence")
data class AppointmentOccurrence(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val appointmentOccurrenceId: Long = -1,

  @ManyToOne
  @JoinColumn(name = "appointment_id", nullable = false)
  val appointment: Appointment,

  var internalLocationId: Int?,

  var inCell: Boolean,

  var startDate: LocalDate,

  var startTime: LocalTime,

  var endTime: LocalTime?,

  var comment: String,

  var cancelled: Boolean,

  var updated: LocalDateTime?,

  var updatedBy: String?,

  @OneToMany(mappedBy = "appointmentOccurrence", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  private val allocations: MutableList<AppointmentOccurrenceAllocation> = mutableListOf()
) {
  fun allocations() = allocations.toList()

  fun addAllocation(allocation: AppointmentOccurrenceAllocation) = allocations.add(allocation)

  fun toModel() = AppointmentOccurrenceModel(
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
