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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nomismapping.api.NomisDpsLocationMapping
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nomismapping.api.NomisMappingAPIClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentAttendee
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentSet
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocationDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSetCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSetDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSetEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.eventOrganiser
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.eventTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.foundationTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentSetCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSetAppointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSetCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentSetRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.EventOrganiserRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.EventTierRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AppointmentSetService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.CUSTOM_NAME_LENGTH_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.DPS_LOCATION_ID_PROPERTY_KEY
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
import java.util.*

class AppointmentSetServiceTest {
  private val appointmentSetRepository: AppointmentSetRepository = mock()
  private val eventTierRepository: EventTierRepository = mock()
  private val eventOrganiserRepository: EventOrganiserRepository = mock()
  private val appointmentCategoryService: AppointmentCategoryService = mock()
  private val locationService: LocationService = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val auditService: AuditService = mock()
  private val nomisMappingAPIClient: NomisMappingAPIClient = mock()

  private val service = AppointmentSetService(
    appointmentSetRepository,
    eventTierRepository,
    eventOrganiserRepository,
    appointmentCategoryService,
    locationService,
    prisonerSearchApiClient,
    TransactionHandler(),
    outboundEventsService,
    telemetryClient,
    auditService,
    nomisMappingAPIClient,
    2L,
  )

