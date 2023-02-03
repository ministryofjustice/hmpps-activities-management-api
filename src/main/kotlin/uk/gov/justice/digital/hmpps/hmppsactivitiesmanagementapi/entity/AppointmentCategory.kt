package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "appointment_category")
data class AppointmentCategory(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val appointmentCategoryId: Long = -1,

  val code: String,

  val description: String,

  val active: Boolean,

  val displayOrder: Int?
) {
  fun toModel() = uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentCategory(
    id = appointmentCategoryId,
    code = code,
    description = description,
    active = active,
    displayOrder = displayOrder
  )
}

fun List<AppointmentCategory>.toModel() = map { it.toModel() }
