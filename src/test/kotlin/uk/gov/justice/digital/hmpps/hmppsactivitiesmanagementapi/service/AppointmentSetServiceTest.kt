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
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSetCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSetDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSetEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.userDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentHostRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSetRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentTierRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.CaseloadAccessException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.addCaseloadIdToRequestHeader
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.clearCaseloadIdFromRequestHeader
import java.security.Principal
import java.util.Optional

class AppointmentSetServiceTest {
  private val appointmentSetRepository: AppointmentSetRepository = mock()
  private val appointmentTierRepository: AppointmentTierRepository = mock()
  private val appointmentHostRepository: AppointmentHostRepository = mock()
  private val referenceCodeService: ReferenceCodeService = mock()
  private val locationService: LocationService = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val prisonApiClient: PrisonApiClient = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val auditService: AuditService = mock()

  private val service = AppointmentSetService(
    appointmentSetRepository,
    appointmentTierRepository,
    appointmentHostRepository,
    referenceCodeService,
    locationService,
    prisonerSearchApiClient,
    prisonApiClient,
    TransactionHandler(),
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
    private val internalLocationId = 1L
    private val createdBy = "CREATED_BY_USER"

    @BeforeEach
    fun setUp() {
      addCaseloadIdToRequestHeader(prisonCode)

      whenever(principal.name).thenReturn(createdBy)

      whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT))
        .thenReturn(mapOf("MEDO" to appointmentCategoryReferenceCode(categoryCode, "Medical - Doctor")))

      whenever(locationService.getLocationsForAppointmentsMap(prisonCode))
        .thenReturn(mapOf(internalLocationId to appointmentLocation(internalLocationId, prisonCode, "HB1 Doctors")))
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

