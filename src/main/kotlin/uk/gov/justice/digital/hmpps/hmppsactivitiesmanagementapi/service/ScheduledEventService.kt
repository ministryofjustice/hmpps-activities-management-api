package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transformActivityScheduledInstancesToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transformToPrisonerScheduledEvents

@Service
class ScheduledEventService(
  private val prisonApiClient: PrisonApiClient,
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val scheduledInstanceService: ScheduledInstanceService
) {

  fun getScheduledEventsByDateRange(
    prisonCode: String,
    prisonerNumber: String,
    dateRange: LocalDateRange
  ): PrisonerScheduledEvents? {

    val prisonerDetail = prisonApiClient.getPrisonerDetails(prisonerNumber).block()
    if (prisonerDetail == null || prisonerDetail.agencyId != prisonCode || prisonerDetail.bookingId == null) {
      return null
    }

    val isPrisonRolledOut = rolloutPrisonRepository.findByCode(prisonCode).let {
      it != null && it.active
    }

    val prisonApiCalls = Mono.zip(
      prisonApiClient.getScheduledAppointments(prisonerDetail.bookingId, dateRange),
      prisonApiClient.getScheduledCourtHearings(prisonerDetail.bookingId, dateRange),
      prisonApiClient.getScheduledVisits(prisonerDetail.bookingId, dateRange),
      if (!isPrisonRolledOut) prisonApiClient.getScheduledActivities(
        prisonerDetail.bookingId,
        dateRange
      ) else Mono.just(emptyList())
    )
      .map { t ->
        transformToPrisonerScheduledEvents(
          prisonerDetail.bookingId,
          prisonCode,
          prisonerNumber,
          dateRange,
          t.t1,
          t.t2,
          t.t3,
          t.t4
        )
      }
    val events = prisonApiCalls.block()
    if (isPrisonRolledOut) {
      events!!.activities = transformActivityScheduledInstancesToScheduledEvents(
        prisonerDetail.bookingId,
        prisonerNumber,
        scheduledInstanceService.getActivityScheduleInstancesByDateRange(prisonCode, prisonerNumber, dateRange)
      )
    }
    return events
  }
}
