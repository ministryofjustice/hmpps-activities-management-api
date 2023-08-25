package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrence
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrenceUpdateDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentCancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPLY_TO_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_ID_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_INSTANCE_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_SERIES_ID_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_TIME_MS_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISON_CODE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.USER_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess
import java.security.Principal
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment as AppointmentModel

@Service
@Transactional
class AppointmentOccurrenceService(
  private val appointmentRepository: AppointmentRepository,
  private val appointmentOccurrenceRepository: AppointmentOccurrenceRepository,
  private val appointmentCancellationReasonRepository: AppointmentCancellationReasonRepository,
  private val referenceCodeService: ReferenceCodeService,
  private val locationService: LocationService,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val appointmentOccurrenceUpdateDomainService: AppointmentOccurrenceUpdateDomainService,
  private val telemetryClient: TelemetryClient,
) {
  fun updateAppointmentOccurrence(appointmentOccurrenceId: Long, request: AppointmentOccurrenceUpdateRequest, principal: Principal): AppointmentModel {
    val startTimeInMs = System.currentTimeMillis()
    val now = LocalDateTime.now()

    val appointmentOccurrence = appointmentOccurrenceRepository.findOrThrowNotFound(appointmentOccurrenceId)
    val appointment = appointmentOccurrence.appointment
    checkCaseloadAccess(appointment.prisonCode)

    require(!appointmentOccurrence.isCancelled()) {
      "Cannot update a cancelled appointment occurrence"
    }

    require(!appointmentOccurrence.isExpired()) {
      "Cannot update a past appointment occurrence"
    }

    if (request.categoryCode != null) {
      referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT)
        .also {
          require(it.containsKey(request.categoryCode)) {
            "Appointment Category with code ${request.categoryCode} not found or is not active"
          }
        }
    }

    if (request.internalLocationId != null) {
      locationService.getLocationsForAppointmentsMap(appointment.prisonCode)
        .also {
          require(it.containsKey(request.internalLocationId)) {
            "Appointment location with id ${request.internalLocationId} not found in prison '${appointment.prisonCode}'"
          }
        }
    }

    val prisonerMap = if (request.addPrisonerNumbers.isNullOrEmpty()) {
      emptyMap()
    } else {
      require(appointment.appointmentType != AppointmentType.INDIVIDUAL) {
        "Cannot add prisoners to an individual appointment occurrence"
      }
      prisonerSearchApiClient.findByPrisonerNumbers(request.addPrisonerNumbers).block()!!
        .filter { prisoner -> prisoner.prisonId == appointment.prisonCode }
        .associateBy { prisoner -> prisoner.prisonerNumber }
        .also {
          val missingPrisonerNumbers = request.addPrisonerNumbers.filter { number -> !it.containsKey(number) }
          require(missingPrisonerNumbers.isEmpty()) {
            "Prisoner(s) with prisoner number(s) '${missingPrisonerNumbers.joinToString("', '")}' not found, were inactive or are residents of a different prison."
          }
        }
    }

    return appointmentOccurrenceUpdateDomainService.updateAppointmentOccurrences(
      appointment,
      appointmentOccurrenceId,
      determineOccurrencesToApplyTo(appointment, appointmentOccurrence, request.applyTo),
      request,
      prisonerMap,
      now,
      principal.name,
      startTimeInMs,
      true,
    )
  }

  fun cancelAppointmentOccurrence(appointmentOccurrenceId: Long, request: AppointmentOccurrenceCancelRequest, principal: Principal): AppointmentModel {
    val startTime = System.currentTimeMillis()
    val appointmentOccurrence = appointmentOccurrenceRepository.findOrThrowNotFound(appointmentOccurrenceId)
    val appointment = appointmentOccurrence.appointment
    checkCaseloadAccess(appointment.prisonCode)

    val cancellationReason = appointmentCancellationReasonRepository.findOrThrowNotFound(request.cancellationReasonId)

    val now = LocalDateTime.now()
    if (LocalDateTime.of(appointmentOccurrence.startDate, appointmentOccurrence.startTime) < now) {
      throw IllegalArgumentException("Cannot cancel a past appointment occurrence")
    }

    val occurrencesToUpdate = determineOccurrencesToApplyTo(appointment, appointmentOccurrence, request.applyTo)

    occurrencesToUpdate.forEach {
      it.cancellationReason = cancellationReason
      it.cancelled = now
      it.cancelledBy = principal.name
      it.deleted = cancellationReason.isDelete
    }

    val updatedAppointment = appointmentRepository.saveAndFlush(appointment)

    occurrencesToUpdate.filter { it.isDeleted() }
      .flatMap { it.allocations().map { alloc -> alloc.appointmentOccurrenceAllocationId } }
      .also {
        logAppointmentDeletedMetric(
          principal,
          appointmentOccurrenceId,
          request,
          updatedAppointment,
          occurrencesToUpdate.size,
          occurrencesToUpdate.flatMap { it.allocations() }.size,
          startTime,
        )
      }

    occurrencesToUpdate.filter { it.isCancelled() }
      .flatMap { it.allocations().map { alloc -> alloc.appointmentOccurrenceAllocationId } }
      .also {
        logAppointmentCancelledMetric(
          principal,
          appointmentOccurrenceId,
          request,
          updatedAppointment,
          occurrencesToUpdate.size,
          occurrencesToUpdate.flatMap { it.allocations() }.size,
          startTime,
        )
      }

    return updatedAppointment.toModel()
  }

  private fun determineOccurrencesToApplyTo(appointment: Appointment, appointmentOccurrence: AppointmentOccurrence, applyTo: ApplyTo) =
    when (applyTo) {
      ApplyTo.THIS_AND_ALL_FUTURE_OCCURRENCES -> listOf(appointmentOccurrence).union(
        appointment.scheduledOccurrencesAfter(appointmentOccurrence.startDateTime()),
      )
      ApplyTo.ALL_FUTURE_OCCURRENCES -> appointment.scheduledOccurrences()
      else -> listOf(appointmentOccurrence)
    }

  private fun logAppointmentCancelledMetric(
    principal: Principal,
    appointmentOccurrenceId: Long,
    request: AppointmentOccurrenceCancelRequest,
    appointment: Appointment,
    occurrencesCancelled: Int,
    instancesCancelled: Int,
    startTimeInMs: Long,
  ) {
    val propertiesMap = mapOf(
      USER_PROPERTY_KEY to principal.name,
      PRISON_CODE_PROPERTY_KEY to appointment.prisonCode,
      APPOINTMENT_SERIES_ID_PROPERTY_KEY to appointment.appointmentId.toString(),
      APPOINTMENT_ID_PROPERTY_KEY to appointmentOccurrenceId.toString(),
      APPLY_TO_PROPERTY_KEY to request.applyTo.toString(),
    )

    val metricsMap = mapOf(
      APPOINTMENT_COUNT_METRIC_KEY to occurrencesCancelled.toDouble(),
      APPOINTMENT_INSTANCE_COUNT_METRIC_KEY to instancesCancelled.toDouble(),
      EVENT_TIME_MS_METRIC_KEY to (System.currentTimeMillis() - startTimeInMs).toDouble(),
    )

    telemetryClient.trackEvent(TelemetryEvent.APPOINTMENT_CANCELLED.value, propertiesMap, metricsMap)
  }

  private fun logAppointmentDeletedMetric(
    principal: Principal,
    appointmentOccurrenceId: Long,
    request: AppointmentOccurrenceCancelRequest,
    appointment: Appointment,
    occurrencesDeleted: Int,
    instancesDeleted: Int,
    startTimeInMs: Long,
  ) {
    val propertiesMap = mapOf(
      USER_PROPERTY_KEY to principal.name,
      PRISON_CODE_PROPERTY_KEY to appointment.prisonCode,
      APPOINTMENT_SERIES_ID_PROPERTY_KEY to appointment.appointmentId.toString(),
      APPOINTMENT_ID_PROPERTY_KEY to appointmentOccurrenceId.toString(),
      APPLY_TO_PROPERTY_KEY to request.applyTo.toString(),
    )

    val metricsMap = mapOf(
      APPOINTMENT_COUNT_METRIC_KEY to occurrencesDeleted.toDouble(),
      APPOINTMENT_INSTANCE_COUNT_METRIC_KEY to instancesDeleted.toDouble(),
      EVENT_TIME_MS_METRIC_KEY to (System.currentTimeMillis() - startTimeInMs).toDouble(),
    )

    telemetryClient.trackEvent(TelemetryEvent.APPOINTMENT_DELETED.value, propertiesMap, metricsMap)
  }
}