    /*@Test
    fun `create appointment set success`() {
      val request = appointmentSetCreateRequest()

      whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT))
        .thenReturn(mapOf(request.categoryCode!! to appointmentCategoryReferenceCode(request.categoryCode!!)))
      whenever(locationService.getLocationsForAppointmentsMap(request.prisonCode!!))
        .thenReturn(
          mapOf(
            request.internalLocationId!! to appointmentLocation(
              request.internalLocationId!!,
              request.prisonCode!!,
            ),
          ),
        )
      whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.appointments.map { it.prisonerNumber!! }))
        .thenReturn(
          listOf(
            PrisonerSearchPrisonerFixture.instance(prisonerNumber = request.appointments[0].prisonerNumber!!, bookingId = 1, prisonId = request.prisonCode),
            PrisonerSearchPrisonerFixture.instance(prisonerNumber = request.appointments[1].prisonerNumber!!, bookingId = 2, prisonId = request.prisonCode),
          ),
        )

      whenever(appointmentSetRepository.saveAndFlush(appointmentSetEntityCaptor.capture())).thenReturn(
        AppointmentSet(
          appointmentSetId = 1,
          prisonCode = request.prisonCode!!,
          categoryCode = request.categoryCode!!,
          customName = request.customName,
          appointmentTier = appointmentTierNotSpecified(),
          internalLocationId = request.internalLocationId,
          inCell = request.inCell,
          startDate = request.startDate!!,
          createdBy = "TEST.USER",
        ).apply {
          this.addAppointmentSeries(appointmentSeriesEntity(appointmentSeriesId = 1, appointmentSet = this))
          this.addAppointmentSeries(appointmentSeriesEntity(appointmentSeriesId = 2, appointmentSet = this))
        },
      )

      service.createAppointmentSet(request, principal)

      verify(telemetryClient).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_SET_CREATED.value),
        telemetryPropertyMap.capture(),
        telemetryMetricsMap.capture(),
      )

      with(appointmentSetEntityCaptor.value) {
        assertThat(prisonCode).isEqualTo(request.prisonCode)
        assertThat(categoryCode).isEqualTo(request.categoryCode)
        assertThat(customName).isEqualTo(request.customName)
        assertThat(internalLocationId).isEqualTo(request.internalLocationId)
        assertThat(inCell).isEqualTo(request.inCell)
        assertThat(startDate).isEqualTo(request.startDate)
        assertThat(createdBy).isEqualTo(uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.DEFAULT_USERNAME)
        assertThat(appointmentSeries()).hasSize(2)
        assertThat(appointmentSeries()[0].appointments()[0].attendees()[0].prisonerNumber).isEqualTo("A1234BC")
        assertThat(appointmentSeries()[1].appointments()[0].attendees()[0].prisonerNumber).isEqualTo("A1234BD")

        appointmentSeries().forEach {
          assertThat(it.categoryCode).isEqualTo("TEST")
          assertThat(it.prisonCode).isEqualTo("TPR")
          assertThat(it.internalLocationId).isEqualTo(123)
          assertThat(it.inCell).isFalse
          assertThat(it.startDate).isEqualTo(java.time.LocalDate.now().plusDays(1))
          assertThat(it.startTime).isEqualTo(java.time.LocalTime.of(13, 0))
          assertThat(it.endTime).isEqualTo(java.time.LocalTime.of(14, 30))
          assertThat(it.extraInformation).isEqualTo("Test comment")
          assertThat(it.customName).isEqualTo("Appointment description")
        }
      }

      with(telemetryPropertyMap) {
        assertThat(value[uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.USER_PROPERTY_KEY]).isEqualTo(principal.name)
        assertThat(value[uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISON_CODE_PROPERTY_KEY]).isEqualTo("TPR")
        assertThat(value[uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_SET_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.CATEGORY_CODE_PROPERTY_KEY]).isEqualTo("TEST")
        assertThat(value[uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.HAS_CUSTOM_NAME_PROPERTY_KEY]).isEqualTo("true")
        assertThat(value[uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.INTERNAL_LOCATION_ID_PROPERTY_KEY]).isEqualTo("123")
        assertThat(value[uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.START_DATE_PROPERTY_KEY]).isEqualTo(request.startDate.toString())
        assertThat(value[uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EARLIEST_START_TIME_PROPERTY_KEY]).isEqualTo("09:00")
        assertThat(value[uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.LATEST_END_TIME_PROPERTY_KEY]).isEqualTo("10:30")
      }

      with(telemetryMetricsMap) {
        assertThat(value[uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISONER_COUNT_METRIC_KEY]).isEqualTo(4.0)
        assertThat(value[uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(4.0)
        assertThat(value[uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(4.0)
        assertThat(value[uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.CUSTOM_NAME_LENGTH_METRIC_KEY]).isEqualTo(23.0)
        assertThat(value[uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EXTRA_INFORMATION_COUNT_METRIC_KEY]).isEqualTo(4.0)
        assertThat(value[uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_TIME_MS_METRIC_KEY]).isNotNull
      }

      verify(auditService).logEvent(any<AppointmentSetCreatedEvent>())
    }

    @Test
    fun `create appointment set throws illegal argument exception when prisoner is not a resident of requested prison code`() {
      val request = appointmentSetCreateRequest()

      whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT))
        .thenReturn(mapOf(request.categoryCode!! to appointmentCategoryReferenceCode(request.categoryCode!!)))
      whenever(locationService.getLocationsForAppointmentsMap(request.prisonCode!!))
        .thenReturn(
          mapOf(
            request.internalLocationId!! to appointmentLocation(
              request.internalLocationId!!,
              request.prisonCode!!,
            ),
          ),
        )
      whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.appointments.map { it.prisonerNumber!! }))
        .thenReturn(
          listOf(
            PrisonerSearchPrisonerFixture.instance(prisonerNumber = request.appointments[0].prisonerNumber!!, bookingId = 1, prisonId = "DIFFERENT"),
            PrisonerSearchPrisonerFixture.instance(prisonerNumber = request.appointments[1].prisonerNumber!!, bookingId = 2, prisonId = request.prisonCode),
          ),
        )

      assertThatThrownBy {
        service.createAppointmentSet(request, principal)
      }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Prisoner(s) with prisoner number(s) '${request.appointments[0].prisonerNumber}' not found, were inactive or are residents of a different prison.")

      verify(appointmentSeriesRepository, never()).saveAndFlush(any())
    }*/
  }
}
