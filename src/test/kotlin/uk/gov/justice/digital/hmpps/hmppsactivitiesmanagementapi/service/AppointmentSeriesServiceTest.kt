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
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.context.SecurityContextHolder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiUserClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentAttendee
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentFrequency
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSet
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCancelledReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSetCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentTierNotSpecified
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.userCaseLoads
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.CreateAppointmentsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeriesSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentSeriesCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentSetCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentCancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentHostRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSeriesRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSetRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentTierRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.NOT_SPECIFIED_APPOINTMENT_TIER_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_INSTANCE_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_SERIES_ID_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_SET_ID_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.CATEGORY_CODE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.DESCRIPTION_LENGTH_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EARLIEST_START_TIME_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.END_TIME_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_TIME_MS_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EXTRA_INFORMATION_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EXTRA_INFORMATION_LENGTH_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.HAS_DESCRIPTION_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.HAS_EXTRA_INFORMATION_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.INTERNAL_LOCATION_ID_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.IS_REPEAT_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.LATEST_END_TIME_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISONER_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISON_CODE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.REPEAT_COUNT_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.REPEAT_PERIOD_PROPERTY_KEY
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.Optional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentFrequency as AppointmentFrequencyModel

@ExtendWith(FakeSecurityContext::class)
class AppointmentSeriesServiceTest {
  private val appointmentSeriesRepository: AppointmentSeriesRepository = mock()
  private val appointmentTierRepository: AppointmentTierRepository = mock()
  private val appointmentHostRepository: AppointmentHostRepository = mock()
  private val appointmentCancellationReasonRepository: AppointmentCancellationReasonRepository = mock()
  private val appointmentSetRepository: AppointmentSetRepository = mock()
  private val referenceCodeService: ReferenceCodeService = mock()
  private val locationService: LocationService = mock()
  private val prisonApiUserClient: PrisonApiUserClient = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val auditService: AuditService = mock()
  private val createAppointmentsJob: CreateAppointmentsJob = mock()
  private lateinit var principal: Principal

  @Captor
  private lateinit var appointmentSeriesEntityCaptor: ArgumentCaptor<AppointmentSeries>

  @Captor
  private lateinit var appointmentSetEntityCaptor: ArgumentCaptor<AppointmentSet>

  @Captor
  private lateinit var telemetryPropertyMap: ArgumentCaptor<Map<String, String>>

  @Captor
  private lateinit var telemetryMetricsMap: ArgumentCaptor<Map<String, Double>>

  private val service = AppointmentSeriesService(
    appointmentSeriesRepository,
    appointmentTierRepository,
    appointmentHostRepository,
    appointmentCancellationReasonRepository,
    appointmentSetRepository,
    referenceCodeService,
    locationService,
    prisonerSearchApiClient,
    createAppointmentsJob,
    telemetryClient,
    auditService,
    maxSyncAppointmentInstanceActions = 14,
  )

