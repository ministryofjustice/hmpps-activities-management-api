package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.CurrentIncentive
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.IncentiveLevel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceReasons
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonPayBandsLowMediumHigh
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ScheduleInstanceCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ScheduledInstanceServiceTest {

  private val repository: ScheduledInstanceRepository = mock()
  private val attendanceReasonRepository: AttendanceReasonRepository = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val service = ScheduledInstanceService(repository, attendanceReasonRepository, prisonerSearchApiClient)

  @Nested
  @DisplayName("getActivityScheduleInstanceById")
  inner class GetActivityScheduleInstanceById {

    @Test
    fun `success`() {
      whenever(repository.findById(1)).thenReturn(
        Optional.of(
          ScheduledInstanceFixture.instance(
            id = 1,
            locationId = 22,
          ),
        ),
      )
      assertThat(service.getActivityScheduleInstanceById(1)).isInstanceOf(ActivityScheduleInstance::class.java)
    }

    @Test
    fun `not found`() {
      whenever(repository.findById(1)).thenReturn(Optional.empty())
      assertThatThrownBy { service.getActivityScheduleInstanceById(-1) }
        .isInstanceOf(EntityNotFoundException::class.java)
        .hasMessage("Scheduled Instance -1 not found")
    }
  }

  @Nested
  @DisplayName("getActivityScheduleInstancesByDateRange")
  inner class GetActivityScheduleInstancesByDateRange {
    @Test
    fun `success`() {
      val prisonCode = "MDI"
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 11, 5)
      val dateRange = LocalDateRange(startDate, endDate)

      whenever(repository.getActivityScheduleInstancesByPrisonCodeAndDateRange(prisonCode, startDate, endDate))
        .thenReturn(listOf(ScheduledInstanceFixture.instance(id = 1, locationId = 22)))

      val result = service.getActivityScheduleInstancesByDateRange(prisonCode, dateRange, null)

      assertThat(result).hasSize(1)
    }

    @Test
    fun `filtered by time slot`() {
      val prisonCode = "MDI"
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 11, 5)
      val dateRange = LocalDateRange(startDate, endDate)

      whenever(repository.getActivityScheduleInstancesByPrisonCodeAndDateRange(prisonCode, startDate, endDate))
        .thenReturn(listOf(ScheduledInstanceFixture.instance(id = 1, locationId = 22)))

      var result = service.getActivityScheduleInstancesByDateRange(prisonCode, dateRange, TimeSlot.PM)
      assertThat(result).hasSize(1)

      result = service.getActivityScheduleInstancesByDateRange(prisonCode, dateRange, TimeSlot.AM)
      assertThat(result).isEmpty()

      result = service.getActivityScheduleInstancesByDateRange(prisonCode, dateRange, TimeSlot.ED)
      assertThat(result).isEmpty()
    }
  }

  @Nested
  @DisplayName("uncancelScheduledInstance")
  inner class UncancelScheduledInstance {

    @Test
    fun `success`() {
      val instance = mock<ScheduledInstance>()
      whenever(repository.findById(1)).thenReturn(
        Optional.of(instance),
      )

      service.uncancelScheduledInstance(1)

      verify(instance).uncancel()
      verify(repository).save(instance)
    }

    @Test
    fun `scheduled event does not exist`() {
      whenever(repository.findById(1)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.uncancelScheduledInstance(1)
      }

      assertThat(exception.message).isEqualTo("Scheduled Instance 1 not found")
    }
  }

  @Nested
  @DisplayName("cancelScheduledInstance")
  inner class CancelScheduledInstance {
    @Test
    fun `success`() {
      val activity = activityEntity(timestamp = LocalDateTime.now())
      val schedule = activity.schedules().first()
      val instance = activity.schedules().first().instances().first()

      activity.addPay(
        incentiveNomisCode = "STD",
        incentiveLevel = "Standard",
        payBand = prisonPayBandsLowMediumHigh()[1],
        rate = 50,
        pieceRate = 60,
        pieceRateItems = 70,
      )
      schedule.apply {
        this.allocatePrisoner(
          prisonerNumber = "A1234AB".toPrisonerNumber(),
          bookingId = 10002,
          payBand = prisonPayBandsLowMediumHigh()[1],
          allocatedBy = "Mr Blogs",
        )
      }
      instance.apply {
        this.attendances.add(
          Attendance(
            attendanceId = 2,
            scheduledInstance = this,
            prisonerNumber = "A1234AB",
          ),
        )
      }

      whenever(repository.findById(1)).thenReturn(Optional.of(instance))

      whenever(attendanceReasonRepository.findByCode(AttendanceReasonEnum.CANCELLED)).thenReturn(attendanceReasons()["CANCELLED"])

      whenever(prisonerSearchApiClient.findByPrisonerNumbers(listOf("A1234AA", "A1234AB"))).thenReturn(
        Mono.just(
          listOf(
            PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A1234AA"),
            PrisonerSearchPrisonerFixture.instance(
              prisonerNumber = "A1234AB",
              currentIncentive = CurrentIncentive(
                level = IncentiveLevel("Standard", "STD"),
                dateTime = "2020-07-20T10:36:53",
                nextReviewDate = LocalDate.of(2021, 7, 20),
              ),
            ),
          ),
        ),
      )

      service.cancelScheduledInstance(1, ScheduleInstanceCancelRequest("Staff unavailable", "USER1", "Resume tomorrow"))

      assertThat(instance.cancelled).isTrue
      assertThat(instance.cancelledTime).isNotNull
      assertThat(instance.cancelledBy).isEqualTo("USER1")
      assertThat(instance.comment).isEqualTo("Resume tomorrow")

      with(instance.attendances.find { it.prisonerNumber == "A1234AA" }!!) {
        assertThat(status).isEqualTo(AttendanceStatus.COMPLETED)
        assertThat(attendanceReason?.code).isEqualTo(AttendanceReasonEnum.CANCELLED)
        assertThat(comment).isEqualTo("Staff unavailable")
        assertThat(recordedBy).isEqualTo("USER1")
        assertThat(recordedTime).isNotNull
      }

      with(instance.attendances.find { it.prisonerNumber == "A1234AB" }!!) {
        assertThat(status).isEqualTo(AttendanceStatus.COMPLETED)
        assertThat(attendanceReason?.code).isEqualTo(AttendanceReasonEnum.CANCELLED)
        assertThat(comment).isEqualTo("Staff unavailable")
        assertThat(recordedBy).isEqualTo("USER1")
        assertThat(recordedTime).isNotNull
      }
    }
  }
}
