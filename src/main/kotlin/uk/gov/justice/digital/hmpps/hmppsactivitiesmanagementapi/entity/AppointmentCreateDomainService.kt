package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentAttendeeRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentCancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSeriesRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.CANCELLED_APPOINTMENT_CANCELLATION_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.TransactionHandler
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeries as AppointmentSeriesModel

@Service
@Transactional
class AppointmentCreateDomainService(
  private val appointmentSeriesRepository: AppointmentSeriesRepository,
  private val appointmentRepository: AppointmentRepository,
  private val appointmentAttendeeRepository: AppointmentAttendeeRepository,
  private val appointmentCancellationReasonRepository: AppointmentCancellationReasonRepository,
  private val transactionHandler: TransactionHandler,
) {
  /**
   * Uses the appointment series as a blueprint to create all the appointments in the series and their attendees.
   * Will only create appointments and attendees not already created making it safe to use for the partial async create process.
   *
   * @param isCancelled specifies whether the appointments should be created in a cancelled state. Can only set to true for migrated appointments
   */
  fun createAppointments(
    appointmentSeries: AppointmentSeries,
    prisonNumberBookingIdMap: Map<String, Long>,
    isCancelled: Boolean = false,
  ): AppointmentSeriesModel {
    require(!isCancelled || appointmentSeries.isMigrated) {
      "Only migrated appointments can be created in a cancelled state"
    }

    val cancelledTime = if (isCancelled) appointmentSeries.updatedTime ?: appointmentSeries.createdTime else null
    val cancellationReason = if (isCancelled) appointmentCancellationReasonRepository.findOrThrowNotFound(CANCELLED_APPOINTMENT_CANCELLATION_REASON_ID) else null
    val cancelledBy = if (isCancelled) appointmentSeries.updatedBy ?: appointmentSeries.createdBy else null

    appointmentSeries.scheduleIterator().withIndex().forEach { indexedStartDate ->
      val sequenceNumber = indexedStartDate.index + 1

      // Retrieve or create appointment in series
      val appointment = appointmentSeries.appointments().singleOrNull { it.sequenceNumber == sequenceNumber }
        ?: transactionHandler.newSpringTransaction {
          appointmentRepository.saveAndFlush(
            appointmentSeries.createAppointment(
              sequenceNumber,
              indexedStartDate.value,
            ).apply {
              this.cancelledTime = cancelledTime
              this.cancellationReason = cancellationReason
              this.cancelledBy = cancelledBy
            },
          )
        }

      // Create any missing attendee records
      val existingPrisonNumbers = appointment.findAttendees(prisonNumberBookingIdMap.keys).map { it.prisonerNumber }
      prisonNumberBookingIdMap.filterNot { existingPrisonNumbers.contains(it.key) }.forEach {
        transactionHandler.newSpringTransaction {
          appointmentAttendeeRepository.saveAndFlush(
            AppointmentAttendee(
              appointment = appointment,
              prisonerNumber = it.key,
              bookingId = it.value,
            ),
          )
        }
        // TODO: publish appointment instance created message post transaction
      }
    }

    return appointmentSeriesRepository.findOrThrowNotFound(appointmentSeries.appointmentSeriesId).toModel()
  }
}

fun AppointmentSeries.createAppointment(sequenceNumber: Int, startDate: LocalDate) =
  Appointment(
    appointmentSeries = this,
    sequenceNumber = sequenceNumber,
    prisonCode = this.prisonCode,
    categoryCode = this.categoryCode,
    customName = this.customName,
    appointmentTier = this.appointmentTier,
    appointmentHost = this.appointmentHost,
    internalLocationId = this.internalLocationId,
    customLocation = this.customLocation,
    inCell = this.inCell,
    onWing = this.onWing,
    offWing = this.offWing,
    startDate = startDate,
    startTime = this.startTime,
    endTime = this.endTime,
    unlockNotes = this.unlockNotes,
    extraInformation = this.extraInformation,
    createdTime = this.createdTime,
    createdBy = this.createdBy,
    updatedTime = this.updatedTime,
    updatedBy = this.updatedBy,
  )