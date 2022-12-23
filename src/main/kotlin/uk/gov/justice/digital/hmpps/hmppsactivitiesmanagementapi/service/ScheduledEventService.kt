package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transformActivityForPrisonerProjectionToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transformActivityScheduledInstancesToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transformToPrisonerScheduledEvents
import java.time.LocalDate
import javax.persistence.EntityNotFoundException

@Service
class ScheduledEventService(
  private val prisonApiClient: PrisonApiClient,
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val scheduledInstanceService: ScheduledInstanceService,
  private val prisonRegimeService: PrisonRegimeService
) {
  /**
   *  Get the scheduled events for a single prison and prisoner number between two dates.
   *  Court hearings, appointments and visits are from Prison API
   *  Activities are from the Activities database if roll out is true, else from Prison API.
   */
  fun getScheduledEventsByDateRange(
    prisonCode: String,
    prisonerNumber: String,
    dateRange: LocalDateRange
  ) = prisonApiClient.getPrisonerDetails(prisonerNumber).block()
    ?.also {
      if (it.agencyId != prisonCode || it.bookingId == null) {
        throw EntityNotFoundException("Prisoner '$prisonerNumber' not found in prison '$prisonCode'")
      }
    }
    ?.let { prisonerDetail ->
      val prisonRolledOut = rolloutPrisonRepository.findByCode(prisonCode).let { it != null && it.active }
      val eventPriorities = prisonRegimeService.getEventPrioritiesForPrison(prisonCode)
      getScheduledEventCalls(prisonerDetail.bookingId!!, prisonRolledOut, dateRange)
        .map { t ->
          transformToPrisonerScheduledEvents(
            prisonerDetail.bookingId,
            prisonCode,
            prisonerNumber,
            dateRange,
            eventPriorities,
            t.t1,
            t.t2,
            t.t3,
            t.t4
          )
        }.block()
        ?.apply {
          if (prisonRolledOut) {
            activities = transformActivityScheduledInstancesToScheduledEvents(
              prisonerDetail.bookingId,
              prisonerNumber,
              EventType.ACTIVITY.defaultPriority,
              eventPriorities[EventType.ACTIVITY],
              scheduledInstanceService.getActivityScheduleInstancesByDateRange(
                prisonCode,
                prisonerNumber,
                dateRange,
                null
              )
            )
          }
        }
    }

  private fun getScheduledEventCalls(bookingId: Long, prisonRolledOut: Boolean, dateRange: LocalDateRange) =
    Mono.zip(
      prisonApiClient.getScheduledAppointments(bookingId, dateRange),
      prisonApiClient.getScheduledCourtHearings(bookingId, dateRange),
      prisonApiClient.getScheduledVisits(bookingId, dateRange),
      if (!prisonRolledOut) prisonApiClient.getScheduledActivities(bookingId, dateRange) else Mono.just(emptyList())
    )

  /**
   * Get the scheduled events for a list of prisoner numbers, for one date and time slot
   * Court hearings, appointments and visits are from Prison API
   * Activities are from the Activities database if roll out is true, else from Prison API.
   */
  fun getScheduledEventsForOffenderList(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate?,
    timeSlot: TimeSlot?,
  ): PrisonerScheduledEvents? {
    val eventPriorities = prisonRegimeService.getEventPrioritiesForPrison(prisonCode)
    val prisonRolledOut = rolloutPrisonRepository.findByCode(prisonCode).let {
      it != null && it.active
    }
    return getPrisonApiEventCalls(prisonCode, prisonerNumbers, prisonRolledOut, date, timeSlot)
      .map { t ->
        transformToPrisonerScheduledEvents(
          prisonCode,
          prisonerNumbers,
          date,
          eventPriorities,
          t.t1,
          t.t2,
          t.t3,
          t.t4,
        )
      }.block()
      ?.apply {
        if (prisonRolledOut) {
          // Populate the activities list from the Activities database for these prisoners, date and time slot
          activities = transformActivityForPrisonerProjectionToScheduledEvents(
            prisonCode,
            EventType.ACTIVITY.defaultPriority,
            eventPriorities[EventType.ACTIVITY],
            scheduledInstanceService.getScheduledInstancesByPrisonerNumbers(prisonCode, prisonerNumbers, date, timeSlot)
          )
        }
      }
  }

  private fun getPrisonApiEventCalls(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    prisonRolledOut: Boolean,
    date: LocalDate?,
    timeSlot: TimeSlot?
  ) =
    Mono.zip(
      prisonApiClient.getScheduledAppointmentsForPrisonerNumbers(prisonCode, prisonerNumbers, date, timeSlot),
      prisonApiClient.getScheduledCourtEventsForPrisonerNumbers(prisonCode, prisonerNumbers, date, timeSlot),
      prisonApiClient.getScheduledVisitsForPrisonerNumbers(prisonCode, prisonerNumbers, date, timeSlot),
      if (!prisonRolledOut) {
        prisonApiClient.getScheduledActivitiesForPrisonerNumbers(prisonCode, prisonerNumbers, date, timeSlot)
      } else {
        Mono.just(emptyList())
      }
    )
}
