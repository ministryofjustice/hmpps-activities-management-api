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
 * This job is used to asynchronously update the remaining appointments and the attendees of those appointments for an appointment series.
 * It is used only when updating very large group appointment series in a way that will affect more than 500 of appointment instances
 * representing that appointment series. This appointment instance count is configurable via applications.max-sync-appointment-instance-actions.
 *
 * If an update is identified as very large (see updateFirstAppointmentOnly logic in AppointmentService.updateAppointment)
 * then only the initial appointment is updated synchronously. This job is then executed asynchronously to update the remaining appointments.
 *
 * This means that a usable updated appointment is returned as quickly as possible, preventing the user having to wait an extended period of time
 * for feedback. This was needed as certain updates to a 360 attendee repeating weekly appointment series, the largest seen in production, would
 * take a minute and cause timeouts on the frontend.
 *
 * The side effect of this approach is that the user will not see all the updates to appointments within a series until this job has completed.
 * This is only for a short time window (minutes) and only affects the 1% of very large appointments updated in the service.
 */
@Component
class UpdateAppointmentsJob(
  private val jobRunner: SafeJobRunner,
  private val service: AppointmentUpdateDomainService,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Async("asyncExecutor")
  fun execute(
    appointmentSeriesId: Long,
    appointmentId: Long,
    appointmentIdsToUpdate: Set<Long>,
    request: AppointmentOccurrenceUpdateRequest,
    prisonerMap: Map<String, Prisoner>,
    updated: LocalDateTime,
    updatedBy: String,
    updateAppointmentsCount: Int,
    updateInstancesCount: Int,
    startTimeInMs: Long,
  ) {
    jobRunner.runJob(
      JobDefinition(JobType.UPDATE_APPOINTMENTS) {
        log.info("Updating remaining appointments for appointment series with id $appointmentSeriesId")
        val elapsed = measureTimeMillis {
          service.updateAppointmentIds(
            appointmentSeriesId,
            appointmentId,
            appointmentIdsToUpdate,
            request,
            prisonerMap,
            updated,
            updatedBy,
            updateAppointmentsCount,
            updateInstancesCount,
            startTimeInMs,
          )
        }
        log.info("Updating remaining appointments for appointment series with id $appointmentSeriesId took ${elapsed}ms")
      },
    )
  }
}
