package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.AdditionalAnswers
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentAttendee
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSet
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSetCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSetDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSetEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.foundationTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.userDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentSetCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSetAppointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSetCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSetRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventOrganiserRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventTierRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.CUSTOM_NAME_LENGTH_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.HAS_CUSTOM_NAME_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.INTERNAL_LOCATION_DESCRIPTION_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.INTERNAL_LOCATION_ID_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.CaseloadAccessException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.addCaseloadIdToRequestHeader
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.clearCaseloadIdFromRequestHeader
import java.security.Principal
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional

class AppointmentSetServiceTest {
  private val appointmentSetRepository: AppointmentSetRepository = mock()
  private val eventTierRepository: EventTierRepository = mock()
  private val eventHostRepository: EventOrganiserRepository = mock()
  private val referenceCodeService: ReferenceCodeService = mock()
  private val locationService: LocationService = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val prisonApiClient: PrisonApiClient = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val auditService: AuditService = mock()

  private val service = AppointmentSetService(
    appointmentSetRepository,
    eventTierRepository,
    eventHostRepository,
    referenceCodeService,
    locationService,
    prisonerSearchApiClient,
    prisonApiClient,
    TransactionHandler(),
    outboundEventsService,
    telemetryClient,
    auditService,
  )

  @BeforeEach
  fun setUp() {
    addCaseloadIdToRequestHeader("TPR")
  }

  @AfterEach
  fun tearDown() {
    clearCaseloadIdFromRequestHeader()
  }

  @Test
  fun `getAppointmentSetById returns mapped appointment details for known appointment set id`() {
    val entity = appointmentSetEntity()
    whenever(appointmentSetRepository.findById(entity.appointmentSetId)).thenReturn(Optional.of(entity))
    assertThat(service.getAppointmentSetById(1)).isEqualTo(entity.toModel())
  }

  @Test
  fun `getAppointmentSetById throws entity not found exception for unknown appointment set id`() {
    assertThatThrownBy { service.getAppointmentSetById(-1) }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Appointment Set -1 not found")
  }

  @Test
  fun `getAppointmentSetById throws caseload access exception when caseload id header is different`() {
    addCaseloadIdToRequestHeader("WRONG")
    val entity = appointmentSetEntity()
    whenever(appointmentSetRepository.findById(entity.appointmentSetId)).thenReturn(Optional.of(entity))
    assertThatThrownBy { service.getAppointmentSetById(entity.appointmentSetId) }.isInstanceOf(CaseloadAccessException::class.java)
  }

