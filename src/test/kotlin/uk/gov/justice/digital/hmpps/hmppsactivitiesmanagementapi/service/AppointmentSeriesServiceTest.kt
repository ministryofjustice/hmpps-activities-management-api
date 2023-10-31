package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.AdditionalAnswers
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentAttendee
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentCreateDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentTierNotSpecified
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.userDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.CreateAppointmentsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeriesDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeriesSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.UserSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentSeriesCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentHostRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSeriesRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentTierRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.NOT_SPECIFIED_APPOINTMENT_TIER_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_INSTANCE_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_SERIES_ID_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.CATEGORY_CODE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.CUSTOM_NAME_LENGTH_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.END_TIME_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_TIME_MS_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EXTRA_INFORMATION_LENGTH_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.FREQUENCY_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.HAS_CUSTOM_NAME_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.HAS_EXTRA_INFORMATION_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.INTERNAL_LOCATION_ID_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.IS_REPEAT_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.NUMBER_OF_APPOINTMENTS_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISONER_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISON_CODE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.START_DATE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.START_TIME_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.USER_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.CaseloadAccessException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.DEFAULT_USERNAME
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.FakeSecurityContext
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.addCaseloadIdToRequestHeader
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.clearCaseloadIdFromRequestHeader
import java.security.Principal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Optional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentFrequency as AppointmentFrequencyModel

@ExtendWith(FakeSecurityContext::class)
class AppointmentSeriesServiceTest {
  private val appointmentSeriesRepository: AppointmentSeriesRepository = mock()
  private val appointmentTierRepository: AppointmentTierRepository = mock()
  private val appointmentHostRepository: AppointmentHostRepository = mock()
  private val referenceCodeService: ReferenceCodeService = mock()
  private val locationService: LocationService = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val prisonApiClient: PrisonApiClient = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val auditService: AuditService = mock()
  private val appointmentCreateDomainService = spy(AppointmentCreateDomainService(mock(), mock(), mock(), TransactionHandler(), telemetryClient, auditService))
  private val createAppointmentsJob: CreateAppointmentsJob = mock()
  private lateinit var principal: Principal

  private val appointmentSeriesEntityCaptor = argumentCaptor<AppointmentSeries>()
  private var telemetryPropertyMap = argumentCaptor<Map<String, String>>()
  private var telemetryMetricsMap = argumentCaptor<Map<String, Double>>()

  private val service = AppointmentSeriesService(
    appointmentSeriesRepository,
    appointmentTierRepository,
    appointmentHostRepository,
    referenceCodeService,
    locationService,
    prisonerSearchApiClient,
    prisonApiClient,
    appointmentCreateDomainService,
    createAppointmentsJob,
    TransactionHandler(),
    maxSyncAppointmentInstanceActions = 14,
  )

  @BeforeEach
  fun setUp() {
    MockitoAnnotations.openMocks(this)
    principal = SecurityContextHolder.getContext().authentication
    addCaseloadIdToRequestHeader("TPR")
    whenever(appointmentTierRepository.findById(NOT_SPECIFIED_APPOINTMENT_TIER_ID)).thenReturn(Optional.of(appointmentTierNotSpecified()))
    whenever(appointmentSeriesRepository.saveAndFlush(appointmentSeriesEntityCaptor.capture())).thenAnswer(AdditionalAnswers.returnsFirstArg<AppointmentSeries>())
  }

  @AfterEach
  fun tearDown() {
    clearCaseloadIdFromRequestHeader()
  }

  @Test
  fun `getAppointmentSeriesById returns an appointment for known appointment series id`() {
    val entity = appointmentSeriesEntity()
    whenever(appointmentSeriesRepository.findById(1)).thenReturn(Optional.of(entity))
    assertThat(service.getAppointmentSeriesById(1)).isEqualTo(entity.toModel())
  }

  @Test
  fun `getAppointmentSeriesById throws entity not found exception for unknown appointment series id`() {
    assertThatThrownBy { service.getAppointmentSeriesById(0) }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Appointment Series 0 not found")
  }

