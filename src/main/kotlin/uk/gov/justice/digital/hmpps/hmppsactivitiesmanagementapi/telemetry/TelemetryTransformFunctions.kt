package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceUpdateRequest

fun AppointmentOccurrenceUpdateRequest.toTelemetryPropertiesMap(
  user: String,
  prisonCode: String,
  appointmentSeriesId: Long,
  appointmentId: Long,
) =
  mutableMapOf(
    USER_PROPERTY_KEY to user,
    PRISON_CODE_PROPERTY_KEY to prisonCode,
    APPOINTMENT_SERIES_ID_PROPERTY_KEY to appointmentSeriesId.toString(),
    APPOINTMENT_ID_PROPERTY_KEY to appointmentId.toString(),
    CATEGORY_CHANGED_PROPERTY_KEY to (this.categoryCode != null).toString(),
    INTERNAL_LOCATION_CHANGED_PROPERTY_KEY to (this.internalLocationId != null).toString(),
    START_DATE_CHANGED_PROPERTY_KEY to (this.startDate != null).toString(),
    START_TIME_CHANGED_PROPERTY_KEY to (this.startTime != null).toString(),
    END_TIME_CHANGED_PROPERTY_KEY to (this.endTime != null).toString(),
    EXTRA_INFORMATION_CHANGED_PROPERTY_KEY to (this.comment != null).toString(),
    APPLY_TO_PROPERTY_KEY to this.applyTo.toString(),
  )

fun AppointmentOccurrenceUpdateRequest.toTelemetryMetricsMap(appointmentCount: Int, appointmentInstanceCount: Int) =
  mutableMapOf(
    APPOINTMENT_COUNT_METRIC_KEY to appointmentCount.toDouble(),
    APPOINTMENT_INSTANCE_COUNT_METRIC_KEY to appointmentInstanceCount.toDouble(),
    PRISONERS_REMOVED_COUNT_METRIC_KEY to (this.removePrisonerNumbers?.size?.toDouble() ?: 0.0),
    PRISONERS_ADDED_COUNT_METRIC_KEY to (this.addPrisonerNumbers?.size?.toDouble() ?: 0.0),
    EVENT_TIME_MS_METRIC_KEY to 0.0,
  )

fun AppointmentOccurrenceCancelRequest.toTelemetryPropertiesMap(
  user: String,
  prisonCode: String,
  appointmentSeriesId: Long,
  appointmentId: Long,
) =
  mutableMapOf(
    USER_PROPERTY_KEY to user,
    PRISON_CODE_PROPERTY_KEY to prisonCode,
    APPOINTMENT_SERIES_ID_PROPERTY_KEY to appointmentSeriesId.toString(),
    APPOINTMENT_ID_PROPERTY_KEY to appointmentId.toString(),
    APPLY_TO_PROPERTY_KEY to this.applyTo.toString(),
  )
