package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentUpdateRequest
import java.time.LocalDate
import java.time.LocalTime

class AppointmentOccurrenceTelemetryTransformFunctionsTest {
  @Test
  fun `update appointment to telemetry properties no property changed`() {
    AppointmentUpdateRequest().toTelemetryPropertiesMap(
      "TEST.USER",
      "MDI",
      1,
      2,
    ) isEqualTo mutableMapOf(
      USER_PROPERTY_KEY to "TEST.USER",
      PRISON_CODE_PROPERTY_KEY to "MDI",
      APPOINTMENT_SERIES_ID_PROPERTY_KEY to "1",
      APPOINTMENT_ID_PROPERTY_KEY to "2",
      CATEGORY_CHANGED_PROPERTY_KEY to false.toString(),
      INTERNAL_LOCATION_CHANGED_PROPERTY_KEY to false.toString(),
      START_DATE_CHANGED_PROPERTY_KEY to false.toString(),
      START_TIME_CHANGED_PROPERTY_KEY to false.toString(),
      END_TIME_CHANGED_PROPERTY_KEY to false.toString(),
      EXTRA_INFORMATION_CHANGED_PROPERTY_KEY to false.toString(),
      APPLY_TO_PROPERTY_KEY to ApplyTo.THIS_APPOINTMENT.toString(),
    )
  }

  @Test
  fun `update appointment to telemetry properties category code changed`() {
    with(
      AppointmentUpdateRequest(categoryCode = "NEW").toTelemetryPropertiesMap(
        "TEST.USER",
        "MDI",
        1,
        2,
      ),
    ) {
      this[CATEGORY_CHANGED_PROPERTY_KEY] isEqualTo true.toString()
      this[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY] isEqualTo false.toString()
      this[START_DATE_CHANGED_PROPERTY_KEY] isEqualTo false.toString()
      this[START_TIME_CHANGED_PROPERTY_KEY] isEqualTo false.toString()
      this[END_TIME_CHANGED_PROPERTY_KEY] isEqualTo false.toString()
      this[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY] isEqualTo false.toString()
    }
  }

  @Test
  fun `update appointment to telemetry properties internal location changed`() {
    with(
      AppointmentUpdateRequest(internalLocationId = 123).toTelemetryPropertiesMap(
        "TEST.USER",
        "MDI",
        1,
        2,
      ),
    ) {
      this[CATEGORY_CHANGED_PROPERTY_KEY] isEqualTo false.toString()
      this[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY] isEqualTo true.toString()
      this[START_DATE_CHANGED_PROPERTY_KEY] isEqualTo false.toString()
      this[START_TIME_CHANGED_PROPERTY_KEY] isEqualTo false.toString()
      this[END_TIME_CHANGED_PROPERTY_KEY] isEqualTo false.toString()
      this[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY] isEqualTo false.toString()
    }
  }

  @Test
  fun `update appointment to telemetry properties start date changed`() {
    with(
      AppointmentUpdateRequest(startDate = LocalDate.now().plusDays(1)).toTelemetryPropertiesMap(
        "TEST.USER",
        "MDI",
        1,
        2,
      ),
    ) {
      this[CATEGORY_CHANGED_PROPERTY_KEY] isEqualTo false.toString()
      this[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY] isEqualTo false.toString()
      this[START_DATE_CHANGED_PROPERTY_KEY] isEqualTo true.toString()
      this[START_TIME_CHANGED_PROPERTY_KEY] isEqualTo false.toString()
      this[END_TIME_CHANGED_PROPERTY_KEY] isEqualTo false.toString()
      this[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY] isEqualTo false.toString()
    }
  }

  @Test
  fun `update appointment to telemetry properties start time changed`() {
    with(
      AppointmentUpdateRequest(startTime = LocalTime.of(9, 30)).toTelemetryPropertiesMap(
        "TEST.USER",
        "MDI",
        1,
        2,
      ),
    ) {
      this[CATEGORY_CHANGED_PROPERTY_KEY] isEqualTo false.toString()
      this[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY] isEqualTo false.toString()
      this[START_DATE_CHANGED_PROPERTY_KEY] isEqualTo false.toString()
      this[START_TIME_CHANGED_PROPERTY_KEY] isEqualTo true.toString()
      this[END_TIME_CHANGED_PROPERTY_KEY] isEqualTo false.toString()
      this[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY] isEqualTo false.toString()
    }
  }

  @Test
  fun `update appointment to telemetry properties end time changed`() {
    with(
      AppointmentUpdateRequest(endTime = LocalTime.of(15, 0)).toTelemetryPropertiesMap(
        "TEST.USER",
        "MDI",
        1,
        2,
      ),
    ) {
      this[CATEGORY_CHANGED_PROPERTY_KEY] isEqualTo false.toString()
      this[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY] isEqualTo false.toString()
      this[START_DATE_CHANGED_PROPERTY_KEY] isEqualTo false.toString()
      this[START_TIME_CHANGED_PROPERTY_KEY] isEqualTo false.toString()
      this[END_TIME_CHANGED_PROPERTY_KEY] isEqualTo true.toString()
      this[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY] isEqualTo false.toString()
    }
  }

