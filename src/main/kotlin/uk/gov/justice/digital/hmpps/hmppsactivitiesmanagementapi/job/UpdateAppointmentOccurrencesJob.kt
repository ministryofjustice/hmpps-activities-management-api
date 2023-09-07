package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentUpdateDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceUpdateRequest
import java.time.LocalDateTime
import kotlin.system.measureTimeMillis

/**
 * This job is used to asynchronously update the remaining occurrences and the allocations to those occurrences for an appointment.
 * It is used only when updating very large group appointments in a way that will affect more than 500 of appointment instances
 * representing that appointment. This appointment instance count is configurable via applications.max-sync-appointment-instance-actions.
 *
 * If an update is identified as very large (see updateFirstOccurrenceOnly logic in AppointmentOccurrenceService.updateAppointmentOccurrence)
 * then only the initial occurrence is updated synchronously. This job is then executed asynchronously to update the remaining occurrences.
 *
 * This means that a usable updated occurrence is returned as quickly as possible, preventing the user having to wait an extended period of time
 * for feedback. This was needed as certain updates to a 360 attendee repeating weekly appointment, the largest seen in production, would
 * take a minute and cause timeouts on the frontend.
 *
 * The side effect of this approach is that the user will not see all the updates to appointments within a series until this job has completed.
 * This is only for a short time window (minutes) and only affects the 1% of very large appointments updated in the service.
 */
@Component
class UpdateAppointmentOccurrencesJob(
    private val jobRunner: SafeJobRunner,
    private val service: AppointmentUpdateDomainService,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Async("asyncExecutor")
  fun execute(
    appointmentId: Long,
    appointmentOccurrenceId: Long,
    occurrenceIdsToUpdate: Set<Long>,
    request: AppointmentOccurrenceUpdateRequest,
    prisonerMap: Map<String, Prisoner>,
    updated: LocalDateTime,
    updatedBy: String,
    updateOccurrencesCount: Int,
    updateInstancesCount: Int,
    startTimeInMs: Long,
  ) {
    jobRunner.runJob(
      JobDefinition(JobType.UPDATE_APPOINTMENTS) {
        log.info("Updating remaining occurrences for appointment with id $appointmentId")
        val elapsed = measureTimeMillis {
          service.updateAppointmentIds(
            appointmentId,
            appointmentOccurrenceId,
            occurrenceIdsToUpdate,
            request,
            prisonerMap,
            updated,
            updatedBy,
            updateOccurrencesCount,
            updateInstancesCount,
            startTimeInMs,
          )
        }
        log.info("Updating remaining occurrences for appointment with id $appointmentId took ${elapsed}ms")
      },
    )
  }
}
