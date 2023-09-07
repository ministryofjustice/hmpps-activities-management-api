package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentAttendee
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import kotlin.system.measureTimeMillis

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
  private val appointmentRepository: AppointmentRepository,
  private val appointmentOccurrenceRepository: AppointmentOccurrenceRepository,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Async("asyncExecutor")
  fun execute(appointmentId: Long, prisonerBookings: Map<String, String?>) {
    jobRunner.runJob(
      JobDefinition(JobType.CREATE_APPOINTMENTS) {
        createAppointments(appointmentId, prisonerBookings)
      },
    )
  }

  private fun createAppointments(appointmentSeriesId: Long, prisonerBookings: Map<String, String?>) {
    log.info("Creating remaining appointments for appointment series with id $appointmentSeriesId")

    val elapsed = measureTimeMillis {
      val appointmentSeries = appointmentRepository.findOrThrowNotFound(appointmentSeriesId)

      appointmentSeries.scheduleIterator().withIndex().forEach {
        val sequenceNumber = it.index + 1
        val appointment = appointmentOccurrenceRepository.findByAppointmentSeriesAndSequenceNumber(appointmentSeries, sequenceNumber)
        if (appointment == null) {
          log.info("Creating appointment $sequenceNumber with ${prisonerBookings.size} attendees for appointment series with id $appointmentSeriesId")
          runCatching {
            appointmentOccurrenceRepository.saveAndFlush(
              Appointment(
                appointmentSeries = appointmentSeries,
                sequenceNumber = sequenceNumber,
                prisonCode = appointmentSeries.prisonCode,
                categoryCode = appointmentSeries.categoryCode,
                customName = appointmentSeries.customName,
                appointmentTier = appointmentSeries.appointmentTier,
                appointmentHost = appointmentSeries.appointmentHost,
                internalLocationId = appointmentSeries.internalLocationId,
                inCell = appointmentSeries.inCell,
                startDate = it.value,
                startTime = appointmentSeries.startTime,
                endTime = appointmentSeries.endTime,
                extraInformation = appointmentSeries.extraInformation,
                createdTime = appointmentSeries.createdTime,
                createdBy = appointmentSeries.createdBy,
              ).apply {
                prisonerBookings.forEach { prisonerBooking ->
                  this.addAttendee(
                    AppointmentAttendee(
                      appointment = this,
                      prisonerNumber = prisonerBooking.key,
                      bookingId = prisonerBooking.value!!.toLong(),
                    ),
                  )
                }
              },
            )
          }.onSuccess {
            log.info("Successfully created appointment $sequenceNumber with ${prisonerBookings.size} attendees for appointment series with id $appointmentSeriesId")
          }.onFailure {
            log.error("Failed to create appointment $sequenceNumber with ${prisonerBookings.size} attendees for appointment series with id $appointmentSeriesId")
          }
        }
      }
    }

    log.info("Creating remaining appointments for appointment series with id $appointmentSeriesId took ${elapsed}ms")
  }
}