  @BeforeEach
  fun setUp() {
    MockitoAnnotations.openMocks(this)
    principal = SecurityContextHolder.getContext().authentication
    addCaseloadIdToRequestHeader("TPR")
    whenever(appointmentTierRepository.findById(NOT_SPECIFIED_APPOINTMENT_TIER_ID)).thenReturn(Optional.of(appointmentTierNotSpecified()))
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
  fun `buildValidAppointmentSeriesEntity throws caseload access exception when requested prison code is not in user's case load`() {
    addCaseloadIdToRequestHeader("DIFFERENT")
    val request = appointmentSeriesCreateRequest()

    whenever(prisonerSearchApiClient.findByPrisonerNumbers(any())).thenReturn(Mono.just(emptyList()))

    assertThatThrownBy {
      service.buildValidAppointmentSeriesEntity(
        appointmentType = request.appointmentType,
        prisonCode = request.prisonCode!!,
        prisonerNumbers = request.prisonerNumbers,
        prisonerBookings = emptyMap(),
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

    whenever(prisonApiUserClient.getUserCaseLoads()).thenReturn(Mono.just(userCaseLoads(request.prisonCode!!)))
    whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT)).thenReturn(emptyMap())
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(any())).thenReturn(Mono.just(emptyList()))
    assertThatThrownBy {
      service.buildValidAppointmentSeriesEntity(
        appointmentType = request.appointmentType,
        prisonCode = request.prisonCode!!,
        prisonerNumbers = request.prisonerNumbers,
        prisonerBookings = emptyMap(),
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

    whenever(prisonApiUserClient.getUserCaseLoads()).thenReturn(Mono.just(userCaseLoads(request.prisonCode!!)))
    whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT))
      .thenReturn(mapOf(request.categoryCode!! to appointmentCategoryReferenceCode(request.categoryCode!!)))
    whenever(locationService.getLocationsForAppointmentsMap(request.prisonCode!!)).thenReturn(emptyMap())

    assertThatThrownBy {
      service.buildValidAppointmentSeriesEntity(
        appointmentType = request.appointmentType,
        prisonCode = request.prisonCode!!,
        prisonerNumbers = request.prisonerNumbers,
        prisonerBookings = emptyMap(),
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

    whenever(prisonApiUserClient.getUserCaseLoads()).thenReturn(Mono.just(userCaseLoads(request.prisonCode!!)))
    whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT))
      .thenReturn(mapOf(request.categoryCode!! to appointmentCategoryReferenceCode(request.categoryCode!!)))
    whenever(locationService.getLocationsForAppointmentsMap(request.prisonCode!!))
      .thenReturn(mapOf(request.internalLocationId!! to appointmentLocation(request.internalLocationId!!, request.prisonCode!!)))
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers)).thenReturn(Mono.just(emptyList()))

    assertThatThrownBy {
      service.buildValidAppointmentSeriesEntity(
        appointmentType = request.appointmentType,
        prisonCode = request.prisonCode!!,
        prisonerNumbers = request.prisonerNumbers,
        prisonerBookings = emptyMap(),
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
  fun`buildValidAppointmentSeriesEntity does not perform any validation if the appointment is a migration`() {
    val request = appointmentMigrateRequest()

    service.buildValidAppointmentSeriesEntity(
      appointmentType = AppointmentType.INDIVIDUAL,
      prisonCode = request.prisonCode!!,
      prisonerNumbers = listOf(request.prisonerNumber!!),
      prisonerBookings = mapOf(request.prisonerNumber!! to request.bookingId.toString()),
      categoryCode = request.categoryCode!!,
      appointmentTier = appointmentTierNotSpecified(),
      internalLocationId = request.internalLocationId,
      startDate = request.startDate,
      startTime = request.startTime,
      endTime = request.endTime,
      extraInformation = request.comment!!,
      createdBy = "MIGRATION.USER",
      isMigrated = true,
    )

    verify(times(0)) { referenceCodeService.getScheduleReasonsMap(any()) }
    verify(times(0)) { locationService.getLocationsForAppointmentsMap(any()) }
  }

  @Test
  fun`buildValidAppointmentSeriesEntity converts a blank custom name to null`() {
    val request = appointmentSeriesCreateRequest(customName = "    ")

    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers)).thenReturn(Mono.just(emptyList()))

    val appointment = service.buildValidAppointmentSeriesEntity(
      appointmentType = request.appointmentType,
      prisonCode = request.prisonCode!!,
      prisonerNumbers = request.prisonerNumbers,
      prisonerBookings = emptyMap(),
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
      isMigrated = true,
    )

    assertThat(appointment.customName).isNull()
  }

  @Test
  fun `createAppointmentSeries throws illegal argument exception when prisoner is not a resident of requested prison code`() {
    val request = appointmentSeriesCreateRequest()

    whenever(prisonApiUserClient.getUserCaseLoads()).thenReturn(Mono.just(userCaseLoads(request.prisonCode!!)))
    whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT))
      .thenReturn(mapOf(request.categoryCode!! to appointmentCategoryReferenceCode(request.categoryCode!!)))
    whenever(locationService.getLocationsForAppointmentsMap(request.prisonCode!!))
      .thenReturn(mapOf(request.internalLocationId!! to appointmentLocation(request.internalLocationId!!, request.prisonCode!!)))
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers))
      .thenReturn(Mono.just(listOf(PrisonerSearchPrisonerFixture.instance(prisonerNumber = request.prisonerNumbers.first(), prisonId = "DIFFERENT"))))

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

    whenever(prisonApiUserClient.getUserCaseLoads()).thenReturn(Mono.just(userCaseLoads(request.prisonCode!!)))
    whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT))
      .thenReturn(mapOf(request.categoryCode!! to appointmentCategoryReferenceCode(request.categoryCode!!)))
    whenever(locationService.getLocationsForAppointmentsMap(request.prisonCode!!))
      .thenReturn(mapOf(request.internalLocationId!! to appointmentLocation(request.internalLocationId!!, request.prisonCode!!)))
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers))
      .thenReturn(
        Mono.just(
          request.prisonerNumbers.map {
            PrisonerSearchPrisonerFixture.instance(
              prisonerNumber = it,
              bookingId = 1,
              prisonId = request.prisonCode!!,
            )
          },
        ),
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

    whenever(prisonApiUserClient.getUserCaseLoads()).thenReturn(Mono.just(userCaseLoads(request.prisonCode!!)))
    whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT))
      .thenReturn(mapOf(request.categoryCode!! to appointmentCategoryReferenceCode(request.categoryCode!!)))
    whenever(locationService.getLocationsForAppointmentsMap(request.prisonCode!!))
      .thenReturn(mapOf(request.internalLocationId!! to appointmentLocation(request.internalLocationId!!, request.prisonCode!!)))
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers))
      .thenReturn(
        Mono.just(
          listOf(
            PrisonerSearchPrisonerFixture.instance(prisonerNumber = request.prisonerNumbers.first(), bookingId = 1, prisonId = request.prisonCode!!),
          ),
        ),
      )
    whenever(appointmentSeriesRepository.saveAndFlush(appointmentSeriesEntityCaptor.capture())).thenReturn(appointmentSeriesEntity())

    service.createAppointmentSeries(request, principal)

    with(appointmentSeriesEntityCaptor.value) {
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

      with(telemetryPropertyMap) {
        assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
        assertThat(value[PRISON_CODE_PROPERTY_KEY]).isEqualTo("TPR")
        assertThat(value[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("1")
        assertThat(value[CATEGORY_CODE_PROPERTY_KEY]).isEqualTo("TEST")
        assertThat(value[HAS_DESCRIPTION_PROPERTY_KEY]).isEqualTo("true")
        assertThat(value[INTERNAL_LOCATION_ID_PROPERTY_KEY]).isEqualTo("123")
        assertThat(value[START_DATE_PROPERTY_KEY]).isEqualTo(startDate.toString())
        assertThat(value[START_TIME_PROPERTY_KEY]).isEqualTo("09:00")
        assertThat(value[END_TIME_PROPERTY_KEY]).isEqualTo("10:30")
        assertThat(value[IS_REPEAT_PROPERTY_KEY]).isEqualTo("false")
        assertThat(value[REPEAT_PERIOD_PROPERTY_KEY]).isEqualTo("")
        assertThat(value[REPEAT_COUNT_PROPERTY_KEY]).isEqualTo("")
        assertThat(value[HAS_EXTRA_INFORMATION_PROPERTY_KEY]).isEqualTo("true")
      }

      with(telemetryMetricsMap) {
        assertThat(value[PRISONER_COUNT_METRIC_KEY]).isEqualTo(1.0)
        assertThat(value[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(1.0)
        assertThat(value[DESCRIPTION_LENGTH_METRIC_KEY]).isEqualTo(23.0)
        assertThat(value[EXTRA_INFORMATION_LENGTH_METRIC_KEY]).isEqualTo(32.0)
        assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull
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

    whenever(prisonApiUserClient.getUserCaseLoads()).thenReturn(Mono.just(userCaseLoads(request.prisonCode!!)))
    whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT))
      .thenReturn(mapOf(request.categoryCode!! to appointmentCategoryReferenceCode(request.categoryCode!!)))
    whenever(locationService.getLocationsForAppointmentsMap(request.prisonCode!!))
      .thenReturn(mapOf(request.internalLocationId!! to appointmentLocation(request.internalLocationId!!, request.prisonCode!!)))
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers))
      .thenReturn(
        Mono.just(
          listOf(
            PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A12345BC", bookingId = 1, prisonId = request.prisonCode!!),
            PrisonerSearchPrisonerFixture.instance(prisonerNumber = "B23456CE", bookingId = 2, prisonId = request.prisonCode!!),
          ),
        ),
      )
    whenever(appointmentSeriesRepository.saveAndFlush(appointmentSeriesEntityCaptor.capture())).thenReturn(appointmentSeriesEntity())

    service.createAppointmentSeries(request, principal)

    with(appointmentSeriesEntityCaptor.value) {
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

    whenever(prisonApiUserClient.getUserCaseLoads()).thenReturn(Mono.just(userCaseLoads(request.prisonCode!!)))
    whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT))
      .thenReturn(mapOf(request.categoryCode!! to appointmentCategoryReferenceCode(request.categoryCode!!)))
    whenever(locationService.getLocationsForAppointmentsMap(request.prisonCode!!))
      .thenReturn(mapOf(request.internalLocationId!! to appointmentLocation(request.internalLocationId!!, request.prisonCode!!)))
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers))
      .thenReturn(
        Mono.just(
          listOf(
            PrisonerSearchPrisonerFixture.instance(prisonerNumber = request.prisonerNumbers.first(), bookingId = 1, prisonId = request.prisonCode!!),
          ),
        ),
      )
    whenever(appointmentSeriesRepository.saveAndFlush(appointmentSeriesEntityCaptor.capture())).thenReturn(appointmentSeriesEntity(frequency = AppointmentFrequency.WEEKLY, numberOfAppointments = 3))

    service.createAppointmentSeries(request, principal)

    with(appointmentSeriesEntityCaptor.value.appointments()) {
      assertThat(size).isEqualTo(3)
      assertThat(map { it.sequenceNumber }).isEqualTo(listOf(1, 2, 3))
    }
  }

  @Test
  fun `createAppointmentSeries for fifteen prisoners synchronously when it does not repeat creating fifteen appointment instances`() {
    val prisonerNumberToBookingIdMap = (1L..15L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val request = appointmentSeriesCreateRequest(appointmentType = AppointmentType.GROUP, prisonerNumbers = prisonerNumberToBookingIdMap.keys.toList(), schedule = null)

    whenever(prisonApiUserClient.getUserCaseLoads()).thenReturn(Mono.just(userCaseLoads(request.prisonCode!!)))
    whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT))
      .thenReturn(mapOf(request.categoryCode!! to appointmentCategoryReferenceCode(request.categoryCode!!)))
    whenever(locationService.getLocationsForAppointmentsMap(request.prisonCode!!))
      .thenReturn(mapOf(request.internalLocationId!! to appointmentLocation(request.internalLocationId!!, request.prisonCode!!)))
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers))
      .thenReturn(
        Mono.just(
          prisonerNumberToBookingIdMap.map {
            PrisonerSearchPrisonerFixture.instance(
              prisonerNumber = it.key,
              bookingId = it.value,
              prisonId = request.prisonCode!!,
            )
          },
        ),
      )
    whenever(appointmentSeriesRepository.saveAndFlush(appointmentSeriesEntityCaptor.capture())).thenReturn(appointmentSeriesEntity(prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap))

    service.createAppointmentSeries(request, principal)

    with(appointmentSeriesEntityCaptor.value) {
      appointments() hasSize 1
      appointments().flatMap { it.attendees() } hasSize 15
    }

    verify(createAppointmentsJob, never()).execute(any(), any())
  }

  @Test
  fun `createAppointmentSeries for fifteen prisoners synchronously when it repeats once creating fifteen appointment instances`() {
    val prisonerNumberToBookingIdMap = (1L..15L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val request = appointmentSeriesCreateRequest(appointmentType = AppointmentType.GROUP, prisonerNumbers = prisonerNumberToBookingIdMap.keys.toList(), schedule = AppointmentSeriesSchedule(AppointmentFrequencyModel.DAILY, 1))

    whenever(prisonApiUserClient.getUserCaseLoads()).thenReturn(Mono.just(userCaseLoads(request.prisonCode!!)))
    whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT))
      .thenReturn(mapOf(request.categoryCode!! to appointmentCategoryReferenceCode(request.categoryCode!!)))
    whenever(locationService.getLocationsForAppointmentsMap(request.prisonCode!!))
      .thenReturn(mapOf(request.internalLocationId!! to appointmentLocation(request.internalLocationId!!, request.prisonCode!!)))
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers))
      .thenReturn(
        Mono.just(
          prisonerNumberToBookingIdMap.map {
            PrisonerSearchPrisonerFixture.instance(
              prisonerNumber = it.key,
              bookingId = it.value,
              prisonId = request.prisonCode!!,
            )
          },
        ),
      )
    whenever(appointmentSeriesRepository.saveAndFlush(appointmentSeriesEntityCaptor.capture())).thenReturn(appointmentSeriesEntity(prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap))

    service.createAppointmentSeries(request, principal)

    with(appointmentSeriesEntityCaptor.value) {
      appointments() hasSize 1
      appointments().flatMap { it.attendees() } hasSize 15
    }

    verify(createAppointmentsJob, never()).execute(any(), any())
  }

  @Test
  fun `createAppointmentSeries for seven prisoners synchronously when it repeats twice creating fourteen appointment instances`() {
    val prisonerNumberToBookingIdMap = (1L..7L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val request = appointmentSeriesCreateRequest(appointmentType = AppointmentType.GROUP, prisonerNumbers = prisonerNumberToBookingIdMap.keys.toList(), schedule = AppointmentSeriesSchedule(AppointmentFrequencyModel.DAILY, 2))

    whenever(prisonApiUserClient.getUserCaseLoads()).thenReturn(Mono.just(userCaseLoads(request.prisonCode!!)))
    whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT))
      .thenReturn(mapOf(request.categoryCode!! to appointmentCategoryReferenceCode(request.categoryCode!!)))
    whenever(locationService.getLocationsForAppointmentsMap(request.prisonCode!!))
      .thenReturn(mapOf(request.internalLocationId!! to appointmentLocation(request.internalLocationId!!, request.prisonCode!!)))
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers))
      .thenReturn(
        Mono.just(
          prisonerNumberToBookingIdMap.map {
            PrisonerSearchPrisonerFixture.instance(
              prisonerNumber = it.key,
              bookingId = it.value,
              prisonId = request.prisonCode!!,
            )
          },
        ),
      )
    whenever(appointmentSeriesRepository.saveAndFlush(appointmentSeriesEntityCaptor.capture())).thenReturn(appointmentSeriesEntity(prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap))

    service.createAppointmentSeries(request, principal)

    with(appointmentSeriesEntityCaptor.value) {
      appointments() hasSize 2
      appointments().flatMap { it.attendees() } hasSize 14
    }

    verify(createAppointmentsJob, never()).execute(any(), any())
  }

  @Test
  fun `createAppointmentSeries for three prisoners asynchronously when it repeats five times creating fifteen appointment instances`() {
    val prisonerNumberToBookingIdMap = (1L..3L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val request = appointmentSeriesCreateRequest(appointmentType = AppointmentType.GROUP, prisonerNumbers = prisonerNumberToBookingIdMap.keys.toList(), schedule = AppointmentSeriesSchedule(AppointmentFrequencyModel.DAILY, 5))

    whenever(prisonApiUserClient.getUserCaseLoads()).thenReturn(Mono.just(userCaseLoads(request.prisonCode!!)))
    whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT))
      .thenReturn(mapOf(request.categoryCode!! to appointmentCategoryReferenceCode(request.categoryCode!!)))
    whenever(locationService.getLocationsForAppointmentsMap(request.prisonCode!!))
      .thenReturn(mapOf(request.internalLocationId!! to appointmentLocation(request.internalLocationId!!, request.prisonCode!!)))
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers))
      .thenReturn(
        Mono.just(
          prisonerNumberToBookingIdMap.map {
            PrisonerSearchPrisonerFixture.instance(
              prisonerNumber = it.key,
              bookingId = it.value,
              prisonId = request.prisonCode!!,
            )
          },
        ),
      )
    whenever(appointmentSeriesRepository.saveAndFlush(appointmentSeriesEntityCaptor.capture())).thenReturn(appointmentSeriesEntity(prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap))

    service.createAppointmentSeries(request, principal)

    with(appointmentSeriesEntityCaptor.value) {
      appointments() hasSize 1
      appointments().flatMap { it.attendees() } hasSize 3
    }

    verify(createAppointmentsJob).execute(1, prisonerNumberToBookingIdMap.map { it.key to it.value.toString() }.toMap())
  }

  @Test
  fun `create appointment set success`() {
    val request = appointmentSetCreateRequest()

    whenever(prisonApiUserClient.getUserCaseLoads()).thenReturn(Mono.just(userCaseLoads(request.prisonCode)))
    whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT))
      .thenReturn(mapOf(request.categoryCode to appointmentCategoryReferenceCode(request.categoryCode)))
    whenever(locationService.getLocationsForAppointmentsMap(request.prisonCode))
      .thenReturn(
        mapOf(
          request.internalLocationId to appointmentLocation(
            request.internalLocationId,
            request.prisonCode,
          ),
        ),
      )
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.appointments.map { it.prisonerNumber }))
      .thenReturn(
        Mono.just(
          listOf(
            PrisonerSearchPrisonerFixture.instance(prisonerNumber = request.appointments[0].prisonerNumber, bookingId = 1, prisonId = request.prisonCode),
            PrisonerSearchPrisonerFixture.instance(prisonerNumber = request.appointments[1].prisonerNumber, bookingId = 2, prisonId = request.prisonCode),
          ),
        ),
      )

    whenever(appointmentSetRepository.saveAndFlush(appointmentSetEntityCaptor.capture())).thenReturn(
      AppointmentSet(
        appointmentSetId = 1,
        prisonCode = request.prisonCode,
        categoryCode = request.categoryCode,
        customName = request.customName,
        appointmentTier = appointmentTierNotSpecified(),
        internalLocationId = request.internalLocationId,
        inCell = request.inCell,
        startDate = request.startDate,
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
      assertThat(createdBy).isEqualTo(DEFAULT_USERNAME)
      assertThat(appointmentSeries()).hasSize(2)
      assertThat(appointmentSeries()[0].appointments()[0].attendees()[0].prisonerNumber).isEqualTo("A1234BC")
      assertThat(appointmentSeries()[1].appointments()[0].attendees()[0].prisonerNumber).isEqualTo("A1234BD")

      appointmentSeries().forEach {
        assertThat(it.categoryCode).isEqualTo("TEST")
        assertThat(it.prisonCode).isEqualTo("TPR")
        assertThat(it.internalLocationId).isEqualTo(123)
        assertThat(it.inCell).isFalse
        assertThat(it.startDate).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(it.startTime).isEqualTo(LocalTime.of(13, 0))
        assertThat(it.endTime).isEqualTo(LocalTime.of(14, 30))
        assertThat(it.extraInformation).isEqualTo("Test comment")
        assertThat(it.customName).isEqualTo("Appointment description")
      }
    }

    with(telemetryPropertyMap) {
      assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
      assertThat(value[PRISON_CODE_PROPERTY_KEY]).isEqualTo("TPR")
      assertThat(value[APPOINTMENT_SET_ID_PROPERTY_KEY]).isEqualTo("1")
      assertThat(value[CATEGORY_CODE_PROPERTY_KEY]).isEqualTo("TEST")
      assertThat(value[HAS_DESCRIPTION_PROPERTY_KEY]).isEqualTo("true")
      assertThat(value[INTERNAL_LOCATION_ID_PROPERTY_KEY]).isEqualTo("123")
      assertThat(value[START_DATE_PROPERTY_KEY]).isEqualTo(request.startDate.toString())
      assertThat(value[EARLIEST_START_TIME_PROPERTY_KEY]).isEqualTo("09:00")
      assertThat(value[LATEST_END_TIME_PROPERTY_KEY]).isEqualTo("10:30")
    }

    with(telemetryMetricsMap) {
      assertThat(value[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(4.0)
      assertThat(value[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(4.0)
      assertThat(value[DESCRIPTION_LENGTH_METRIC_KEY]).isEqualTo(23.0)
      assertThat(value[EXTRA_INFORMATION_COUNT_METRIC_KEY]).isEqualTo(4.0)
      assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull
    }

    verify(auditService).logEvent(any<AppointmentSetCreatedEvent>())
  }

  @Test
  fun `create appointment set throws illegal argument exception when prisoner is not a resident of requested prison code`() {
    val request = appointmentSetCreateRequest()

    whenever(prisonApiUserClient.getUserCaseLoads()).thenReturn(Mono.just(userCaseLoads(request.prisonCode)))
    whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT))
      .thenReturn(mapOf(request.categoryCode to appointmentCategoryReferenceCode(request.categoryCode)))
    whenever(locationService.getLocationsForAppointmentsMap(request.prisonCode))
      .thenReturn(
        mapOf(
          request.internalLocationId to appointmentLocation(
            request.internalLocationId,
            request.prisonCode,
          ),
        ),
      )
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.appointments.map { it.prisonerNumber }))
      .thenReturn(
        Mono.just(
          listOf(
            PrisonerSearchPrisonerFixture.instance(prisonerNumber = request.appointments[0].prisonerNumber, bookingId = 1, prisonId = "DIFFERENT"),
            PrisonerSearchPrisonerFixture.instance(prisonerNumber = request.appointments[1].prisonerNumber, bookingId = 2, prisonId = request.prisonCode),
          ),
        ),
      )

    assertThatThrownBy {
      service.createAppointmentSet(request, principal)
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Prisoner(s) with prisoner number(s) '${request.appointments[0].prisonerNumber}' not found, were inactive or are residents of a different prison.")

    verify(appointmentSeriesRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `create appointment set fails if no appointments provided`() {
    val request = appointmentSetCreateRequest().copy(appointments = emptyList())

    assertThatThrownBy {
      service.createAppointmentSet(request, principal)
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("One or more appointments must be supplied.")

    verify(appointmentSeriesRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `migrateAppointment success`() {
    val request = appointmentMigrateRequest()
    whenever(appointmentSeriesRepository.saveAndFlush(appointmentSeriesEntityCaptor.capture())).thenReturn(appointmentSeriesEntity())

    service.migrateAppointment(request, principal)

    with(appointmentSeriesEntityCaptor.value) {
      assertThat(categoryCode).isEqualTo(request.categoryCode)
      assertThat(prisonCode).isEqualTo(request.prisonCode)
      assertThat(internalLocationId).isEqualTo(request.internalLocationId)
      assertThat(startDate).isEqualTo(request.startDate)
      assertThat(startTime).isEqualTo(request.startTime)
      assertThat(endTime).isEqualTo(request.endTime)
      assertThat(extraInformation).isEqualTo(request.comment)
      assertThat(customName).isNull()
      assertThat(createdTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(createdBy).isEqualTo(request.createdBy)
      assertThat(updatedTime).isNull()
      assertThat(updatedBy).isNull()
      assertThat(isMigrated).isTrue
      with(appointments()) {
        assertThat(size).isEqualTo(1)
        with(appointments().first()) {
          assertThat(categoryCode).isEqualTo(request.categoryCode)
          assertThat(prisonCode).isEqualTo(request.prisonCode)
          assertThat(internalLocationId).isEqualTo(request.internalLocationId)
          assertThat(startDate).isEqualTo(request.startDate)
          assertThat(startTime).isEqualTo(request.startTime)
          assertThat(endTime).isEqualTo(request.endTime)
          assertThat(extraInformation).isEqualTo(request.comment)
          assertThat(createdTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
          assertThat(createdBy).isEqualTo(request.createdBy)
          assertThat(updatedTime).isNull()
          assertThat(updatedBy).isNull()
          assertThat(isDeleted).isFalse
          with(attendees()) {
            assertThat(size).isEqualTo(1)
            with(first()) {
              assertThat(prisonerNumber).isEqualTo(request.prisonerNumber)
              assertThat(bookingId).isEqualTo(request.bookingId)
            }
          }
        }
      }
    }
  }

  @Test
  fun `migrateAppointment with specified created and createdBy success`() {
    val request = appointmentMigrateRequest(
      createdTime = LocalDateTime.of(2022, 10, 23, 10, 30),
      createdBy = "DPS.USER",
    )
    whenever(appointmentSeriesRepository.saveAndFlush(appointmentSeriesEntityCaptor.capture())).thenReturn(appointmentSeriesEntity())

    service.migrateAppointment(request, principal)

    with(appointmentSeriesEntityCaptor.value) {
      assertThat(createdTime).isEqualTo(request.created)
      assertThat(createdBy).isEqualTo(request.createdBy)
    }
  }

  @Test
  fun `migrateAppointment with specified updated and updatedBy success`() {
    val request = appointmentMigrateRequest(
      updatedTime = LocalDateTime.of(2022, 10, 23, 10, 30),
      updatedBy = "DPS.USER",
    )
    whenever(appointmentSeriesRepository.saveAndFlush(appointmentSeriesEntityCaptor.capture())).thenReturn(appointmentSeriesEntity())

    service.migrateAppointment(request, principal)

    with(appointmentSeriesEntityCaptor.value) {
      assertThat(updatedTime).isEqualTo(request.updated)
      assertThat(updatedBy).isEqualTo(request.updatedBy)
      with(appointments().first()) {
        assertThat(updatedTime).isEqualTo(request.updated)
        assertThat(updatedBy).isEqualTo(request.updatedBy)
      }
    }
  }

  @Test
  fun `migrateAppointment isCancelled defaults to false`() {
    val request = appointmentMigrateRequest(isCancelled = null)

    whenever(appointmentSeriesRepository.saveAndFlush(appointmentSeriesEntityCaptor.capture())).thenReturn(appointmentSeriesEntity())

    service.migrateAppointment(request, principal)

    with(appointmentSeriesEntityCaptor.value) {
      with(appointments().first()) {
        assertThat(cancelledTime).isNull()
        assertThat(cancellationReason).isNull()
        assertThat(cancelledBy).isNull()
      }
    }
  }

  @Test
  fun `migrateAppointment with isCancelled = true success`() {
    val request = appointmentMigrateRequest(isCancelled = true)
    val cancellationReason = appointmentCancelledReason()
    whenever(appointmentCancellationReasonRepository.findById(2)).thenReturn(Optional.of(cancellationReason))
    whenever(appointmentSeriesRepository.saveAndFlush(appointmentSeriesEntityCaptor.capture())).thenReturn(appointmentSeriesEntity())

    service.migrateAppointment(request, principal)

    with(appointmentSeriesEntityCaptor.value) {
      with(appointments().first()) {
        assertThat(cancelledTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
        assertThat(cancellationReason).isEqualTo(cancellationReason)
        assertThat(cancelledBy).isEqualTo(request.createdBy)
      }
    }
  }

  @Test
  fun `migrateAppointment with isCancelled = true will use updated and updated by if specified`() {
    val request = appointmentMigrateRequest(
      isCancelled = true,
      updatedTime = LocalDateTime.of(2022, 10, 23, 10, 30),
      updatedBy = "DPS.USER",
    )
    val cancellationReason = appointmentCancelledReason()
    whenever(appointmentCancellationReasonRepository.findById(2)).thenReturn(Optional.of(cancellationReason))
    whenever(appointmentSeriesRepository.saveAndFlush(appointmentSeriesEntityCaptor.capture())).thenReturn(appointmentSeriesEntity())

    service.migrateAppointment(request, principal)

    with(appointmentSeriesEntityCaptor.value) {
      with(appointments().first()) {
        assertThat(cancelledTime).isEqualTo(request.updated)
        assertThat(cancellationReason).isEqualTo(cancellationReason)
        assertThat(cancelledBy).isEqualTo(request.updatedBy)
      }
    }
  }

  @Test
  fun `migrateAppointment with isCancelled = true will use created and created by if specified and updated and updated by are null`() {
    val request = appointmentMigrateRequest(
      isCancelled = true,
      createdTime = LocalDateTime.of(2022, 10, 23, 10, 30),
      createdBy = "DPS.USER",
      updatedTime = null,
      updatedBy = null,
    )
    val cancellationReason = appointmentCancelledReason()
    whenever(appointmentCancellationReasonRepository.findById(2)).thenReturn(Optional.of(cancellationReason))
    whenever(appointmentSeriesRepository.saveAndFlush(appointmentSeriesEntityCaptor.capture())).thenReturn(appointmentSeriesEntity())

    service.migrateAppointment(request, principal)

    with(appointmentSeriesEntityCaptor.value) {
      with(appointments().first()) {
        assertThat(cancelledTime).isEqualTo(request.created)
        assertThat(cancellationReason).isEqualTo(cancellationReason)
        assertThat(cancelledBy).isEqualTo(request.createdBy)
      }
    }
  }
}
