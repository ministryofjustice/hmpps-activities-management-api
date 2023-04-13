package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrence
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrenceAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import java.security.Principal
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment as AppointmentModel

@Service
class AppointmentOccurrenceService(
  private val appointmentRepository: AppointmentRepository,
  private val appointmentOccurrenceRepository: AppointmentOccurrenceRepository,
  private val referenceCodeService: ReferenceCodeService,
  private val locationService: LocationService,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val outboundEventsService: OutboundEventsService,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun updateAppointmentOccurrence(appointmentOccurrenceId: Long, request: AppointmentOccurrenceUpdateRequest, principal: Principal): AppointmentModel {
    val appointmentOccurrence = appointmentOccurrenceRepository.findOrThrowNotFound(appointmentOccurrenceId)
    val appointment = appointmentOccurrence.appointment

    val now = LocalDateTime.now()

    if (LocalDateTime.of(appointmentOccurrence.startDate, appointmentOccurrence.startTime) < now) {
      throw IllegalArgumentException("Cannot update a past appointment occurrence")
    }

    val occurrencesToUpdate =
      when (request.applyTo) {
        ApplyTo.THIS_AND_ALL_FUTURE_OCCURRENCES -> listOf(appointmentOccurrence).union(
          appointment.occurrences().filter { LocalDateTime.of(it.startDate, it.startTime) > LocalDateTime.of(appointmentOccurrence.startDate, appointmentOccurrence.startTime) },
        )
        ApplyTo.ALL_FUTURE_OCCURRENCES -> appointment.occurrences().filter { LocalDateTime.of(it.startDate, it.startTime) > now }
        else -> listOf(appointmentOccurrence)
      }

    val updatedIds = mutableListOf<Long>()

    applyCategoryCodeUpdate(request, appointment, now, principal.name, updatedIds)
    applyStartDateUpdate(request, appointment, occurrencesToUpdate, now, principal.name, updatedIds)
    applyInternalLocationUpdate(request, appointment, occurrencesToUpdate, now, principal.name, updatedIds)
    applyStartEndTimeUpdate(request, occurrencesToUpdate, now, principal.name, updatedIds)
    applyCommentUpdate(request, occurrencesToUpdate, now, principal.name, updatedIds)
    applyAllocationUpdate(request, appointment, occurrencesToUpdate, now, principal.name, updatedIds)

    val updatedAppointment = appointmentRepository.saveAndFlush(appointment)

    updatedIds.sortedBy { it }.forEach {
      runCatching {
        outboundEventsService.send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, it)
      }.onFailure {
        log.error(
          "Failed to send appointment instance updated event for appointment instance id $it",
          it,
        )
      }
    }

    return updatedAppointment.toModel()
  }

  private fun applyCategoryCodeUpdate(
    request: AppointmentOccurrenceUpdateRequest,
    appointment: Appointment,
    updated: LocalDateTime,
    updatedBy: String,
    updatedIds: MutableList<Long>,
  ) {
    request.categoryCode?.apply {
      failIfCategoryNotFound(this)

      // Category updates are applied at the appointment level
      appointment.categoryCode = this

      // Mark appointment and occurrences as updated and add associated ids for event publishing
      appointment.updated = updated
      appointment.updatedBy = updatedBy
      appointment.occurrences().forEach { it.markAsUpdated(updated, updatedBy, updatedIds) }
    }
  }

  private fun applyInternalLocationUpdate(
    request: AppointmentOccurrenceUpdateRequest,
    appointment: Appointment,
    occurrencesToUpdate: Collection<AppointmentOccurrence>,
    updated: LocalDateTime,
    updatedBy: String,
    updatedIds: MutableList<Long>,
  ) {
    occurrencesToUpdate.forEach {
      if (request.inCell == true) {
        it.internalLocationId = null
        it.inCell = true
        it.markAsUpdated(updated, updatedBy, updatedIds)
      } else {
        request.internalLocationId?.apply {
          failIfLocationNotFound(this, appointment.prisonCode)
          it.internalLocationId = this
          it.inCell = false
          it.markAsUpdated(updated, updatedBy, updatedIds)
        }
      }
    }
  }

  private fun applyStartDateUpdate(
    request: AppointmentOccurrenceUpdateRequest,
    appointment: Appointment,
    occurrencesToUpdate: Collection<AppointmentOccurrence>,
    updated: LocalDateTime,
    updatedBy: String,
    updatedIds: MutableList<Long>,
  ) {
    // Changing the start date of a repeat appointment changes the start date of all affected appointments based on the original schedule using the new start date
    request.startDate?.apply {
      val scheduleIterator = appointment.scheduleIterator().apply { startDate = request.startDate }
      occurrencesToUpdate.sortedBy { it.sequenceNumber }.forEach {
        it.startDate = scheduleIterator.next()
        it.markAsUpdated(updated, updatedBy, updatedIds)
      }
    }
  }

  private fun applyStartEndTimeUpdate(
    request: AppointmentOccurrenceUpdateRequest,
    occurrencesToUpdate: Collection<AppointmentOccurrence>,
    updated: LocalDateTime,
    updatedBy: String,
    updatedIds: MutableList<Long>,
  ) {
    occurrencesToUpdate.forEach {
      request.startTime?.apply {
        it.startTime = this
        it.markAsUpdated(updated, updatedBy, updatedIds)
      }

      request.endTime?.apply {
        it.endTime = this
        it.markAsUpdated(updated, updatedBy, updatedIds)
      }
    }
  }

  private fun applyCommentUpdate(
    request: AppointmentOccurrenceUpdateRequest,
    occurrencesToUpdate: Collection<AppointmentOccurrence>,
    updated: LocalDateTime,
    updatedBy: String,
    updatedIds: MutableList<Long>,
  ) {
    occurrencesToUpdate.forEach {
      request.comment?.apply {
        it.comment = this
        it.markAsUpdated(updated, updatedBy, updatedIds)
      }
    }
  }

  private fun applyAllocationUpdate(
    request: AppointmentOccurrenceUpdateRequest,
    appointment: Appointment,
    occurrencesToUpdate: Collection<AppointmentOccurrence>,
    updated: LocalDateTime,
    updatedBy: String,
    updatedIds: MutableList<Long>,
  ) {
    occurrencesToUpdate.forEach {
      request.prisonerNumbers?.apply {
        if (request.prisonerNumbers.size > 1 && appointment.appointmentType == AppointmentType.INDIVIDUAL) {
          throw IllegalArgumentException("Cannot allocate more than one prisoner to an individual appointment occurrence")
        }

        val prisonerMap = prisonerSearchApiClient.findByPrisonerNumbers(this).block()!!
          .filter { prisoner -> prisoner.prisonId == appointment.prisonCode }
          .associateBy { prisoner -> prisoner.prisonerNumber }

        failIfMissingPrisoners(this, prisonerMap)

        it.markAsUpdated(updated, updatedBy, updatedIds)

        it.allocations()
          .filter { allocation -> !prisonerMap.containsKey(allocation.prisonerNumber) }
          .forEach { allocation ->
            it.removeAllocation(allocation)
            // Remove id from updated list as it has been removed
            updatedIds.remove(allocation.appointmentOccurrenceAllocationId)
          }

        val prisonerAllocationMap = it.allocations().associateBy { allocation -> allocation.prisonerNumber }
        val newPrisoners = prisonerMap.filter { !prisonerAllocationMap.containsKey(it.key) }.values

        newPrisoners.forEach { prisoner ->
          it.addAllocation(
            AppointmentOccurrenceAllocation(
              appointmentOccurrence = it,
              prisonerNumber = prisoner.prisonerNumber,
              bookingId = prisoner.bookingId!!.toLong(),
            ),
          )
        }
      }
    }
  }

  private fun AppointmentOccurrence.markAsUpdated(
    updated: LocalDateTime,
    updatedBy: String,
    updatedIds: MutableList<Long>,
  ) {
    this.updated = updated
    this.updatedBy = updatedBy
    this.allocations().forEach {
      if (!updatedIds.contains(it.appointmentOccurrenceAllocationId)) {
        updatedIds.add(it.appointmentOccurrenceAllocationId)
      }
    }
  }

  private fun failIfCategoryNotFound(categoryCode: String) {
    referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT)[categoryCode]
      ?: throw IllegalArgumentException("Appointment Category with code $categoryCode not found or is not active")
  }

  private fun failIfLocationNotFound(internalLocationId: Long, prisonCode: String) {
    locationService.getLocationsForAppointmentsMap(prisonCode)[internalLocationId]
      ?: throw IllegalArgumentException("Appointment location with id $internalLocationId not found in prison '$prisonCode'")
  }

  private fun failIfMissingPrisoners(prisonerNumbers: List<String>, prisonerMap: Map<String, Prisoner>) {
    prisonerNumbers.filter { number -> !prisonerMap.containsKey(number) }.let {
      if (it.any()) throw IllegalArgumentException("Prisoner(s) with prisoner number(s) '${it.joinToString("', '")}' not found, were inactive or are residents of a different prison.")
    }
  }
}
