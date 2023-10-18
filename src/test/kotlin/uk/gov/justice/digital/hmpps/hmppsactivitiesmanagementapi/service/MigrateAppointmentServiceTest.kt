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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.deleteMigratedAppointmentReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentCancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSeriesRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSeriesSpecification
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.DELETE_MIGRATED_APPOINTMENT_CANCELLATION_REASON_ID
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

class MigrateAppointmentServiceTest {
  private val appointmentSeriesSpecification: AppointmentSeriesSpecification = spy()
  private val appointmentSeriesRepository: AppointmentSeriesRepository = mock()
  private val appointmentCancellationReasonRepository: AppointmentCancellationReasonRepository = mock()

  private val service = MigrateAppointmentService(
    appointmentSeriesSpecification,
    appointmentSeriesRepository,
    appointmentCancellationReasonRepository,
    TransactionHandler(),
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
    whenever(appointmentCancellationReasonRepository.findById(DELETE_MIGRATED_APPOINTMENT_CANCELLATION_REASON_ID))
      .thenReturn(Optional.of(deleteMigratedAppointmentReason()))
    whenever(appointmentSeriesRepository.findAll(any<Specification<AppointmentSeries>>()))
      .thenReturn(listOf(appointmentSeries))
  }

  @Test
  fun `deleteMigratedAppointments deletes migrated appointments matching prison code and start date`() {
    val appointment = appointmentSeries.appointments().single()

    service.deleteMigratedAppointments(prisonCode, startDate)

    verify(appointmentSeriesSpecification).prisonCodeEquals(prisonCode)
    verify(appointmentSeriesSpecification).startDateGreaterThanOrEquals(startDate)
    verify(appointmentSeriesSpecification).isMigrated()
    verifyNoMoreInteractions(appointmentSeriesSpecification)

    with(appointment) {
      cancelledTime isCloseTo LocalDateTime.now()
      cancellationReason isEqualTo deleteMigratedAppointmentReason()
      cancelledBy isEqualTo "DELETE_MIGRATED_APPOINTMENT_SERVICE"
      isDeleted isBool true
    }
  }

  @Test
  fun `deleteMigratedAppointments deletes migrated appointments matching prison code, start date and category`() {
    val appointment = appointmentSeries.appointments().single()

    service.deleteMigratedAppointments(prisonCode, startDate, categoryCode)

    verify(appointmentSeriesSpecification).prisonCodeEquals(prisonCode)
    verify(appointmentSeriesSpecification).startDateGreaterThanOrEquals(startDate)
    verify(appointmentSeriesSpecification).isMigrated()
    verify(appointmentSeriesSpecification).categoryCodeEquals(categoryCode)
    verifyNoMoreInteractions(appointmentSeriesSpecification)

    with(appointment) {
      cancelledTime isCloseTo LocalDateTime.now()
      cancellationReason isEqualTo deleteMigratedAppointmentReason()
      cancelledBy isEqualTo "DELETE_MIGRATED_APPOINTMENT_SERVICE"
      isDeleted isBool true
    }
  }
}
