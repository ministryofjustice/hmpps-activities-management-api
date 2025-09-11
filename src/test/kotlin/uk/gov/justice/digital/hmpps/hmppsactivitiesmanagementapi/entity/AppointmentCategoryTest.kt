package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.toAppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.toAppointmentName
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentCategorySummary

class AppointmentCategoryTest {
  @Test
  fun `appointment code to appointment category summary returns category code for null appointment codes`() {
    assertThat((null as AppointmentCategory?).toAppointmentCategorySummary("MEDO")).isEqualTo(
      AppointmentCategorySummary("MEDO", "MEDO"),
    )
  }

  @Test
  fun `appointment code to appointment category summary mapping`() {
    assertThat(
      appointmentCategory(
        "MEDO",
        "Medical - Doctor",
      ).toAppointmentCategorySummary("MEDO"),
    ).isEqualTo(
      AppointmentCategorySummary("MEDO", "Medical - Doctor"),
    )
  }


  @Test
  fun `appointment code list to appointment category summary list mapping`() {
    assertThat(
      listOf(
        appointmentCategory(
          "MEDO",
          "Medical - Doctor",
        ),
      ).toAppointmentCategorySummary(),
    ).isEqualTo(
      listOf(AppointmentCategorySummary("MEDO", "Medical - Doctor")),
    )
  }

  @Test
  fun `appointment code to appointment name mapping`() {
    assertThat(
      appointmentCategory("MEDO", "Medical - Doctor")
        .toAppointmentName("MEDO", "John's doctor appointment"),
    ).isEqualTo("John's doctor appointment (Medical - Doctor)")
  }

  @Test
  fun `appointment code to appointment name mapping for null appointment code`() {
    assertThat(null.toAppointmentName("MEDO", "John's doctor appointment"))
      .isEqualTo("John's doctor appointment (MEDO)")
  }

  @Test
  fun `appointment code to appointment name mapping with no description`() {
    assertThat(
      appointmentCategory("MEDO", "Medical - Doctor")
        .toAppointmentName("MEDO", null),
    ).isEqualTo("Medical - Doctor")
  }

  @Test
  fun `appointment code to appointment name mapping for null appointment code and no description`() {
    assertThat(null.toAppointmentName("MEDO", null)).isEqualTo("MEDO")
  }

}