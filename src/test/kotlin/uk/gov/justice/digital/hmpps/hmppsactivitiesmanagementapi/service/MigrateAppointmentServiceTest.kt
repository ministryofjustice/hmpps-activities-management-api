package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.AdditionalAnswers
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCancelledReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentInstanceEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.foundationTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentSeriesRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentSeriesSpecification
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.AppointmentCancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.DELETE_MIGRATED_APPOINTMENT_CANCELLATION_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.EventTierRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AppointmentCancelDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AppointmentCreateDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.MigrateAppointmentService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.ReferenceCodeService
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment as AppointmentModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentAttendee as AppointmentAttendeeModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeries as AppointmentSeriesModel

class MigrateAppointmentServiceTest {
  private val outboundEventsService: OutboundEventsService = mock()
  private val appointmentRepository: AppointmentRepository = mock()
  private val appointmentSeriesSpecification: AppointmentSeriesSpecification = spy()
  private val appointmentSeriesRepository: AppointmentSeriesRepository = mock()
  private val eventTierRepository: EventTierRepository = mock()
  private val appointmentInstanceRepository: AppointmentInstanceRepository = mock()
  private val appointmentCancellationReasonRepository: AppointmentCancellationReasonRepository = mock()
  private val appointmentCreateDomainService = spy(AppointmentCreateDomainService(mock(), appointmentRepository, appointmentCancellationReasonRepository, TransactionHandler(), outboundEventsService, mock(), mock()))
  private val appointmentCancelDomainService: AppointmentCancelDomainService = mock()
  private val referenceCodeService: ReferenceCodeService = mock()

  private val appointmentCancelledReason = appointmentCancelledReason()

  private val service = MigrateAppointmentService(
    appointmentSeriesSpecification,
    appointmentSeriesRepository,
    appointmentInstanceRepository,
    appointmentCreateDomainService,
    appointmentCancelDomainService,
    appointmentRepository,
    referenceCodeService,
    TransactionHandler(),
  )