  @BeforeEach
  fun setUp() {
    addCaseloadIdToRequestHeader("TPR")

    whenever(eventTierRepository.findByCode(eventTier().code)).thenReturn(eventTier())
    whenever(eventOrganiserRepository.findByCode(eventOrganiser().code)).thenReturn(eventOrganiser())

    whenever(prisonerSearchApiClient.findByPrisonerNumbers(listOf("A1234BC", "A1234BD"))).thenReturn(
      listOf(
        PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = "A1234BC",
          bookingId = 456,
          firstName = "TEST02",
          lastName = "PRISONER01",
          prisonId = MOORLAND_PRISON_CODE,
          cellLocation = "1-2-3",
        ),
        PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = "A1234BD",
          bookingId = 457,
          firstName = "TEST02",
          lastName = "PRISONER02",
          prisonId = MOORLAND_PRISON_CODE,
          cellLocation = "1-2-4",
        ),
      ),
    )
  }

  @AfterEach
  fun tearDown() {
    clearCaseloadIdFromRequestHeader()
  }

  @Test
  fun `getAppointmentSetDetailsById returns mapped appointment details for known appointment set id`() {
    val entity = appointmentSetEntity()
    whenever(appointmentSetRepository.findById(entity.appointmentSetId)).thenReturn(Optional.of(entity))

    whenever(appointmentCategoryService.getAll())
      .thenReturn(mapOf(entity.categoryCode to appointmentCategory(entity.categoryCode)))

    whenever(locationService.getLocationDetailsForAppointmentsMap(entity.prisonCode))
      .thenReturn(mapOf(entity.internalLocationId!! to appointmentLocationDetails(entity.internalLocationId!!, entity.dpsLocationId!!, "TPR")))

    whenever(prisonerSearchApiClient.findByPrisonerNumbersMap(entity.prisonerNumbers())).thenReturn(
      mapOf(
        "A1234BC" to PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = "A1234BC",
          bookingId = 456,
          firstName = "TEST01",
          lastName = "PRISONER01",
          prisonId = "TPR",
          cellLocation = "1-2-3",
          category = "A",
        ),
        "B2345CD" to PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = "B2345CD",
          bookingId = 457,
          firstName = "TEST02",
          lastName = "PRISONER02",
          prisonId = "TPR",
          cellLocation = "1-2-4",
          category = "E",
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
    private val prisonCode = MOORLAND_PRISON_CODE
    private val categoryCode = "MEDO"
    private val appointmentTier = foundationTier()
    private val internalLocationId = 1L
    private val dpsLocationId = UUID.fromString("44444444-1111-2222-3333-444444444444")
    private val createdBy = "CREATED_BY_USER"

    private val appointmentSetCaptor = argumentCaptor<AppointmentSet>()

    private val createAppointmentSetWithOneAppointment = AppointmentSetCreateRequest(
      prisonCode = prisonCode,
      categoryCode = categoryCode,
      tierCode = eventTier().code,
      organiserCode = eventOrganiser().code,
      customName = "Custom name",
      internalLocationId = internalLocationId,
      dpsLocationId = dpsLocationId,
      inCell = false,
      startDate = LocalDate.now(),
      appointments = listOf(
        AppointmentSetAppointment(
          prisonerNumber = "A1234BC",
          startTime = LocalTime.of(8, 45),
          endTime = LocalTime.of(9, 15),
          extraInformation = "Extra medical information for 'A1234BC'",
          prisonerExtraInformation = "Prisoner extra information for 'A1234BC'",
        ),
      ),
    )

    private val createAppointmentSetWithThreeAppointments = AppointmentSetCreateRequest(
      prisonCode = prisonCode,
      categoryCode = categoryCode,
      tierCode = eventTier().code,
      organiserCode = eventOrganiser().code,
      customName = "Custom name",
      internalLocationId = internalLocationId,
      dpsLocationId = dpsLocationId,
      inCell = false,
      startDate = LocalDate.now(),
      appointments = listOf(
        AppointmentSetAppointment(
          prisonerNumber = "A1234BC",
          startTime = LocalTime.of(8, 45),
          endTime = LocalTime.of(9, 15),
          extraInformation = "Extra medical information for 'A1234BC'",
          prisonerExtraInformation = "Prisoner extra information for 'A1234BC'",
        ),
        AppointmentSetAppointment(
          prisonerNumber = "B2345CD",
          startTime = LocalTime.of(9, 15),
          endTime = LocalTime.of(9, 45),
          extraInformation = "Extra medical information for 'B2345CD'",
          prisonerExtraInformation = "Prisoner extra information for 'B2345CD'",
        ),
        AppointmentSetAppointment(
          prisonerNumber = "C3456DE",
          startTime = LocalTime.of(9, 45),
          endTime = LocalTime.of(10, 15),
          extraInformation = "Extra medical information for 'C3456DE'",
          prisonerExtraInformation = "Prisoner extra information for 'C3456DE'",
        ),
      ),
    )

    private val propertiesMapCaptor = argumentCaptor<Map<String, String>>()
    private val metricsMapCaptor = argumentCaptor<Map<String, Double>>()

    @BeforeEach
    fun setUp() {
      addCaseloadIdToRequestHeader(prisonCode)

      whenever(principal.name).thenReturn(createdBy)

      whenever(appointmentCategoryService.getAll())
        .thenReturn(mapOf("MEDO" to appointmentCategory(categoryCode, "Medical - Doctor")))

      whenever(locationService.getLocationDetailsForAppointmentsMapByDpsLocationId(prisonCode))
        .thenReturn(mapOf(dpsLocationId to appointmentLocationDetails(internalLocationId, dpsLocationId, prisonCode, "HB1 Doctors")))

      whenever(nomisMappingAPIClient.getLocationMappingByDpsId(dpsLocationId))
        .thenReturn(NomisDpsLocationMapping(dpsLocationId, 123))

      whenever(nomisMappingAPIClient.getLocationMappingByNomisId(123))
        .thenReturn(NomisDpsLocationMapping(dpsLocationId, 123))

      whenever(prisonerSearchApiClient.findByPrisonerNumbers(listOf("A1234BC")))
        .thenReturn(
          listOf(
            PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A1234BC", bookingId = 1, prisonId = MOORLAND_PRISON_CODE),
          ),
        )

      whenever(prisonerSearchApiClient.findByPrisonerNumbers(listOf("A1234BC", "B2345CD", "C3456DE")))
        .thenReturn(
          listOf(
            PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A1234BC", bookingId = 1, prisonId = MOORLAND_PRISON_CODE),
            PrisonerSearchPrisonerFixture.instance(prisonerNumber = "B2345CD", bookingId = 2, prisonId = MOORLAND_PRISON_CODE),
            PrisonerSearchPrisonerFixture.instance(prisonerNumber = "C3456DE", bookingId = 3, prisonId = MOORLAND_PRISON_CODE),
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
      val exception = assertThrows<IllegalArgumentException> {
        service.createAppointmentSet(
          appointmentSetCreateRequest(prisonCode = prisonCode, categoryCode = "NOT_FOUND"),
          principal,
        )
      }
      exception.message isEqualTo "Appointment Category with code 'NOT_FOUND' not found or is not active"

      verifyNoInteractions(appointmentSetRepository)
    }

    @Test
    fun `DPS location id not found`() {
      val dpsLocationId = UUID.randomUUID()

      val exception = assertThrows<IllegalArgumentException> {
        service.createAppointmentSet(
          appointmentSetCreateRequest(prisonCode = prisonCode, categoryCode = categoryCode, dpsLocationId = dpsLocationId),
          principal,
        )
      }
      exception.message isEqualTo "Appointment location with DPS Location id '$dpsLocationId' not found in prison 'MDI'"

      verifyNoInteractions(appointmentSetRepository)
    }

    @Test
    fun `internal location id not found`() {
      val exception = assertThrows<IllegalArgumentException> {
        service.createAppointmentSet(
          appointmentSetCreateRequest(prisonCode = prisonCode, categoryCode = categoryCode, internalLocationId = 999, dpsLocationId = null),
          principal,
        )
      }
      exception.message isEqualTo "Appointment location with id '999' not found in prison 'MDI'"

      verifyNoInteractions(appointmentSetRepository)
    }

    @Test
    fun `createAppointmentSet determines DPS location id when DPS location id is null`() {
      val request = appointmentSetCreateRequest(prisonCode = prisonCode, categoryCode = categoryCode, dpsLocationId = null)

      whenever(locationService.getLocationDetailsForAppointmentsMap(prisonCode))
        .thenReturn(mapOf(123L to appointmentLocationDetails(123L, dpsLocationId, request.prisonCode!!)))

      service.createAppointmentSet(request, principal)

      with(appointmentSetCaptor.firstValue) {
        inCell isBool false
        internalLocationId isEqualTo 123
        dpsLocationId = dpsLocationId
        appointmentSeries().forEach {
          it.inCell isBool false
          it.internalLocationId isEqualTo 123
          it.dpsLocationId = dpsLocationId
          with(it.appointments().single()) {
            inCell isBool false
            internalLocationId isEqualTo 123
            dpsLocationId = dpsLocationId
          }
        }
      }

      verify(telemetryClient).trackEvent(eq(TelemetryEvent.APPOINTMENT_SET_CREATED.value), propertiesMapCaptor.capture(), metricsMapCaptor.capture())

      with(propertiesMapCaptor.firstValue) {
        this[INTERNAL_LOCATION_ID_PROPERTY_KEY] isEqualTo "123"
        this[DPS_LOCATION_ID_PROPERTY_KEY] isEqualTo "44444444-1111-2222-3333-444444444444"
        this[INTERNAL_LOCATION_DESCRIPTION_PROPERTY_KEY] isEqualTo "Test Appointment Location"
      }

      verify(nomisMappingAPIClient).getLocationMappingByNomisId(123L)
      verify(appointmentSetRepository).saveAndFlush(any())
    }

    @Test
    fun `createAppointmentSet determines location id when location id is null`() {
      val request = appointmentSetCreateRequest(prisonCode = prisonCode, categoryCode = categoryCode, internalLocationId = null)

      whenever(locationService.getLocationDetailsForAppointmentsMap(prisonCode))
        .thenReturn(mapOf(123L to appointmentLocationDetails(123L, dpsLocationId, request.prisonCode!!)))

      service.createAppointmentSet(request, principal)

      with(appointmentSetCaptor.firstValue) {
        inCell isBool false
        internalLocationId isEqualTo 123
        dpsLocationId = dpsLocationId
        appointmentSeries().forEach {
          it.inCell isBool false
          it.internalLocationId isEqualTo 123
          it.dpsLocationId = dpsLocationId
          with(it.appointments().single()) {
            inCell isBool false
            internalLocationId isEqualTo 123
            dpsLocationId = dpsLocationId
          }
        }
      }

      verify(telemetryClient).trackEvent(eq(TelemetryEvent.APPOINTMENT_SET_CREATED.value), propertiesMapCaptor.capture(), metricsMapCaptor.capture())

      with(propertiesMapCaptor.firstValue) {
        this[INTERNAL_LOCATION_ID_PROPERTY_KEY] isEqualTo "123"
        this[DPS_LOCATION_ID_PROPERTY_KEY] isEqualTo "44444444-1111-2222-3333-444444444444"
        this[INTERNAL_LOCATION_DESCRIPTION_PROPERTY_KEY] isEqualTo "HB1 Doctors"
      }

      verify(nomisMappingAPIClient).getLocationMappingByDpsId(dpsLocationId)
      verify(appointmentSetRepository).saveAndFlush(any())
    }

    @Test
    fun `createAppointmentSet tier code not found`() {
      val exception = assertThrows<IllegalArgumentException> {
        service.createAppointmentSet(
          appointmentSetCreateRequest(
            prisonCode = MOORLAND_PRISON_CODE,
            categoryCode = categoryCode,
            internalLocationId = internalLocationId,
            tierCode = "INVALID",
          ),
          principal,
        )
      }
      exception.message isEqualTo "Event tier \"INVALID\" not found"

      verifyNoInteractions(appointmentSetRepository)
    }

    @Test
    fun `createAppointmentSet organiser code not found`() {
      val exception = assertThrows<IllegalArgumentException> {
        service.createAppointmentSet(
          appointmentSetCreateRequest(
            prisonCode = MOORLAND_PRISON_CODE,
            categoryCode = categoryCode,
            internalLocationId = internalLocationId,
            organiserCode = "INVALID",
          ),
          principal,
        )
      }
      exception.message isEqualTo "Event organiser \"INVALID\" not found"

      verifyNoInteractions(appointmentSetRepository)
    }

    @Test
    fun `prison numbers not found`() {
      val exception = assertThrows<IllegalArgumentException> {
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
      exception.message isEqualTo "Prisoner(s) with prisoner number(s) 'D4567EF', 'E4567FG' not found, were inactive or are residents of a different prison."

      verifyNoInteractions(appointmentSetRepository)
    }

    @Test
    fun `prison number not in supplied prison code`() {
      whenever(prisonerSearchApiClient.findByPrisonerNumbers(listOf("D4567EF", "E4567FG")))
        .thenReturn(
          listOf(
            PrisonerSearchPrisonerFixture.instance(prisonerNumber = "D4567EF", bookingId = 1, prisonId = "DIFFERENT"),
            PrisonerSearchPrisonerFixture.instance(prisonerNumber = "E4567FG", bookingId = 2, prisonId = MOORLAND_PRISON_CODE),
          ),
        )

      val exception = assertThrows<IllegalArgumentException> {
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
      exception.message isEqualTo "Prisoner(s) with prisoner number(s) 'D4567EF' not found, were inactive or are residents of a different prison."

      verifyNoInteractions(appointmentSetRepository)
    }

    @Test
    fun `fails if start date is too far into the future`() {
      val exception = assertThrows<IllegalArgumentException> {
        service.createAppointmentSet(
          appointmentSetCreateRequest(
            prisonCode = prisonCode,
            categoryCode = categoryCode,
            internalLocationId = internalLocationId,
            startDate = LocalDate.now().plusDays(3),
          ),
          principal,
        )
      }
      exception.message isEqualTo "Start date cannot be more than 2 days into the future."

      verifyNoInteractions(appointmentSetRepository)
    }

    @Test
    fun `succeeds if start date is the maximum allowed`() {
      val result = service.createAppointmentSet(
        appointmentSetCreateRequest(
          prisonCode = prisonCode,
          categoryCode = categoryCode,
          internalLocationId = internalLocationId,
          startDate = LocalDate.now().plusDays(2),
        ),
        principal,
      )

      assertThat(result.startDate).isEqualTo(LocalDate.now().plusDays(2))
    }

    @Test
    fun `create appointment set with one appointment`() {
      val request = createAppointmentSetWithOneAppointment

      service.createAppointmentSet(request, principal)

      with(appointmentSetCaptor.firstValue) {
        this isEqualTo AppointmentSet(
          appointmentSetId = 0L,
          prisonCode = request.prisonCode!!,
          categoryCode = request.categoryCode!!,
          appointmentTier = eventTier(),
          customName = "Custom name",
          internalLocationId = 123,
          dpsLocationId = request.dpsLocationId!!,
          customLocation = null,
          inCell = false,
          onWing = false,
          offWing = true,
          startDate = request.startDate!!,
          createdTime = appointmentSetCaptor.firstValue.createdTime,
          createdBy = principal.name,
          updatedTime = null,
          updatedBy = null,
        ).also {
          it.appointmentOrganiser = appointmentOrganiser
        }

        with(appointmentSeries().single()) {
          val appointmentSeries = this
          this isEqualTo AppointmentSeries(
            appointmentSeriesId = 0L,
            appointmentSet = null,
            appointmentType = uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentType.INDIVIDUAL,
            prisonCode = request.prisonCode,
            categoryCode = request.categoryCode,
            appointmentTier = eventTier(),
            customName = "Custom name",
            internalLocationId = 123,
            dpsLocationId = request.dpsLocationId,
            customLocation = null,
            inCell = false,
            onWing = false,
            offWing = true,
            startDate = request.startDate,
            startTime = request.appointments.single().startTime!!,
            endTime = request.appointments.single().endTime!!,
            schedule = null,
            unlockNotes = null,
            extraInformation = request.appointments.single().extraInformation,
            prisonerExtraInformation = request.appointments.single().prisonerExtraInformation,
            createdTime = appointmentSetCaptor.firstValue.createdTime,
            createdBy = principal.name,
            updatedTime = null,
            updatedBy = null,
          ).also {
            it.appointmentOrganiser = appointmentOrganiser
          }

          with(appointments().single()) {
            val appointment = this
            this isEqualTo Appointment(
              appointmentId = 0L,
              appointmentSeries = appointmentSeries,
              sequenceNumber = 1,
              prisonCode = request.prisonCode,
              categoryCode = request.categoryCode,
              customName = "Custom name",
              appointmentTier = eventTier(),
              internalLocationId = 123,
              dpsLocationId = request.dpsLocationId,
              customLocation = null,
              inCell = false,
              onWing = false,
              offWing = true,
              startDate = request.startDate,
              startTime = request.appointments.single().startTime!!,
              endTime = request.appointments.single().endTime!!,
              unlockNotes = null,
              extraInformation = request.appointments.single().extraInformation,
              prisonerExtraInformation = request.appointments.single().prisonerExtraInformation,
              createdTime = appointmentSetCaptor.firstValue.createdTime,
              createdBy = principal.name,
              updatedTime = null,
              updatedBy = null,
            ).also {
              it.appointmentOrganiser = appointmentOrganiser
            }

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
    fun `internal location id = null, DPS location id is null and in cell is true`() {
      val request = createAppointmentSetWithOneAppointment.copy(internalLocationId = null, dpsLocationId = null, inCell = true)

      service.createAppointmentSet(request, principal)

      with(appointmentSetCaptor.firstValue) {
        inCell isBool true
        internalLocationId isEqualTo null
        dpsLocationId = null
        with(appointmentSeries().single()) {
          inCell isBool true
          internalLocationId isEqualTo null
          dpsLocationId = null
          with(appointments().single()) {
            inCell isBool true
            internalLocationId isEqualTo null
            dpsLocationId = null
          }
        }
      }

      verify(telemetryClient).trackEvent(eq(TelemetryEvent.APPOINTMENT_SET_CREATED.value), propertiesMapCaptor.capture(), metricsMapCaptor.capture())

      with(propertiesMapCaptor.firstValue) {
        this[INTERNAL_LOCATION_ID_PROPERTY_KEY] isEqualTo ""
        this[DPS_LOCATION_ID_PROPERTY_KEY] isEqualTo ""
        this[INTERNAL_LOCATION_DESCRIPTION_PROPERTY_KEY] isEqualTo "In cell"
      }
    }

    @Test
    fun `internal location id is not null, DPS location id is null and in cell is true`() {
      val request = createAppointmentSetWithOneAppointment.copy(dpsLocationId = null, inCell = true)

      service.createAppointmentSet(request, principal)

      with(appointmentSetCaptor.firstValue) {
        inCell isBool true
        internalLocationId isEqualTo null
        dpsLocationId = null
        with(appointmentSeries().single()) {
          inCell isBool true
          internalLocationId isEqualTo null
          dpsLocationId = null
          with(appointments().single()) {
            inCell isBool true
            internalLocationId isEqualTo null
            dpsLocationId = null
          }
        }
      }

      verify(telemetryClient).trackEvent(eq(TelemetryEvent.APPOINTMENT_SET_CREATED.value), propertiesMapCaptor.capture(), metricsMapCaptor.capture())

      with(propertiesMapCaptor.firstValue) {
        this[INTERNAL_LOCATION_ID_PROPERTY_KEY] isEqualTo ""
        this[DPS_LOCATION_ID_PROPERTY_KEY] isEqualTo ""
        this[INTERNAL_LOCATION_DESCRIPTION_PROPERTY_KEY] isEqualTo "In cell"
      }
    }

    @Test
    fun `internal location id is null, DPS location id is not null and in cell is true`() {
      val request = createAppointmentSetWithOneAppointment.copy(internalLocationId = null, inCell = true)

      service.createAppointmentSet(request, principal)

      with(appointmentSetCaptor.firstValue) {
        inCell isBool true
        internalLocationId isEqualTo null
        dpsLocationId = null
        with(appointmentSeries().single()) {
          inCell isBool true
          internalLocationId isEqualTo null
          dpsLocationId = null
          with(appointments().single()) {
            inCell isBool true
            internalLocationId isEqualTo null
            dpsLocationId = null
          }
        }
      }

      verify(telemetryClient).trackEvent(eq(TelemetryEvent.APPOINTMENT_SET_CREATED.value), propertiesMapCaptor.capture(), metricsMapCaptor.capture())

      with(propertiesMapCaptor.firstValue) {
        this[INTERNAL_LOCATION_ID_PROPERTY_KEY] isEqualTo ""
        this[DPS_LOCATION_ID_PROPERTY_KEY] isEqualTo ""
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
