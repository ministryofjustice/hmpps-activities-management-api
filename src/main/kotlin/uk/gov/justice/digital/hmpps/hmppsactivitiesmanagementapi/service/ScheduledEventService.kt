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
  ): PrisonerScheduledEvents? =
    prisonApiClient.getPrisonerDetails(prisonerNumber).block()
      ?.takeUnless { it.agencyId != prisonCode || it.bookingId == null }
      ?.let { prisonerDetail ->
        val prisonRolledOut = rolloutPrisonRepository.findByCode(prisonCode).let { it != null && it.active }
        getScheduledEventCalls(prisonerDetail.bookingId!!, prisonRolledOut, dateRange)
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
          }.block()
          ?.apply {
            if (prisonRolledOut) {
              activities = transformActivityScheduledInstancesToScheduledEvents(
                prisonerDetail.bookingId,
                prisonerNumber,
                scheduledInstanceService.getActivityScheduleInstancesByDateRange(prisonCode, prisonerNumber, dateRange)
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
}
