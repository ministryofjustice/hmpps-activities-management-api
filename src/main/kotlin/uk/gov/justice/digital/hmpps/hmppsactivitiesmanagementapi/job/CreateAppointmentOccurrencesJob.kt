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
 * This job is used to asynchronously create the remaining occurrences and the allocations to those occurrences for an appointment.
 * It is used only when creating very large group appointments. Those are defined as any appointment that will be represented by
 * more than 500 appointment instances, the number of attendees multiplied by the repeat count. This appointment instance count is
 * configurable via applications.max-sync-appointment-instance-actions.
 *
 * If an appointment is identified as very large (see createFirstOccurrenceOnly logic in AppointmentService.createAppointment) then
 * only the first occurrence is created synchronously. This job is then executed asynchronously to create the remaining occurrences.
 *
 * This means that a usable appointment with its first occurrence is returned as quickly as possible, preventing the user having to
 * wait an extended period of time for feedback. This was needed as the creation of a 360 attendee repeating weekly appointment, the
 * largest seen in production, would take a minute and cause timeouts on the frontend.
 *
 * The side effect of this approach is that the user will not see all the appointments within a series until this job has completed.
 * This is only for a short time window (minutes) and only affects the 1% of very large appointments created in the service.
 */
@Component
class CreateAppointmentOccurrencesJob(
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
        createAppointmentOccurrences(appointmentId, prisonerBookings)
      },
    )
  }

  private fun createAppointmentOccurrences(appointmentId: Long, prisonerBookings: Map<String, String?>) {
    log.info("Creating remaining occurrences for appointment with id $appointmentId")

    val elapsed = measureTimeMillis {
      val appointmentSeries = appointmentRepository.findOrThrowNotFound(appointmentId)

      appointmentSeries.scheduleIterator().withIndex().forEach {
        val sequenceNumber = it.index + 1
        val occurrence = appointmentOccurrenceRepository.findByAppointmentAndSequenceNumber(appointmentSeries, sequenceNumber)
        if (occurrence == null) {
          log.info("Creating occurrence $sequenceNumber with ${prisonerBookings.size} allocations for appointment with id $appointmentId")
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
            log.info("Successfully created occurrence $sequenceNumber with ${prisonerBookings.size} allocations for appointment with id $appointmentId")
          }.onFailure {
            log.error("Failed to create occurrence $sequenceNumber with ${prisonerBookings.size} allocations for appointment with id $appointmentId")
          }
        }
      }
    }

    log.info("Creating remaining occurrences for appointment with id $appointmentId took ${elapsed}ms")
  }
}