  @Test
  fun `getAppointmentSeriesById throws caseload access exception if caseload id header does not match`() {
    addCaseloadIdToRequestHeader("WRONG")
    val entity = appointmentSeriesEntity()
    whenever(appointmentSeriesRepository.findById(entity.appointmentSeriesId)).thenReturn(Optional.of(entity))

    assertThatThrownBy { service.getAppointmentSeriesById(entity.appointmentSeriesId) }.isInstanceOf(CaseloadAccessException::class.java)
  }

  @Test
  fun `getAppointmentSeriesDetailsById returns mapped appointment series details for known appointment id`() {
    addCaseloadIdToRequestHeader("TPR")
    val entity = appointmentSeriesEntity()
    val appointmentEntity = entity.appointments().first()
    whenever(appointmentSeriesRepository.findById(entity.appointmentSeriesId)).thenReturn(Optional.of(entity))
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
    assertThat(service.getAppointmentSeriesDetailsById(1)).isEqualTo(
      AppointmentSeriesDetails(
        entity.appointmentSeriesId,
        AppointmentType.INDIVIDUAL,
        entity.prisonCode,
        "Appointment description (Test Category)",
        AppointmentCategorySummary(entity.categoryCode, "Test Category"),
        "Appointment description",
        AppointmentLocationSummary(entity.internalLocationId!!, "TPR", "Test Appointment Location User Description"),
        entity.inCell,
        entity.startDate,
        entity.startTime,
        entity.endTime,
        null,
        entity.extraInformation,
        entity.createdTime,
        UserSummary(1, "CREATE.USER", "CREATE", "USER"),
        entity.updatedTime,
        UserSummary(2, "UPDATE.USER", "UPDATE", "USER"),
        appointments = listOf(
          AppointmentSummary(
            appointmentEntity.appointmentId,
            1,
            appointmentEntity.startDate,
            appointmentEntity.startTime,
            appointmentEntity.endTime,
            isEdited = true,
            isCancelled = false,
          ),
        ),
      ),
    )
  }

  @Test
  fun `getAppointmentSeriesDetailsById throws entity not found exception for unknown appointment series id`() {
    assertThatThrownBy { service.getAppointmentSeriesDetailsById(-1) }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Appointment Series -1 not found")
  }

  @Test
  fun `getAppointmentSeriesDetailsById throws caseload access exception if caseload id header does not match`() {
    addCaseloadIdToRequestHeader("WRONG")
    val entity = appointmentSeriesEntity()
    whenever(appointmentSeriesRepository.findById(entity.appointmentSeriesId)).thenReturn(Optional.of(entity))

    assertThatThrownBy { service.getAppointmentSeriesDetailsById(entity.appointmentSeriesId) }.isInstanceOf(CaseloadAccessException::class.java)
  }

