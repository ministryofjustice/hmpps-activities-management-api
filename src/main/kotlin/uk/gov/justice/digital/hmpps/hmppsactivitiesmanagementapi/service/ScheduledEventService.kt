package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
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

  fun getScheduledEventsByDateRange(
    prisonCode: String,
    prisonerNumber: String,
    dateRange: LocalDateRange
  ) = prisonApiClient.getPrisonerDetails(prisonerNumber).block()
    ?.also { if (it.agencyId != prisonCode || it.bookingId == null) throw EntityNotFoundException("Prisoner '$prisonerNumber' not found in prison '$prisonCode'") }
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

  fun getScheduledEventsForOffenderList(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate?,
    timeSlot: TimeSlot?
  ): PrisonerScheduledEvents? {
    val eventPriorities = prisonRegimeService.getEventPrioritiesForPrison(prisonCode)
    return getScheduledEventCalls(prisonCode, prisonerNumbers, date, timeSlot)
      .map { t ->
        transformToPrisonerScheduledEvents(
          prisonCode,
          prisonerNumbers,
          date,
          eventPriorities,
          t.t1,
          t.t2,
          t.t3,
        )
      }.block()
  }

  private fun getScheduledEventCalls(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate?,
    timeSlot: TimeSlot?
  ) =
    Mono.zip(
      prisonApiClient.getScheduledAppointmentsForPrisonerNumbers(prisonCode, prisonerNumbers, date, timeSlot),
      prisonApiClient.getScheduledCourtEventsForPrisonerNumbers(prisonCode, prisonerNumbers, date, timeSlot),
      prisonApiClient.getScheduledVisitsForPrisonerNumbers(prisonCode, prisonerNumbers, date, timeSlot),
    )
}