  @Test
  fun `getAppointmentSetDetailsById returns mapped appointment details for known appointment set id`() {
    val entity = appointmentSetEntity()
    whenever(appointmentSetRepository.findById(entity.appointmentSetId)).thenReturn(Optional.of(entity))
    whenever(referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY))
      .thenReturn(mapOf(entity.categoryCode to appointmentCategoryReferenceCode(entity.categoryCode)))
    whenever(locationService.getLocationsForAppointmentsMap(entity.prisonCode))
      .thenReturn(mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR")))
    whenever(prisonApiClient.getUserDetailsList(entity.usernames())).thenReturn(
      listOf(
        userDetail(1, "CREATE.USER", "CREATE", "USER"),
        userDetail(2, "UPDATE.USER", "UPDATE", "USER"),
      ),
    )
    whenever(prisonerSearchApiClient.findByPrisonerNumbersMap(entity.prisonerNumbers())).thenReturn(
      mapOf(
        "A1234BC" to PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = "A1234BC",
          bookingId = 456,
          firstName = "TEST01",
          lastName = "PRISONER01",
          prisonId = "TPR",
          cellLocation = "1-2-3",
        ),
        "B2345CD" to PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = "B2345CD",
          bookingId = 457,
          firstName = "TEST02",
          lastName = "PRISONER02",
          prisonId = "TPR",
          cellLocation = "1-2-4",
        ),
        "C3456DE" to PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = "C3456DE",
          bookingId = 458,
          firstName = "TEST03",
          lastName = "PRISONER03",
          prisonId = "TPR",
          cellLocation = "1-2-5",
        ),
      ),
    )
    assertThat(service.getAppointmentSetDetailsById(1)).isEqualTo(
      appointmentSetDetails(
        createdTime = entity.createdTime,
      ),
    )
  }

  @Test
  fun `getAppointmentSetDetailsById throws entity not found exception for unknown appointment set id`() {
    assertThatThrownBy { service.getAppointmentSetDetailsById(-1) }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Appointment Set -1 not found")
  }

  @Test
  fun `getAppointmentSetDetailsById throws caseload access exception when caseload id header is different`() {
    addCaseloadIdToRequestHeader("WRONG")
    val entity = appointmentSetEntity()
    whenever(appointmentSetRepository.findById(entity.appointmentSetId)).thenReturn(Optional.of(entity))
    assertThatThrownBy { service.getAppointmentSetDetailsById(entity.appointmentSetId) }.isInstanceOf(CaseloadAccessException::class.java)
  }

  @Nested
  @DisplayName("create appointment set")
  inner class CreateAppointmentSet {
    private val principal = mock<Principal>()
    private val prisonCode = moorlandPrisonCode
    private val categoryCode = "MEDO"
    private val appointmentTier = foundationTier()
    private val internalLocationId = 1L
    private val createdBy = "CREATED_BY_USER"

    private val appointmentSetCaptor = argumentCaptor<AppointmentSet>()

    private val createAppointmentSetWithOneAppointment = AppointmentSetCreateRequest(
      prisonCode = prisonCode,
      categoryCode = categoryCode,
      customName = "Custom name",
      internalLocationId = internalLocationId,
      inCell = false,
      startDate = LocalDate.now(),
      appointments = listOf(
        AppointmentSetAppointment(
          prisonerNumber = "A1234BC",
          startTime = LocalTime.of(8, 45),
          endTime = LocalTime.of(9, 15),
          extraInformation = "Extra medical information for 'A1234BC'",
        ),
      ),
    )

    private val createAppointmentSetWithThreeAppointments = AppointmentSetCreateRequest(
      prisonCode = prisonCode,
      categoryCode = categoryCode,
      customName = "Custom name",
      internalLocationId = internalLocationId,
      inCell = false,
      startDate = LocalDate.now(),
      appointments = listOf(
        AppointmentSetAppointment(
          prisonerNumber = "A1234BC",
          startTime = LocalTime.of(8, 45),
          endTime = LocalTime.of(9, 15),
          extraInformation = "Extra medical information for 'A1234BC'",
        ),
        AppointmentSetAppointment(
          prisonerNumber = "B2345CD",
          startTime = LocalTime.of(9, 15),
          endTime = LocalTime.of(9, 45),
          extraInformation = "Extra medical information for 'B2345CD'",
        ),
        AppointmentSetAppointment(
          prisonerNumber = "C3456DE",
          startTime = LocalTime.of(9, 45),
          endTime = LocalTime.of(10, 15),
          extraInformation = "Extra medical information for 'C3456DE'",
        ),
      ),
    )

    private val propertiesMapCaptor = argumentCaptor<Map<String, String>>()
    private val metricsMapCaptor = argumentCaptor<Map<String, Double>>()

    @BeforeEach
    fun setUp() {
      addCaseloadIdToRequestHeader(prisonCode)

      whenever(principal.name).thenReturn(createdBy)

      whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT))
        .thenReturn(mapOf("MEDO" to appointmentCategoryReferenceCode(categoryCode, "Medical - Doctor")))

      whenever(locationService.getLocationsForAppointmentsMap(prisonCode))
        .thenReturn(mapOf(internalLocationId to appointmentLocation(internalLocationId, prisonCode, "HB1 Doctors")))

      whenever(prisonerSearchApiClient.findByPrisonerNumbers(listOf("A1234BC")))
        .thenReturn(
          listOf(
            PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A1234BC", bookingId = 1, prisonId = moorlandPrisonCode),
          ),
        )

      whenever(prisonerSearchApiClient.findByPrisonerNumbers(listOf("A1234BC", "B2345CD", "C3456DE")))
        .thenReturn(
          listOf(
            PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A1234BC", bookingId = 1, prisonId = moorlandPrisonCode),
            PrisonerSearchPrisonerFixture.instance(prisonerNumber = "B2345CD", bookingId = 2, prisonId = moorlandPrisonCode),
            PrisonerSearchPrisonerFixture.instance(prisonerNumber = "C3456DE", bookingId = 3, prisonId = moorlandPrisonCode),
          ),
        )

      whenever(eventTierRepository.findById(foundationTier().eventTierId))
        .thenReturn(Optional.of(appointmentTier))

      whenever(appointmentSetRepository.saveAndFlush(appointmentSetCaptor.capture())).thenAnswer(AdditionalAnswers.returnsFirstArg<AppointmentSet>())
    }

    @Test
    fun `caseload access exception`() {
      addCaseloadIdToRequestHeader("WRONG")
      assertThatThrownBy {
        service.createAppointmentSet(appointmentSetCreateRequest(prisonCode = prisonCode), principal)
      }.isInstanceOf(CaseloadAccessException::class.java)
    }

    @Test
    fun `category code not found`() {
      assertThrows<IllegalArgumentException>(
        "Appointment Category with code 'NOT_FOUND' not found or is not active",
      ) {
        service.createAppointmentSet(
          appointmentSetCreateRequest(prisonCode = prisonCode, categoryCode = "NOT_FOUND"),
          principal,
        )
      }

      verifyNoInteractions(appointmentSetRepository)
    }

    @Test
    fun `internal location id not found`() {
      assertThrows<IllegalArgumentException>(
        "Appointment location with id '999' not found in prison 'MDI'",
      ) {
        service.createAppointmentSet(
          appointmentSetCreateRequest(prisonCode = prisonCode, categoryCode = categoryCode, internalLocationId = 999),
          principal,
        )
      }

      verifyNoInteractions(appointmentSetRepository)
    }

    @Test
    fun `prison numbers not found`() {
      assertThrows<IllegalArgumentException>(
        "Prisoner(s) with prisoner number(s) 'D4567EF', 'E4567FG' not found in prison 'MDI'",
      ) {
        service.createAppointmentSet(
          appointmentSetCreateRequest(
            prisonCode = prisonCode,
            categoryCode = categoryCode,
            internalLocationId = internalLocationId,
            prisonerNumbers = listOf("D4567EF", "E4567FG"),
          ),
          principal,
        )
      }

      verifyNoInteractions(appointmentSetRepository)
    }

    @Test
    fun `prison number not in supplied prison code`() {
      whenever(prisonerSearchApiClient.findByPrisonerNumbers(listOf("D4567EF", "E4567FG")))
        .thenReturn(
          listOf(
            PrisonerSearchPrisonerFixture.instance(prisonerNumber = "D4567EF", bookingId = 1, prisonId = "DIFFERENT"),
            PrisonerSearchPrisonerFixture.instance(prisonerNumber = "E4567FG", bookingId = 2, prisonId = moorlandPrisonCode),
          ),
        )

      assertThrows<IllegalArgumentException>(
        "Prisoner(s) with prisoner number(s) 'D4567EF' not found in prison 'MDI'",
      ) {
        service.createAppointmentSet(
          appointmentSetCreateRequest(
            prisonCode = prisonCode,
            categoryCode = categoryCode,
            internalLocationId = internalLocationId,
            prisonerNumbers = listOf("D4567EF", "E4567FG"),
          ),
          principal,
        )
      }

      verifyNoInteractions(appointmentSetRepository)
    }

    @Test
    fun `create appointment set with one appointment`() {
      val request = createAppointmentSetWithOneAppointment

      service.createAppointmentSet(request, principal)

      with(appointmentSetCaptor.firstValue) {
        this isEqualTo AppointmentSet(
          appointmentSetId = 0L,
          prisonCode = prisonCode,
          categoryCode = categoryCode,
          customName = "Custom name",
          appointmentTier = appointmentTier,
          appointmentOrganiser = null,
          internalLocationId = internalLocationId,
          customLocation = null,
          inCell = false,
          onWing = false,
          offWing = true,
          startDate = request.startDate!!,
          createdTime = appointmentSetCaptor.firstValue.createdTime,
          createdBy = createdBy,
          updatedTime = null,
          updatedBy = null,
        )

        with(appointmentSeries().single()) {
          val appointmentSeries = this
          this isEqualTo AppointmentSeries(
            appointmentSeriesId = 0L,
            appointmentSet = null,
            appointmentType = AppointmentType.INDIVIDUAL,
            prisonCode = prisonCode,
            categoryCode = categoryCode,
            customName = "Custom name",
            appointmentTier = appointmentTier,
            appointmentOrganiser = null,
            internalLocationId = internalLocationId,
            customLocation = null,
            inCell = false,
            onWing = false,
            offWing = true,
            startDate = request.startDate!!,
            startTime = request.appointments.single().startTime!!,
            endTime = request.appointments.single().endTime!!,
            schedule = null,
            unlockNotes = null,
            extraInformation = request.appointments.single().extraInformation,
            createdTime = appointmentSetCaptor.firstValue.createdTime,
            createdBy = createdBy,
            updatedTime = null,
            updatedBy = null,
          )

          with(appointments().single()) {
            val appointment = this
            this isEqualTo Appointment(
              appointmentId = 0L,
              appointmentSeries = appointmentSeries,
              sequenceNumber = 1,
              prisonCode = prisonCode,
              categoryCode = categoryCode,
              customName = "Custom name",
              appointmentTier = appointmentTier,
              appointmentOrganiser = null,
              internalLocationId = internalLocationId,
              customLocation = null,
              inCell = false,
              onWing = false,
              offWing = true,
              startDate = request.startDate!!,
              startTime = request.appointments.single().startTime!!,
              endTime = request.appointments.single().endTime!!,
              unlockNotes = null,
              extraInformation = request.appointments.single().extraInformation,
              createdTime = appointmentSetCaptor.firstValue.createdTime,
              createdBy = createdBy,
              updatedTime = null,
              updatedBy = null,
            )

            with(attendees().single()) {
              this isEqualTo AppointmentAttendee(
                appointmentAttendeeId = 0L,
                appointment = appointment,
                prisonerNumber = request.appointments.single().prisonerNumber!!,
                bookingId = 1,
                addedTime = null,
                addedBy = null,
                attended = null,
                attendanceRecordedTime = null,
                attendanceRecordedBy = null,
              ).apply {
                removedTime = null
                removalReason = null
                removedBy = null
              }
            }
          }
        }
      }
    }

    @Test
    fun `create appointment set with three appointments`() {
      val request = createAppointmentSetWithThreeAppointments

      service.createAppointmentSet(request, principal)

      with(appointmentSetCaptor.firstValue.appointments()) {
        this hasSize 3
        map { it.startDate }.distinct().single() isEqualTo request.startDate
        map { it.startTime } isEqualTo request.appointments.map { it.startTime }
        map { it.endTime } isEqualTo request.appointments.map { it.endTime }
        map { it.extraInformation } isEqualTo request.appointments.map { it.extraInformation }

        with(flatMap { it.attendees() }) {
          this hasSize 3
          map { it.prisonerNumber } isEqualTo listOf("A1234BC", "B2345CD", "C3456DE")
          map { it.bookingId } isEqualTo listOf(1L, 2L, 3L)
        }
      }
    }

    @Test
    fun `internal location id = null, in cell = true`() {
      val request = createAppointmentSetWithOneAppointment.copy(internalLocationId = null, inCell = true)

      service.createAppointmentSet(request, principal)

      appointmentSetCaptor.firstValue.internalLocationId isEqualTo null
      appointmentSetCaptor.firstValue.inCell isBool true

      verify(telemetryClient).trackEvent(eq(TelemetryEvent.APPOINTMENT_SET_CREATED.value), propertiesMapCaptor.capture(), metricsMapCaptor.capture())
      with(propertiesMapCaptor.firstValue) {
        this[INTERNAL_LOCATION_ID_PROPERTY_KEY] isEqualTo ""
        this[INTERNAL_LOCATION_DESCRIPTION_PROPERTY_KEY] isEqualTo "In cell"
      }
    }

    @Test
    fun `internal location id = 1, in cell = true`() {
      val request = createAppointmentSetWithOneAppointment.copy(internalLocationId = 1, inCell = true)

      service.createAppointmentSet(request, principal)

      appointmentSetCaptor.firstValue.internalLocationId isEqualTo null
      appointmentSetCaptor.firstValue.inCell isBool true

      verify(telemetryClient).trackEvent(eq(TelemetryEvent.APPOINTMENT_SET_CREATED.value), propertiesMapCaptor.capture(), metricsMapCaptor.capture())
      with(propertiesMapCaptor.firstValue) {
        this[INTERNAL_LOCATION_ID_PROPERTY_KEY] isEqualTo ""
        this[INTERNAL_LOCATION_DESCRIPTION_PROPERTY_KEY] isEqualTo "In cell"
      }
    }

    @Test
    fun `null custom name`() {
      val request = createAppointmentSetWithOneAppointment.copy(customName = null)

      service.createAppointmentSet(request, principal)

      appointmentSetCaptor.firstValue.customName isEqualTo null

      verify(telemetryClient).trackEvent(eq(TelemetryEvent.APPOINTMENT_SET_CREATED.value), propertiesMapCaptor.capture(), metricsMapCaptor.capture())
      propertiesMapCaptor.firstValue[HAS_CUSTOM_NAME_PROPERTY_KEY] isEqualTo "false"
      metricsMapCaptor.firstValue[CUSTOM_NAME_LENGTH_METRIC_KEY] isEqualTo 0.0
    }

    @Test
    fun `empty custom name`() {
      val request = createAppointmentSetWithOneAppointment.copy(customName = "")

      service.createAppointmentSet(request, principal)

      appointmentSetCaptor.firstValue.customName isEqualTo null

      verify(telemetryClient).trackEvent(eq(TelemetryEvent.APPOINTMENT_SET_CREATED.value), propertiesMapCaptor.capture(), metricsMapCaptor.capture())
      propertiesMapCaptor.firstValue[HAS_CUSTOM_NAME_PROPERTY_KEY] isEqualTo "false"
      metricsMapCaptor.firstValue[CUSTOM_NAME_LENGTH_METRIC_KEY] isEqualTo 0.0
    }

    @Test
    fun `whitespace only custom name`() {
      val request = createAppointmentSetWithOneAppointment.copy(customName = "    ")

      service.createAppointmentSet(request, principal)

      appointmentSetCaptor.firstValue.customName isEqualTo null

      verify(telemetryClient).trackEvent(eq(TelemetryEvent.APPOINTMENT_SET_CREATED.value), propertiesMapCaptor.capture(), metricsMapCaptor.capture())
      propertiesMapCaptor.firstValue[HAS_CUSTOM_NAME_PROPERTY_KEY] isEqualTo "false"
      metricsMapCaptor.firstValue[CUSTOM_NAME_LENGTH_METRIC_KEY] isEqualTo 0.0
    }

    @Test
    fun `whitespace start and end custom name`() {
      val request = createAppointmentSetWithOneAppointment.copy(customName = "   Custom name  ")

      service.createAppointmentSet(request, principal)

      appointmentSetCaptor.firstValue.customName isEqualTo "Custom name"

      verify(telemetryClient).trackEvent(eq(TelemetryEvent.APPOINTMENT_SET_CREATED.value), propertiesMapCaptor.capture(), metricsMapCaptor.capture())
      propertiesMapCaptor.firstValue[HAS_CUSTOM_NAME_PROPERTY_KEY] isEqualTo "true"
      metricsMapCaptor.firstValue[CUSTOM_NAME_LENGTH_METRIC_KEY] isEqualTo 11.0
    }

    @Test
    fun `null extra information`() {
      val request = createAppointmentSetWithOneAppointment.copy(
        appointments = listOf(
          createAppointmentSetWithOneAppointment.appointments.single().copy(extraInformation = null),
        ),
      )

      service.createAppointmentSet(request, principal)

      appointmentSetCaptor.firstValue.appointments().single().extraInformation isEqualTo null
    }

    @Test
    fun `empty extra information`() {
      val request = createAppointmentSetWithOneAppointment.copy(
        appointments = listOf(
          createAppointmentSetWithOneAppointment.appointments.single().copy(extraInformation = ""),
        ),
      )

      service.createAppointmentSet(request, principal)

      appointmentSetCaptor.firstValue.appointments().single().extraInformation isEqualTo null
    }

    @Test
    fun `whitespace only extra information`() {
      val request = createAppointmentSetWithOneAppointment.copy(
        appointments = listOf(
          createAppointmentSetWithOneAppointment.appointments.single().copy(extraInformation = "    "),
        ),
      )

      service.createAppointmentSet(request, principal)

      appointmentSetCaptor.firstValue.appointments().single().extraInformation isEqualTo null
    }

    @Test
    fun `whitespace start and end extra information`() {
      val request = createAppointmentSetWithOneAppointment.copy(
        appointments = listOf(
          createAppointmentSetWithOneAppointment.appointments.single().copy(extraInformation = "   Extra medical information for 'A1234BC'  "),
        ),
      )

      service.createAppointmentSet(request, principal)

      appointmentSetCaptor.firstValue.appointments().single().extraInformation isEqualTo "Extra medical information for 'A1234BC'"
    }

    @Test
    fun `track custom event`() {
      val request = createAppointmentSetWithOneAppointment

      service.createAppointmentSet(request, principal)

      verify(telemetryClient).trackEvent(eq(TelemetryEvent.APPOINTMENT_SET_CREATED.value), any(), any())
    }

    @Test
    fun `log audit event`() {
      val request = createAppointmentSetWithOneAppointment

      service.createAppointmentSet(request, principal)

      verify(auditService).logEvent(any<AppointmentSetCreatedEvent>())
    }
  }
}