  /*@Test
  fun `buildValidAppointmentSeriesEntity throws caseload access exception when requested prison code is not in user's case load`() {
    addCaseloadIdToRequestHeader("DIFFERENT")
    val request = appointmentSeriesCreateRequest()

    whenever(prisonerSearchApiClient.findByPrisonerNumbers(any(), any())).thenReturn(emptyList())

    assertThatThrownBy {
      service.buildValidAppointmentSeriesEntity(
        appointmentType = request.appointmentType,
        prisonCode = request.prisonCode!!,
        prisonerNumbers = request.prisonerNumbers,
        prisonNumberBookingIdMap = emptyMap(),
        categoryCode = request.categoryCode!!,
        customName = request.customName,
        appointmentTier = appointmentTierNotSpecified(),
        internalLocationId = request.internalLocationId,
        inCell = request.inCell,
        startDate = request.startDate,
        startTime = request.startTime,
        endTime = request.endTime,
        repeat = request.schedule,
        extraInformation = request.extraInformation,
        createdBy = principal.name,
      )
    }.isInstanceOf(CaseloadAccessException::class.java)

    verify(appointmentSeriesRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `buildValidAppointmentSeriesEntity throws illegal argument exception when requested category code is not found`() {
    val request = appointmentSeriesCreateRequest()

    whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT)).thenReturn(emptyMap())
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(any(), any())).thenReturn(emptyList())
    assertThatThrownBy {
      service.buildValidAppointmentSeriesEntity(
        appointmentType = request.appointmentType,
        prisonCode = request.prisonCode!!,
        prisonerNumbers = request.prisonerNumbers,
        prisonNumberBookingIdMap = emptyMap(),
        categoryCode = request.categoryCode!!,
        customName = request.customName,
        appointmentTier = appointmentTierNotSpecified(),
        internalLocationId = request.internalLocationId,
        inCell = request.inCell,
        startDate = request.startDate,
        startTime = request.startTime,
        endTime = request.endTime,
        repeat = request.schedule,
        extraInformation = request.extraInformation,
        createdBy = principal.name,
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Appointment Category with code ${request.categoryCode} not found or is not active")

    verify(appointmentSeriesRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `buildValidAppointmentSeriesEntity throws illegal argument exception when inCell = false and requested internal location id is not found`() {
    val request = appointmentSeriesCreateRequest()

    whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT))
      .thenReturn(mapOf(request.categoryCode!! to appointmentCategoryReferenceCode(request.categoryCode!!)))
    whenever(locationService.getLocationsForAppointmentsMap(request.prisonCode!!)).thenReturn(emptyMap())

    assertThatThrownBy {
      service.buildValidAppointmentSeriesEntity(
        appointmentType = request.appointmentType,
        prisonCode = request.prisonCode!!,
        prisonerNumbers = request.prisonerNumbers,
        prisonNumberBookingIdMap = emptyMap(),
        categoryCode = request.categoryCode!!,
        customName = request.customName,
        appointmentTier = appointmentTierNotSpecified(),
        internalLocationId = request.internalLocationId,
        inCell = request.inCell,
        startDate = request.startDate,
        startTime = request.startTime,
        endTime = request.endTime,
        repeat = request.schedule,
        extraInformation = request.extraInformation,
        createdBy = principal.name,
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Appointment location with id ${request.internalLocationId} not found in prison '${request.prisonCode}'")

    verify(appointmentSeriesRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `buildValidAppointmentSeriesEntity throws illegal argument exception when prisoner is not found`() {
    val request = appointmentSeriesCreateRequest()

    whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT))
      .thenReturn(mapOf(request.categoryCode!! to appointmentCategoryReferenceCode(request.categoryCode!!)))
    whenever(locationService.getLocationsForAppointmentsMap(request.prisonCode!!))
      .thenReturn(mapOf(request.internalLocationId!! to appointmentLocation(request.internalLocationId!!, request.prisonCode!!)))
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers)).thenReturn(emptyList())

    assertThatThrownBy {
      service.buildValidAppointmentSeriesEntity(
        appointmentType = request.appointmentType,
        prisonCode = request.prisonCode!!,
        prisonerNumbers = request.prisonerNumbers,
        prisonNumberBookingIdMap = emptyMap(),
        categoryCode = request.categoryCode!!,
        customName = request.customName,
        appointmentTier = appointmentTierNotSpecified(),
        internalLocationId = request.internalLocationId,
        inCell = request.inCell,
        startDate = request.startDate,
        startTime = request.startTime,
        endTime = request.endTime,
        repeat = request.schedule,
        extraInformation = request.extraInformation,
        createdBy = principal.name,
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Prisoner(s) with prisoner number(s) '${request.prisonerNumbers.first()}' not found, were inactive or are residents of a different prison.")

    verify(appointmentSeriesRepository, never()).saveAndFlush(any())
  }

  @Test
  fun`buildValidAppointmentSeriesEntity converts a blank custom name to null`() {
    val request = appointmentSeriesCreateRequest(customName = "    ")

    whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT))
      .thenReturn(mapOf(request.categoryCode!! to appointmentCategoryReferenceCode(request.categoryCode!!)))
    whenever(locationService.getLocationsForAppointmentsMap(request.prisonCode!!))
      .thenReturn(mapOf(request.internalLocationId!! to appointmentLocation(request.internalLocationId!!, request.prisonCode!!)))
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers))
      .thenReturn(
        listOf(
          PrisonerSearchPrisonerFixture.instance(prisonerNumber = request.prisonerNumbers.first(), bookingId = 1, prisonId = request.prisonCode!!),
        ),
      )

    val appointment = service.buildValidAppointmentSeriesEntity(
      appointmentType = request.appointmentType,
      prisonCode = request.prisonCode!!,
      prisonerNumbers = request.prisonerNumbers,
      prisonNumberBookingIdMap = mapOf(request.prisonerNumbers.first() to 1),
      categoryCode = request.categoryCode!!,
      customName = request.customName,
      appointmentTier = appointmentTierNotSpecified(),
      internalLocationId = request.internalLocationId,
      inCell = request.inCell,
      startDate = request.startDate,
      startTime = request.startTime,
      endTime = request.endTime,
      repeat = request.schedule,
      extraInformation = request.extraInformation,
      createdBy = principal.name,
    )

    assertThat(appointment.customName).isNull()
  }*/

