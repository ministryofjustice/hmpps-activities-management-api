package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCategoryRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentCategory as ModelAppointmentCategory

@Entity
@Table(name = "appointment_category")
data class AppointmentCategory(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val appointmentCategoryId: Long = 0,

  var code: String,

  var description: String,

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "appointment_parent_category_id")
  var appointmentParentCategory: AppointmentParentCategory,

  var status: CategoryStatus,
) {
  fun updateCategory(request: AppointmentCategoryRequest, appointmentParentCategory: AppointmentParentCategory) {
    this.code = request.code
    this.description = request.description
    this.appointmentParentCategory = appointmentParentCategory
    this.status = request.status
  }

  fun toModel() = ModelAppointmentCategory(
    id = appointmentCategoryId,
    code = code,
    description = description,
    appointmentParentCategory = appointmentParentCategory,
    status = status,
  )
}

fun List<AppointmentCategory>.toModel() = map { it.toModel() }

enum class CategoryStatus {
  ACTIVE,
  INACTIVE,
}