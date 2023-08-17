package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrence
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrenceAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound

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
      JobDefinition(JobType.CREATE_APPOINTMENT_OCCURRENCES) {
        createAppointmentOccurrences(appointmentId, prisonerBookings)
      },
    )
  }

  private fun createAppointmentOccurrences(appointmentId: Long, prisonerBookings: Map<String, String?>) {
    log.info("Creating occurrences for appointment with id $appointmentId")

    val appointment = appointmentRepository.findOrThrowNotFound(appointmentId)

    appointment.scheduleIterator().withIndex().forEach {
      val sequenceNumber = it.index + 1
      val occurrence = appointmentOccurrenceRepository.findByAppointmentAndSequenceNumber(appointment, sequenceNumber)
      if (occurrence == null) {
        log.info("Creating occurrence $sequenceNumber with ${prisonerBookings.size} allocations for appointment with id $appointmentId")
        runCatching {
          appointmentOccurrenceRepository.saveAndFlush(
            AppointmentOccurrence(
              appointment = appointment,
              sequenceNumber = sequenceNumber,
              internalLocationId = appointment.internalLocationId,
              inCell = appointment.inCell,
              startDate = it.value,
              startTime = appointment.startTime,
              endTime = appointment.endTime,
            ).apply {
              prisonerBookings.forEach { prisonerBooking ->
                this.addAllocation(
                  AppointmentOccurrenceAllocation(
                    appointmentOccurrence = this,
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
}