  @Test
  fun `update appointment to telemetry properties extra information changed`() {
    with(
      AppointmentUpdateRequest(extraInformation = "New").toTelemetryPropertiesMap(
        "TEST.USER",
        "MDI",
        1,
        2,
      ),
    ) {
      this[CATEGORY_CHANGED_PROPERTY_KEY] isEqualTo false.toString()
      this[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY] isEqualTo false.toString()
      this[START_DATE_CHANGED_PROPERTY_KEY] isEqualTo false.toString()
      this[START_TIME_CHANGED_PROPERTY_KEY] isEqualTo false.toString()
      this[END_TIME_CHANGED_PROPERTY_KEY] isEqualTo false.toString()
      this[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY] isEqualTo true.toString()
    }
  }

  @Test
  fun `update appointment to telemetry properties apply to this and all future occurrences`() {
    with(
      AppointmentUpdateRequest(applyTo = ApplyTo.THIS_AND_ALL_FUTURE_APPOINTMENTS).toTelemetryPropertiesMap(
        "TEST.USER",
        "MDI",
        1,
        2,
      ),
    ) {
      this[APPLY_TO_PROPERTY_KEY] isEqualTo ApplyTo.THIS_AND_ALL_FUTURE_APPOINTMENTS.toString()
    }
  }

  @Test
  fun `update appointment to telemetry properties apply to all future occurrences`() {
    with(
      AppointmentUpdateRequest(applyTo = ApplyTo.ALL_FUTURE_APPOINTMENTS).toTelemetryPropertiesMap(
        "TEST.USER",
        "MDI",
        1,
        2,
      ),
    ) {
      this[APPLY_TO_PROPERTY_KEY] isEqualTo ApplyTo.ALL_FUTURE_APPOINTMENTS.toString()
    }
  }

  @Test
  fun `update appointment to telemetry metrics no property changed`() {
    AppointmentUpdateRequest().toTelemetryMetricsMap(3, 0) isEqualTo mutableMapOf(
      APPOINTMENT_COUNT_METRIC_KEY to 3.0,
      APPOINTMENT_INSTANCE_COUNT_METRIC_KEY to 0.0,
      PRISONERS_REMOVED_COUNT_METRIC_KEY to 0.0,
      PRISONERS_ADDED_COUNT_METRIC_KEY to 0.0,
      EVENT_TIME_MS_METRIC_KEY to 0.0,
    )
  }

  @Test
  fun `update appointment to telemetry metrics remove prisoners`() {
    AppointmentUpdateRequest(
      removePrisonerNumbers = listOf("A1234BC", "B2345CD"),
    ).toTelemetryMetricsMap(3, 6) isEqualTo mutableMapOf(
      APPOINTMENT_COUNT_METRIC_KEY to 3.0,
      APPOINTMENT_INSTANCE_COUNT_METRIC_KEY to 6.0,
      PRISONERS_REMOVED_COUNT_METRIC_KEY to 2.0,
      PRISONERS_ADDED_COUNT_METRIC_KEY to 0.0,
      EVENT_TIME_MS_METRIC_KEY to 0.0,
    )
  }

  @Test
  fun `update appointment to telemetry metrics add prisoners`() {
    AppointmentUpdateRequest(
      addPrisonerNumbers = listOf("A1234BC", "B2345CD", "C3456DE"),
    ).toTelemetryMetricsMap(3, 9) isEqualTo mutableMapOf(
      APPOINTMENT_COUNT_METRIC_KEY to 3.0,
      APPOINTMENT_INSTANCE_COUNT_METRIC_KEY to 9.0,
      PRISONERS_REMOVED_COUNT_METRIC_KEY to 0.0,
      PRISONERS_ADDED_COUNT_METRIC_KEY to 3.0,
      EVENT_TIME_MS_METRIC_KEY to 0.0,
    )
  }

  @Test
  fun `cancel appointment to telemetry properties no property changed`() {
    AppointmentCancelRequest(cancellationReasonId = 1).toTelemetryPropertiesMap(
      "TEST.USER",
      "MDI",
      1,
      2,
    ) isEqualTo mutableMapOf(
      USER_PROPERTY_KEY to "TEST.USER",
      PRISON_CODE_PROPERTY_KEY to "MDI",
      APPOINTMENT_SERIES_ID_PROPERTY_KEY to "1",
      APPOINTMENT_ID_PROPERTY_KEY to "2",
      APPLY_TO_PROPERTY_KEY to ApplyTo.THIS_APPOINTMENT.toString(),
    )
  }
}
