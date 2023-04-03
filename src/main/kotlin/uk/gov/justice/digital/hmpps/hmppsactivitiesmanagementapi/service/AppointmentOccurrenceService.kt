package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrenceAllocation
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

    val updatedIds = mutableListOf<Long>()

    // Category updates are applied at the appointment level
    request.categoryCode?.apply {
      failIfCategoryNotFound(this)
      appointment.categoryCode = this
      appointment.updated = now
      appointment.updatedBy = principal.name

      updatedIds.addAll(appointment.occurrences().flatMap { it.allocations().map { allocation -> allocation.appointmentOccurrenceAllocationId } })
    }

    val occurrencesToUpdate =
      when (request.applyTo) {
        ApplyTo.THIS_AND_ALL_FUTURE_OCCURRENCES -> listOf(appointmentOccurrence).union(
          appointment.occurrences().filter { LocalDateTime.of(it.startDate, it.startTime) > LocalDateTime.of(appointmentOccurrence.startDate, appointmentOccurrence.startTime) },
        )
        ApplyTo.ALL_FUTURE_OCCURRENCES -> appointment.occurrences().filter { LocalDateTime.of(it.startDate, it.startTime) > now }
        else -> listOf(appointmentOccurrence)
      }

    // Changing the start date of a repeat appointment changes the start date of all affected appointments based on the original schedule using the new start date
    request.startDate?.apply {
      val scheduleIterator = appointment.scheduleIterator().apply { startDate = request.startDate }
      occurrencesToUpdate.sortedBy { it.sequenceNumber }.forEach { it.startDate = scheduleIterator.next() }
    }

    occurrencesToUpdate.forEach { occurrence ->
      occurrence.updated = now
      occurrence.updatedBy = principal.name

      if (request.inCell == true) {
        occurrence.internalLocationId = null
        occurrence.inCell = true
      } else {
        request.internalLocationId?.apply {
          failIfLocationNotFound(this, appointment.prisonCode)
          occurrence.internalLocationId = this
          occurrence.inCell = false
        }
      }

      request.startTime?.apply { occurrence.startTime = this }

      request.endTime?.apply { occurrence.endTime = this }

      request.comment?.apply { occurrence.comment = this }

      request.prisonerNumbers?.apply {
        val prisonerMap = prisonerSearchApiClient.findByPrisonerNumbers(this).block()!!
          .filter { prisoner -> prisoner.prisonId == appointment.prisonCode }
          .associateBy { prisoner -> prisoner.prisonerNumber }

        failIfMissingPrisoners(this, prisonerMap)

        occurrence.allocations()
          .filter { allocation -> !prisonerMap.containsKey(allocation.prisonerNumber) }
          .forEach { allocation -> occurrence.removeAllocation(allocation) }

        val prisonerAllocationMap = occurrence.allocations().associateBy { allocation -> allocation.prisonerNumber }
        val newPrisoners = prisonerMap.filter { !prisonerAllocationMap.containsKey(it.key) }.values

        newPrisoners.forEach { prisoner ->
          occurrence.addAllocation(
            AppointmentOccurrenceAllocation(
              appointmentOccurrence = occurrence,
              prisonerNumber = prisoner.prisonerNumber,
              bookingId = prisoner.bookingId!!.toLong(),
            ),
          )
        }
      }

      occurrence.allocations().forEach { if (!updatedIds.contains(it.appointmentOccurrenceAllocationId)) updatedIds.add(it.appointmentOccurrenceAllocationId) }
    }

    val updatedAppointment = appointmentRepository.saveAndFlush(appointment)

    updatedIds.sortedBy { it }.forEach {
      runCatching {
        outboundEventsService.send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, it)
      }.onFailure {
        log.error(
          "Failed to send appointment instance creation event for appointment instance id $it",
          it,
        )
      }
    }

    return updatedAppointment.toModel()
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
