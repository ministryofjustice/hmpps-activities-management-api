package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentCancelDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSeriesRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSeriesSpecification
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.DELETE_MIGRATED_APPOINTMENT_CANCELLATION_REASON_ID
import java.time.LocalDate

class MigrateAppointmentServiceTest {
  private val appointmentSeriesSpecification: AppointmentSeriesSpecification = spy()
  private val appointmentSeriesRepository: AppointmentSeriesRepository = mock()
  private val appointmentCancelDomainService: AppointmentCancelDomainService = mock()

  private val service = MigrateAppointmentService(
    appointmentSeriesSpecification,
    appointmentSeriesRepository,
    appointmentCancelDomainService,
  )

  @Nested
  @DisplayName("delete migrated appointments")
  inner class DeleteMigratedAppointments {
    private val prisonCode: String = "MDI"
    private val startDate = LocalDate.now()
    private val categoryCode = "CHAP"

    @Test
    fun `finds migrated appointments matching prison code and start date`() {
      service.deleteMigratedAppointments(prisonCode, startDate)

      verify(appointmentSeriesSpecification).prisonCodeEquals(prisonCode)
      verify(appointmentSeriesSpecification).startDateGreaterThanOrEquals(startDate)
      verify(appointmentSeriesSpecification).isMigrated()
      verifyNoMoreInteractions(appointmentSeriesSpecification)
    }

    @Test
    fun `finds migrated appointments matching prison code, start date and category`() {
      service.deleteMigratedAppointments(prisonCode, startDate, categoryCode)

      verify(appointmentSeriesSpecification).prisonCodeEquals(prisonCode)
      verify(appointmentSeriesSpecification).startDateGreaterThanOrEquals(startDate)
      verify(appointmentSeriesSpecification).isMigrated()
      verify(appointmentSeriesSpecification).categoryCodeEquals(categoryCode)
      verifyNoMoreInteractions(appointmentSeriesSpecification)
    }

    @Test
    fun `deletes migrated appointments`() {
      val appointmentSeries = appointmentSeriesEntity()

      whenever(appointmentSeriesRepository.findAll(any<Specification<AppointmentSeries>>()))
        .thenReturn(listOf(appointmentSeries))

      service.deleteMigratedAppointments(prisonCode, startDate)

      verify(appointmentCancelDomainService).cancelAppointments(
        eq(appointmentSeries),
        eq(appointmentSeries.appointments().first().appointmentId),
        eq(appointmentSeries.appointments().toSet()),
        eq(AppointmentCancelRequest(DELETE_MIGRATED_APPOINTMENT_CANCELLATION_REASON_ID, ApplyTo.THIS_AND_ALL_FUTURE_APPOINTMENTS)),
        any(),
        eq("DELETE_MIGRATED_APPOINTMENT_SERVICE"),
        eq(1),
        eq(1),
        any(),
        eq(false),
        eq(true),
      )
    }

    @Test
    fun `does not delete migrated appointments before start date`() {
      val appointmentSeries = appointmentSeriesEntity(startDate = startDate.minusDays(1))

      whenever(appointmentSeriesRepository.findAll(any<Specification<AppointmentSeries>>()))
        .thenReturn(listOf(appointmentSeries))

      service.deleteMigratedAppointments(prisonCode, startDate)

      verifyNoInteractions(appointmentCancelDomainService)
    }
  }
}
