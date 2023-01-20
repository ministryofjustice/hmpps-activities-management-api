package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityScheduleCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.Slot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional
import javax.persistence.EntityNotFoundException

class ActivityScheduleCreationServiceTest {

  private val repository: ActivityRepository = mock()
  private val prisonApiClient: PrisonApiClient = mock()
  private val service = ActivityScheduleCreationService(repository, prisonApiClient)
  private val location = Location(
    locationId = 1,
    locationType = "type",
    description = "description",
    agencyId = "MDI",
  )

  @BeforeEach
  fun before() {
    whenever(prisonApiClient.getLocation(1)).thenReturn(Mono.just(location))
  }

  @Test
  fun `can add a schedule with a morning slot to an activity`() {
    val activity = activityEntity(activityId = 1, prisonCode = "MDI", noSchedules = true)

    whenever(repository.findById(1)).thenReturn(Optional.of(activity))
    whenever(repository.saveAndFlush(activity)).thenReturn(activity)
    whenever(prisonApiClient.getLocation(1)).thenReturn(Mono.just(location))

    val request = ActivityScheduleCreateRequest(
      description = "Test schedule",
      startDate = activity.startDate,
      locationId = 1,
      capacity = 10,
      slots = listOf(Slot("AM", monday = true))
    )

    with(service.createSchedule(1, request)) {
      assertThat(slots).containsExactly(
        ActivityScheduleSlot(
          id = -1,
          startTime = LocalTime.of(9, 0),
          endTime = LocalTime.of(10, 0),
          daysOfWeek = listOf("Mon")
        )
      )
    }
  }

  @Test
  fun `fails to add schedule when activity not found`() {
    whenever(repository.findById(any())).thenReturn(Optional.empty())

    val request = ActivityScheduleCreateRequest(
      description = "Test schedule",
      startDate = LocalDate.now(),
      locationId = 1,
      capacity = 10,
      slots = listOf(Slot("AM", monday = true))
    )

    assertThatThrownBy {
      service.createSchedule(1, request)
    }.isInstanceOf(EntityNotFoundException::class.java)
  }

  @Test
  fun `fails to add schedule when prison do not match with the activity and location supplied`() {
    whenever(repository.findById(any())).thenReturn(Optional.of(activityEntity(prisonCode = "DOES_NOT_MATCH")))

    val request = ActivityScheduleCreateRequest(
      description = "Test schedule",
      startDate = LocalDate.now(),
      locationId = 1,
      capacity = 10,
      slots = listOf(Slot("AM", monday = true))
    )

    assertThatThrownBy {
      service.createSchedule(1, request)
    }.isInstanceOf(IllegalArgumentException::class.java)
  }
}
