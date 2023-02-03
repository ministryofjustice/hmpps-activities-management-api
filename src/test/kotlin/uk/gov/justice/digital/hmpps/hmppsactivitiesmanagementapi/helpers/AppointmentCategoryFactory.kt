package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentCategory

internal fun appointmentCategoryEntity() =
  AppointmentCategory(
    appointmentCategoryId = 1,
    code = "TEST",
    description = "Test Category",
    active = true,
    displayOrder = 2
  )
