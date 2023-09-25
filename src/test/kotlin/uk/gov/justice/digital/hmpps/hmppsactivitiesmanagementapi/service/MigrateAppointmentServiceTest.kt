package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSeriesRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSeriesSpecification
import java.time.LocalDate

class MigrateAppointmentServiceTest {
  private val appointmentSeriesSpecification: AppointmentSeriesSpecification = spy()
  private val appointmentSeriesRepository: AppointmentSeriesRepository = mock()

  private val service = MigrateAppointmentService(
    appointmentSeriesSpecification,
    appointmentSeriesRepository,
  )

  private val prisonCode: String = "MDI"
  private val startDate = LocalDate.now()
  private val categoryCode = "CHAP"
  private val appointmentSeries = appointmentSeriesEntity(
    prisonCode = prisonCode,
    categoryCode = categoryCode,
    startDate = startDate,
    isMigrated = true,
  )

  @BeforeEach
  fun setUp() {
    whenever(appointmentSeriesRepository.findAll(any<Specification<AppointmentSeries>>()))
      .thenReturn(listOf(appointmentSeries))
  }

  @Test
  fun `deleteMigratedAppointments deletes migrated appointments matching prison code and start date`() {
    service.deleteMigratedAppointments(prisonCode, startDate)

    verify(appointmentSeriesSpecification).prisonCodeEquals(prisonCode)
    verify(appointmentSeriesSpecification).startDateGreaterThanOrEquals(startDate)
    verify(appointmentSeriesSpecification).isMigrated()
    verifyNoMoreInteractions(appointmentSeriesSpecification)

    verify(appointmentSeriesRepository).deleteAll(listOf(appointmentSeries))
  }

  @Test
  fun `deleteMigratedAppointments deletes migrated appointments matching prison code, start date and category`() {
    service.deleteMigratedAppointments(prisonCode, startDate, categoryCode)

    verify(appointmentSeriesSpecification).prisonCodeEquals(prisonCode)
    verify(appointmentSeriesSpecification).startDateGreaterThanOrEquals(startDate)
    verify(appointmentSeriesSpecification).isMigrated()
    verify(appointmentSeriesSpecification).categoryCodeEquals(categoryCode)
    verifyNoMoreInteractions(appointmentSeriesSpecification)

    verify(appointmentSeriesRepository).deleteAll(listOf(appointmentSeries))
  }
}
