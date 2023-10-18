package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentCancelDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCancelRequest
import java.time.LocalDateTime
import kotlin.system.measureTimeMillis

/**
 * This job is used to asynchronously cancel the remaining appointments and the attendees of those appointments for an appointment series.
 * It is used only when cancelling very large group appointment series in a way that will affect more than 500 of appointment instances
 * representing that appointment series. This appointment instance count is configurable via applications.max-sync-appointment-instance-actions.
 *
 * If a cancel is identified as very large (see cancelFirstAppointmentOnly logic in AppointmentService.cancelAppointment)
 * then only the initial appointment is cancelled synchronously. This job is then executed asynchronously to cancel the remaining appointments.
 *
 * This means that a usable cancelled appointment is returned as quickly as possible, preventing the user having to wait an extended period of time
 * for feedback. This was needed as certain cancellations of a 360 attendee repeating weekly appointment series, the largest seen in production, would
 * take a minute and cause timeouts on the frontend.
 *
 * The side effect of this approach is that the user will not see all the cancellations of appointments within a series until this job has completed.
 * This is only for a short time window (minutes) and only affects the 1% of very large appointment series cancelled in the service.
 */
@Component
class CancelAppointmentsJob(
  private val jobRunner: SafeJobRunner,
  private val service: AppointmentCancelDomainService,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Async("asyncExecutor")
  fun execute(
    appointmentSeriesId: Long,
    appointmentId: Long,
    appointmentIdsToCancel: Set<Long>,
    request: AppointmentCancelRequest,
    cancelledTime: LocalDateTime,
    cancelledBy: String,
    cancelAppointmentsCount: Int,
    cancelInstancesCount: Int,
    startTimeInMs: Long,
  ) {
    jobRunner.runJob(
      JobDefinition(JobType.CANCEL_APPOINTMENTS) {
        log.info("Cancelling remaining appointments for series with id $appointmentSeriesId")
        val elapsed = measureTimeMillis {
          service.cancelAppointmentIds(
            appointmentSeriesId,
            appointmentId,
            appointmentIdsToCancel,
            request,
            cancelledTime,
            cancelledBy,
            cancelAppointmentsCount,
            cancelInstancesCount,
            startTimeInMs,
          )
        }
        log.info("Cancelling remaining appointments for series with id $appointmentSeriesId took ${elapsed}ms")
      },
    )
  }
}
