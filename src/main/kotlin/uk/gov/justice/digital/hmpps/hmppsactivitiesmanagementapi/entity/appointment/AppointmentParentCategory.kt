package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentParentCategory as ModelAppointmentParentCategory

@Entity
@Table(name = "appointment_parent_category")
data class AppointmentParentCategory(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val appointmentParentCategoryId: Long = 0,

  val name: String,

  val description: String? = null,
) {
  fun toModel() = ModelAppointmentParentCategory(
    id = appointmentParentCategoryId,
    name = name,
    description = description,
  )
}

fun List<AppointmentParentCategory>.toModel() = map { it.toModel() }
