package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentCategory

internal fun appointmentCategoryEntity(active: Boolean = true) =
  AppointmentCategory(
    appointmentCategoryId = 1,
    code = "TEST",
    description = "Test Category",
    active = active,
    displayOrder = 2,
  )

internal fun appointmentCategoryEntities() =
  listOf(
    AppointmentCategory(
      appointmentCategoryId = 1,
      code = "AC1",
      description = "Appointment Category 1",
      active = true,
      displayOrder = 1,
    ),
    AppointmentCategory(
      appointmentCategoryId = 2,
      code = "AC2",
      description = "Appointment Category 2",
      active = true,
      displayOrder = 2,
    ),
    AppointmentCategory(
      appointmentCategoryId = 3,
      code = "AC3",
      description = "Appointment Category 3",
      active = true,
      displayOrder = 3,
    ),
  )
