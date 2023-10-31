package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentCreateDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType

/**
 * This job is used to asynchronously create the remaining appointments and the attendees of those appointments for an appointment series.
 * It is used only when creating very large group appointment series. Those are defined as any appointment series that will be represented by
 * more than 500 appointment instances, the number of attendees multiplied by the number of appointments. This appointment instance count is
 * configurable via applications.max-sync-appointment-instance-actions.
 *
 * If an appointment series is identified as very large (see createFirstAppointmentOnly logic in AppointmentSeriesService.createAppointmentSeries) then
 * only the first appointment is created synchronously. This job is then executed asynchronously to create the remaining appointments.
 *
 * This means that a usable appointment series with its first appointment is returned as quickly as possible, preventing the user having to
 * wait an extended period of time for feedback. This was needed as the creation of a 360 attendee repeating weekly appointment series, the
 * largest seen in production, would take a minute and cause timeouts on the frontend.
 *
 * The side effect of this approach is that the user will not see all the appointments within a series until this job has completed.
 * This is only for a short time window (minutes) and only affects the 1% of very large appointments created in the service.
 */
@Component
class CreateAppointmentsJob(
  private val jobRunner: SafeJobRunner,
  private val appointmentCreateDomainService: AppointmentCreateDomainService,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Async("asyncExecutor")
  fun execute(
    appointmentSeriesId: Long,
    prisonNumberBookingIdMap: Map<String, Long>,
    startTimeInMs: Long,
    categoryDescription: String,
    locationDescription: String,
  ) {
    jobRunner.runJob(
      JobDefinition(JobType.CREATE_APPOINTMENTS) {
        log.info("Creating remaining appointments for appointment series with id '$appointmentSeriesId' asynchronously")

        appointmentCreateDomainService.createAppointments(
          appointmentSeriesId,
          prisonNumberBookingIdMap,
          startTimeInMs,
          categoryDescription,
          locationDescription,
        )

        log.info("Created remaining appointments for appointment series with id '$appointmentSeriesId' asynchronously")
      },
    )
  }
}