  @Test
  fun `createAppointmentSeries throws illegal argument exception when prisoner is not a resident of requested prison code`() {
    val request = appointmentSeriesCreateRequest()

    whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT))
      .thenReturn(mapOf(request.categoryCode!! to appointmentCategoryReferenceCode(request.categoryCode!!)))
    whenever(locationService.getLocationsForAppointmentsMap(request.prisonCode!!))
      .thenReturn(mapOf(request.internalLocationId!! to appointmentLocation(request.internalLocationId!!, request.prisonCode!!)))
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers))
      .thenReturn(listOf(PrisonerSearchPrisonerFixture.instance(prisonerNumber = request.prisonerNumbers.first(), prisonId = "DIFFERENT")))

    assertThatThrownBy {
      service.createAppointmentSeries(request, principal)
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Prisoner(s) with prisoner number(s) '${request.prisonerNumbers.first()}' not found, were inactive or are residents of a different prison.")

    verify(appointmentSeriesRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `createAppointmentSeries group repeat appointment series throws error if appointment instances exceeds 20,000`() {
    val prisonerList = MutableList(60) { prisoner -> "A11${prisoner}BC" }
    val request = appointmentSeriesCreateRequest(
      prisonerNumbers = prisonerList,
      schedule = AppointmentSeriesSchedule(AppointmentFrequencyModel.DAILY, 350),
    )

    whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT))
      .thenReturn(mapOf(request.categoryCode!! to appointmentCategoryReferenceCode(request.categoryCode!!)))
    whenever(locationService.getLocationsForAppointmentsMap(request.prisonCode!!))
      .thenReturn(mapOf(request.internalLocationId!! to appointmentLocation(request.internalLocationId!!, request.prisonCode!!)))
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers))
      .thenReturn(
        request.prisonerNumbers.map {
          PrisonerSearchPrisonerFixture.instance(
            prisonerNumber = it,
            bookingId = 1,
            prisonId = request.prisonCode!!,
          )
        },
      )

    assertThatThrownBy {
      service.createAppointmentSeries(request, principal)
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("You cannot schedule more than 333 appointments for this number of attendees.")

    verify(appointmentSeriesRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `createAppointmentSeries single appointment single prisoner success`() {
    val request = appointmentSeriesCreateRequest()

    whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT))
      .thenReturn(mapOf(request.categoryCode!! to appointmentCategoryReferenceCode(request.categoryCode!!)))
    whenever(locationService.getLocationsForAppointmentsMap(request.prisonCode!!))
      .thenReturn(mapOf(request.internalLocationId!! to appointmentLocation(request.internalLocationId!!, request.prisonCode!!)))
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers))
      .thenReturn(
        listOf(
          PrisonerSearchPrisonerFixture.instance(prisonerNumber = request.prisonerNumbers.first(), bookingId = 1, prisonId = request.prisonCode!!),
        ),
      )

    service.createAppointmentSeries(request, principal)

    with(appointmentSeriesEntityCaptor.firstValue) {
      assertThat(categoryCode).isEqualTo(request.categoryCode)
      assertThat(prisonCode).isEqualTo(request.prisonCode)
      assertThat(internalLocationId).isEqualTo(request.internalLocationId)
      assertThat(inCell).isEqualTo(request.inCell)
      assertThat(startDate).isEqualTo(request.startDate)
      assertThat(startTime).isEqualTo(request.startTime)
      assertThat(endTime).isEqualTo(request.endTime)
      assertThat(extraInformation).isEqualTo(request.extraInformation)
      assertThat(createdTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(createdBy).isEqualTo(DEFAULT_USERNAME)
      assertThat(updatedTime).isNull()
      assertThat(updatedBy).isNull()
      with(appointments()) {
        assertThat(size).isEqualTo(1)
        with(appointments().first()) {
          assertThat(categoryCode).isEqualTo(request.categoryCode)
          assertThat(prisonCode).isEqualTo(request.prisonCode)
          assertThat(internalLocationId).isEqualTo(request.internalLocationId)
          assertThat(inCell).isEqualTo(request.inCell)
          assertThat(startDate).isEqualTo(request.startDate)
          assertThat(startTime).isEqualTo(request.startTime)
          assertThat(endTime).isEqualTo(request.endTime)
          assertThat(extraInformation).isEqualTo(request.extraInformation)
          assertThat(createdTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
          assertThat(createdBy).isEqualTo(DEFAULT_USERNAME)
          assertThat(updatedTime).isNull()
          assertThat(updatedBy).isNull()
          assertThat(isDeleted).isFalse
          with(attendees()) {
            assertThat(size).isEqualTo(1)
            with(first()) {
              assertThat(prisonerNumber).isEqualTo(request.prisonerNumbers.first())
              assertThat(bookingId).isEqualTo(1)
            }
          }
        }
      }

      verify(telemetryClient).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_SERIES_CREATED.value),
        telemetryPropertyMap.capture(),
        telemetryMetricsMap.capture(),
      )

      with(telemetryPropertyMap.firstValue) {
        assertThat(this[USER_PROPERTY_KEY]).isEqualTo(principal.name)
        assertThat(this[PRISON_CODE_PROPERTY_KEY]).isEqualTo("TPR")
        assertThat(this[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("0")
        assertThat(this[CATEGORY_CODE_PROPERTY_KEY]).isEqualTo("TEST")
        assertThat(this[HAS_CUSTOM_NAME_PROPERTY_KEY]).isEqualTo("true")
        assertThat(this[INTERNAL_LOCATION_ID_PROPERTY_KEY]).isEqualTo("123")
        assertThat(this[START_DATE_PROPERTY_KEY]).isEqualTo(startDate.toString())
        assertThat(this[START_TIME_PROPERTY_KEY]).isEqualTo("13:00")
        assertThat(this[END_TIME_PROPERTY_KEY]).isEqualTo("14:30")
        assertThat(this[IS_REPEAT_PROPERTY_KEY]).isEqualTo("false")
        assertThat(this[FREQUENCY_PROPERTY_KEY]).isEqualTo("")
        assertThat(this[NUMBER_OF_APPOINTMENTS_PROPERTY_KEY]).isEqualTo("")
        assertThat(this[HAS_EXTRA_INFORMATION_PROPERTY_KEY]).isEqualTo("true")
      }

      with(telemetryMetricsMap.firstValue) {
        assertThat(this[PRISONER_COUNT_METRIC_KEY]).isEqualTo(1.0)
        assertThat(this[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(1.0)
        assertThat(this[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(1.0)
        assertThat(this[CUSTOM_NAME_LENGTH_METRIC_KEY]).isEqualTo(23.0)
        assertThat(this[EXTRA_INFORMATION_LENGTH_METRIC_KEY]).isEqualTo(25.0)
        assertThat(this[EVENT_TIME_MS_METRIC_KEY]).isNotNull
      }

      verify(auditService).logEvent(any<AppointmentSeriesCreatedEvent>())
    }
  }

  @Test
  fun `createAppointmentSeries group appointment two prisoners success`() {
    val request = appointmentSeriesCreateRequest(
      appointmentType = AppointmentType.GROUP,
      prisonerNumbers = listOf("A12345BC", "B23456CE"),
    )

    whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT))
      .thenReturn(mapOf(request.categoryCode!! to appointmentCategoryReferenceCode(request.categoryCode!!)))
    whenever(locationService.getLocationsForAppointmentsMap(request.prisonCode!!))
      .thenReturn(mapOf(request.internalLocationId!! to appointmentLocation(request.internalLocationId!!, request.prisonCode!!)))
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers))
      .thenReturn(
        listOf(
          PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A12345BC", bookingId = 1, prisonId = request.prisonCode!!),
          PrisonerSearchPrisonerFixture.instance(prisonerNumber = "B23456CE", bookingId = 2, prisonId = request.prisonCode!!),
        ),
      )

    service.createAppointmentSeries(request, principal)

    with(appointmentSeriesEntityCaptor.firstValue) {
      with(appointments()) {
        assertThat(size).isEqualTo(1)
        with(appointments().first()) {
          assertThat(attendees()).containsAll(
            listOf(
              AppointmentAttendee(
                appointmentAttendeeId = 0,
                appointment = this,
                prisonerNumber = "A12345BC",
                bookingId = 1,
              ),
              AppointmentAttendee(
                appointmentAttendeeId = 0,
                appointment = this,
                prisonerNumber = "B23456CE",
                bookingId = 2,
              ),
            ),
          )
        }
      }
    }
  }

  @Test
  fun `createAppointmentSeries individual repeat appointment success`() {
    val request = appointmentSeriesCreateRequest(schedule = AppointmentSeriesSchedule(AppointmentFrequencyModel.WEEKLY, 3))

    whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT))
      .thenReturn(mapOf(request.categoryCode!! to appointmentCategoryReferenceCode(request.categoryCode!!)))
    whenever(locationService.getLocationsForAppointmentsMap(request.prisonCode!!))
      .thenReturn(mapOf(request.internalLocationId!! to appointmentLocation(request.internalLocationId!!, request.prisonCode!!)))
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers))
      .thenReturn(
        listOf(
          PrisonerSearchPrisonerFixture.instance(prisonerNumber = request.prisonerNumbers.first(), bookingId = 1, prisonId = request.prisonCode!!),
        ),
      )

    service.createAppointmentSeries(request, principal)

    with(appointmentSeriesEntityCaptor.firstValue.appointments()) {
      assertThat(size).isEqualTo(3)
      assertThat(map { it.sequenceNumber }).isEqualTo(listOf(1, 2, 3))
    }
  }

  @Test
  fun `createAppointmentSeries for fifteen prisoners synchronously when it does not repeat creating fifteen appointment instances`() {
    val prisonerNumberToBookingIdMap = (1L..15L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val request = appointmentSeriesCreateRequest(appointmentType = AppointmentType.GROUP, prisonerNumbers = prisonerNumberToBookingIdMap.keys.toList(), schedule = null)

    whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT))
      .thenReturn(mapOf(request.categoryCode!! to appointmentCategoryReferenceCode(request.categoryCode!!)))
    whenever(locationService.getLocationsForAppointmentsMap(request.prisonCode!!))
      .thenReturn(mapOf(request.internalLocationId!! to appointmentLocation(request.internalLocationId!!, request.prisonCode!!)))
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers))
      .thenReturn(
        prisonerNumberToBookingIdMap.map {
          PrisonerSearchPrisonerFixture.instance(
            prisonerNumber = it.key,
            bookingId = it.value,
            prisonId = request.prisonCode!!,
          )
        },
      )

    service.createAppointmentSeries(request, principal)

    with(appointmentSeriesEntityCaptor.firstValue) {
      appointments() hasSize 1
      appointments().flatMap { it.attendees() } hasSize 15
    }

    verify(createAppointmentsJob, never()).execute(any(), any(), any(), any(), any())
  }

  @Test
  fun `createAppointmentSeries for fifteen prisoners synchronously when it repeats once creating fifteen appointment instances`() {
    val prisonerNumberToBookingIdMap = (1L..15L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val request = appointmentSeriesCreateRequest(appointmentType = AppointmentType.GROUP, prisonerNumbers = prisonerNumberToBookingIdMap.keys.toList(), schedule = AppointmentSeriesSchedule(AppointmentFrequencyModel.DAILY, 1))

    whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT))
      .thenReturn(mapOf(request.categoryCode!! to appointmentCategoryReferenceCode(request.categoryCode!!)))
    whenever(locationService.getLocationsForAppointmentsMap(request.prisonCode!!))
      .thenReturn(mapOf(request.internalLocationId!! to appointmentLocation(request.internalLocationId!!, request.prisonCode!!)))
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers))
      .thenReturn(
        prisonerNumberToBookingIdMap.map {
          PrisonerSearchPrisonerFixture.instance(
            prisonerNumber = it.key,
            bookingId = it.value,
            prisonId = request.prisonCode!!,
          )
        },
      )

    service.createAppointmentSeries(request, principal)

    with(appointmentSeriesEntityCaptor.firstValue) {
      appointments() hasSize 1
      appointments().flatMap { it.attendees() } hasSize 15
    }

    verify(createAppointmentsJob, never()).execute(any(), any(), any(), any(), any())
  }

  @Test
  fun `createAppointmentSeries for seven prisoners synchronously when it repeats twice creating fourteen appointment instances`() {
    val prisonerNumberToBookingIdMap = (1L..7L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val request = appointmentSeriesCreateRequest(appointmentType = AppointmentType.GROUP, prisonerNumbers = prisonerNumberToBookingIdMap.keys.toList(), schedule = AppointmentSeriesSchedule(AppointmentFrequencyModel.DAILY, 2))

    whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT))
      .thenReturn(mapOf(request.categoryCode!! to appointmentCategoryReferenceCode(request.categoryCode!!)))
    whenever(locationService.getLocationsForAppointmentsMap(request.prisonCode!!))
      .thenReturn(mapOf(request.internalLocationId!! to appointmentLocation(request.internalLocationId!!, request.prisonCode!!)))
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers))
      .thenReturn(
        prisonerNumberToBookingIdMap.map {
          PrisonerSearchPrisonerFixture.instance(
            prisonerNumber = it.key,
            bookingId = it.value,
            prisonId = request.prisonCode!!,
          )
        },
      )

    service.createAppointmentSeries(request, principal)

    with(appointmentSeriesEntityCaptor.firstValue) {
      appointments() hasSize 2
      appointments().flatMap { it.attendees() } hasSize 14
    }

    verify(createAppointmentsJob, never()).execute(any(), any(), any(), any(), any())
  }

  @Test
  fun `createAppointmentSeries for three prisoners asynchronously when it repeats five times creating fifteen appointment instances`() {
    val prisonerNumberToBookingIdMap = (1L..3L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val request = appointmentSeriesCreateRequest(appointmentType = AppointmentType.GROUP, prisonerNumbers = prisonerNumberToBookingIdMap.keys.toList(), schedule = AppointmentSeriesSchedule(AppointmentFrequencyModel.DAILY, 5))

    whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT))
      .thenReturn(mapOf(request.categoryCode!! to appointmentCategoryReferenceCode(request.categoryCode!!)))
    whenever(locationService.getLocationsForAppointmentsMap(request.prisonCode!!))
      .thenReturn(mapOf(request.internalLocationId!! to appointmentLocation(request.internalLocationId!!, request.prisonCode!!)))
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers))
      .thenReturn(
        prisonerNumberToBookingIdMap.map {
          PrisonerSearchPrisonerFixture.instance(
            prisonerNumber = it.key,
            bookingId = it.value,
            prisonId = request.prisonCode!!,
          )
        },
      )

    service.createAppointmentSeries(request, principal)

    with(appointmentSeriesEntityCaptor.firstValue) {
      appointments() hasSize 1
      appointments().flatMap { it.attendees() } hasSize 3

      verify(createAppointmentsJob).execute(
        eq(appointmentSeriesId),
        eq(prisonerNumberToBookingIdMap.map { it.key to it.value }.toMap()),
        any(),
        eq("Test Category"),
        eq("Test Appointment Location User Description"),
      )
    }
  }
}