  @Nested
  @DisplayName("migrate appointment")
  inner class MigrateAppointment {
    private val appointmentSeriesCaptor = argumentCaptor<AppointmentSeries>()
    private val appointmentCaptor = argumentCaptor<Appointment>()

    @BeforeEach
    fun setUp() {
      whenever(appointmentRepository.saveAndFlush(appointmentCaptor.capture())).thenAnswer(AdditionalAnswers.returnsFirstArg<Appointment>())
      whenever(appointmentSeriesRepository.saveAndFlush(appointmentSeriesCaptor.capture())).thenAnswer(AdditionalAnswers.returnsFirstArg<AppointmentSeries>())
      whenever(eventTierRepository.findById(foundationTier().eventTierId)).thenReturn(Optional.of(foundationTier()))
      whenever(appointmentInstanceRepository.findById(any())).thenReturn(Optional.of(appointmentInstanceEntity()))
      whenever(appointmentCancellationReasonRepository.findById(appointmentCancelledReason.appointmentCancellationReasonId)).thenReturn(
        Optional.of(appointmentCancelledReason),
      )
    }

    @Test
    fun `uses request when creating appointment series`() {
      val request = appointmentMigrateRequest(comment = null)

      service.migrateAppointment(request)

      appointmentSeriesCaptor.firstValue isEqualTo AppointmentSeries(
        appointmentType = AppointmentType.INDIVIDUAL,
        prisonCode = request.prisonCode!!,
        categoryCode = request.categoryCode!!,
        customName = null,
        appointmentTier = null,
        internalLocationId = request.internalLocationId,
        customLocation = null,
        inCell = false,
        onWing = false,
        offWing = true,
        startDate = request.startDate!!,
        startTime = request.startTime!!,
        endTime = request.endTime,
        unlockNotes = null,
        extraInformation = null,
        createdTime = request.created!!,
        createdBy = request.createdBy!!,
        updatedTime = request.updated,
        updatedBy = request.updatedBy,
        isMigrated = true,
      )
    }

    @Test
    fun `sets is migrated to true`() {
      val request = appointmentMigrateRequest()

      service.migrateAppointment(request)

      appointmentSeriesCaptor.firstValue.isMigrated isBool true
    }

    @Test
    fun `calls appointment create domain service`() {
      val request = appointmentMigrateRequest()

      service.migrateAppointment(request)

      verify(appointmentCreateDomainService).createAppointments(appointmentSeriesCaptor.firstValue, mapOf(request.prisonerNumber!! to request.bookingId!!), false)
    }

    @Test
    fun `migrated appointment does not raise sync event`() {
      val request = appointmentMigrateRequest()

      service.migrateAppointment(request)

      verifyNoInteractions(outboundEventsService)
    }

    @Test
    fun `uses attendee id as appointment instance id`() {
      val appointmentCreateDomainService = mock<AppointmentCreateDomainService>()
      val service = MigrateAppointmentService(
        mock(),
        appointmentSeriesRepository,
        appointmentInstanceRepository,
        appointmentCreateDomainService,
        mock(),
        appointmentRepository,
        referenceCodeService,
        TransactionHandler(),
      )

      val appointmentSeriesModel = mock<AppointmentSeriesModel>()
      val appointmentModel = mock<AppointmentModel>()
      whenever(appointmentSeriesModel.appointments).thenReturn(listOf(appointmentModel))
      val appointmentAttendeeModel = mock<AppointmentAttendeeModel>()
      whenever(appointmentModel.attendees).thenReturn(listOf(appointmentAttendeeModel))
      whenever(appointmentAttendeeModel.id).thenReturn(123)
      whenever(appointmentCreateDomainService.createAppointments(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(appointmentSeriesModel)

      val request = appointmentMigrateRequest()

      service.migrateAppointment(request)

      verify(appointmentInstanceRepository).findById(123)
    }

    @Test
    fun `null comment`() {
      val request = appointmentMigrateRequest(comment = null)

      service.migrateAppointment(request)

      with(appointmentSeriesCaptor.firstValue) {
        customName isEqualTo null
        extraInformation isEqualTo null
      }
    }

    @Test
    fun `null end time is replaced by start time plus one hour`() {
      val request = appointmentMigrateRequest(endTime = null)

      service.migrateAppointment(request)

      with(appointmentSeriesCaptor.firstValue) {
        endTime isEqualTo startTime.plusHours(1)
      }
    }

    @Test
    fun `empty comment`() {
      val request = appointmentMigrateRequest(comment = "")

      service.migrateAppointment(request)

      with(appointmentSeriesCaptor.firstValue) {
        customName isEqualTo null
        extraInformation isEqualTo null
      }
    }

    @Test
    fun `whitespace only comment`() {
      val request = appointmentMigrateRequest(comment = "    ")

      service.migrateAppointment(request)

      with(appointmentSeriesCaptor.firstValue) {
        customName isEqualTo null
        extraInformation isEqualTo null
      }
    }

    @Test
    fun `whitespace start and end comment`() {
      val request = appointmentMigrateRequest(comment = "   First 40 characters will become the appointments custom name but the full comment will go to extra information.  ")

      service.migrateAppointment(request)

      with(appointmentSeriesCaptor.firstValue) {
        customName isEqualTo "First 40 characters will become the appo"
        extraInformation isEqualTo request.comment!!.trim()
      }
    }

    @ParameterizedTest
    @ValueSource(strings = ["Appointments comment", "Appointments comment which is 41 characts"])
    fun `characters comment is copied to customName with first 40 chars and extra information the whole comment`(requestComment: String) {
      val request = appointmentMigrateRequest(comment = requestComment)

      service.migrateAppointment(request)

      with(appointmentSeriesCaptor.firstValue) {
        customName isEqualTo requestComment.take(40)
        extraInformation isEqualTo requestComment
      }
    }

    @ParameterizedTest
    @ValueSource(strings = ["VLLA", "VLB", "VLOO", "VLPA", "VLPM"])
    fun `custom name is empty for BVLS categoryCodes`(categoryCode: String) {
      val request = appointmentMigrateRequest(comment = "appointment comment", categoryCode = categoryCode)

      service.migrateAppointment(request)

      with(appointmentSeriesCaptor.firstValue) {
        customName isEqualTo null
        extraInformation isEqualTo "appointment comment"
      }
    }

    @Test
    fun `over 40 characters comment`() {
      val request = appointmentMigrateRequest(comment = "First 40 characters will become the appointments custom name but the full comment will go to extra information.")

      service.migrateAppointment(request)

      with(appointmentSeriesCaptor.firstValue) {
        customName isEqualTo "First 40 characters will become the appo"
        extraInformation isEqualTo request.comment
      }
    }

    @Test
    fun `specified updated and updatedBy`() {
      val request = appointmentMigrateRequest(
        updatedTime = LocalDateTime.of(2022, 10, 23, 10, 30),
        updatedBy = "DPS.USER",
      )

      service.migrateAppointment(request)

      with(appointmentSeriesCaptor.firstValue) {
        updatedTime isEqualTo request.updated
        updatedBy isEqualTo request.updatedBy
      }
    }

    @Test
    fun `isCancelled defaults to false`() {
      val request = appointmentMigrateRequest(isCancelled = null)

      service.migrateAppointment(request)

      verify(appointmentCreateDomainService).createAppointments(appointmentSeriesCaptor.firstValue, mapOf(request.prisonerNumber!! to request.bookingId!!), false)
    }

    @Test
    fun `isCancelled = true`() {
      val request = appointmentMigrateRequest(isCancelled = true)

      service.migrateAppointment(request)

      verify(appointmentCreateDomainService).createAppointments(appointmentSeriesCaptor.firstValue, mapOf(request.prisonerNumber!! to request.bookingId!!), false, true)
    }
  }

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
