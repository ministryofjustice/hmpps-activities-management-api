package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.*
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.Where
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(name = "appointment")
@SQLDelete(sql = "UPDATE appointment SET deleted = true WHERE appointment_category_id = ?")
@Where(clause = "deleted = false")
data class Appointment (
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val appointmentId: Long = -1,

  @OneToOne
  @JoinColumn(name = "appointment_category_id", nullable = false)
  val category: AppointmentCategory,

  val prisonCode: String,

  val internalLocationId: Int?,

  val inCell: Boolean,

  val startDate: LocalDate,

  val startTime: LocalTime,

  val endTime: LocalTime?,

  val comment: String,

  val created: LocalDateTime,

  val createdBy: String,

  val updated: LocalDateTime?,

  val updatedBy: String?,

  val deleted: Boolean
) {
  fun toModel() = uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment(
    id = appointmentId,
    category = category.toModel(),
    prisonCode = prisonCode,
    internalLocationId = internalLocationId,
    inCell = inCell,
    startDate = startDate,
    startTime = startTime,
    endTime = endTime,
    comment = comment,
    created = created,
    createdBy = createdBy,
    updated = updated,
    updatedBy = updatedBy
  )
}

fun List<Appointment>.toModel() = map { it.toModel() }