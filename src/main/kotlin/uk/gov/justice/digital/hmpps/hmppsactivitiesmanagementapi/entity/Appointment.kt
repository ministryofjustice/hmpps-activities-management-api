package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.Where
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment as AppointmentModel

@Entity
@Table(name = "appointment")
@SQLDelete(sql = "UPDATE appointment SET deleted = true WHERE appointment_category_id = ?")
@Where(clause = "deleted = false")
data class Appointment(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val appointmentId: Long = -1,

  @OneToOne
  @JoinColumn(name = "appointment_category_id", nullable = false)
  var category: AppointmentCategory,

  val prisonCode: String,

  var internalLocationId: Long?,

  var inCell: Boolean,

  var startDate: LocalDate,

  var startTime: LocalTime,

  var endTime: LocalTime?,

  var comment: String,

  val created: LocalDateTime = LocalDateTime.now(),

  val createdBy: String,

  var updated: LocalDateTime? = null,

  var updatedBy: String? = null,

  val deleted: Boolean = false
) {
  @OneToMany(mappedBy = "appointment", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  private val occurrences: MutableList<AppointmentOccurrence> = mutableListOf()

  fun occurrences() = occurrences.toList()

  fun addOccurrence(occurrence: AppointmentOccurrence) = occurrences.add(occurrence)

  fun toModel() = AppointmentModel(
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
    updatedBy = updatedBy,
    occurrences = occurrences.toModel(),
  )
}

fun List<Appointment>.toModel() = map { it.toModel() }
