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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentCancellationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentRepeatPeriod
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.BulkAppointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.bulkAppointmentRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.userCaseLoads
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.CreateAppointmentOccurrencesJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrenceAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentRepeat
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentCancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.BulkAppointmentRepository
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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentRepeatPeriod as AppointmentRepeatPeriodModel

@ExtendWith(FakeSecurityContext::class)
class AppointmentServiceTest {
  private val appointmentRepository: AppointmentRepository = mock()
  private val appointmentCancellationReasonRepository: AppointmentCancellationReasonRepository = mock()
  private val bulkAppointmentRepository: BulkAppointmentRepository = mock()
  private val referenceCodeService: ReferenceCodeService = mock()
  private val locationService: LocationService = mock()
  private val prisonApiUserClient: PrisonApiUserClient = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val createAppointmentOccurrencesJob: CreateAppointmentOccurrencesJob = mock()
  private lateinit var principal: Principal

  @Captor
  private lateinit var appointmentEntityCaptor: ArgumentCaptor<Appointment>

  @Captor
  private lateinit var bulkAppointmentEntityCaptor: ArgumentCaptor<BulkAppointment>

  @Captor
  private lateinit var telemetryPropertyMap: ArgumentCaptor<Map<String, String>>

  @Captor
  private lateinit var telemetryMetricsMap: ArgumentCaptor<Map<String, Double>>

  private val service = AppointmentService(
    appointmentRepository,
    appointmentCancellationReasonRepository,
    bulkAppointmentRepository,
    referenceCodeService,
    locationService,
    prisonerSearchApiClient,
    createAppointmentOccurrencesJob,
    telemetryClient,
    maxSyncAppointmentInstanceActions = 14,
  )

  @BeforeEach
  fun setUp() {
    MockitoAnnotations.openMocks(this)
    principal = SecurityContextHolder.getContext().authentication
    addCaseloadIdToRequestHeader("TPR")
  }

  @AfterEach
  fun tearDown() {
    clearCaseloadIdFromRequestHeader()
  }

  @Test
  fun `getAppointmentById returns an appointment for known appointment id`() {
    val entity = appointmentEntity()
    whenever(appointmentRepository.findById(1)).thenReturn(Optional.of(entity))
    assertThat(service.getAppointmentById(1)).isEqualTo(entity.toModel())
  }

  @Test
  fun `getAppointmentById throws entity not found exception for unknown appointment id`() {
    assertThatThrownBy { service.getAppointmentById(0) }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Appointment 0 not found")
  }

  @Test
  fun `buildValidAppointmentEntity throws caseload access exception when requested prison code is not in user's case load`() {
    addCaseloadIdToRequestHeader("DIFFERENT")
    val request = appointmentCreateRequest()

    whenever(prisonerSearchApiClient.findByPrisonerNumbers(any())).thenReturn(Mono.just(emptyList()))

    assertThatThrownBy {
      service.buildValidAppointmentEntity(
        appointmentType = request.appointmentType,
        prisonCode = request.prisonCode!!,
        prisonerNumbers = request.prisonerNumbers,
        prisonerBookings = emptyMap(),
        categoryCode = request.categoryCode,
        appointmentDescription = request.appointmentDescription,
        internalLocationId = request.internalLocationId,
        inCell = request.inCell,
        startDate = request.startDate,
        startTime = request.startTime,
        endTime = request.endTime,
        repeat = request.repeat,
        comment = request.comment,
        createdBy = principal.name,
      )
    }.isInstanceOf(CaseloadAccessException::class.java)

    verify(appointmentRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `buildValidAppointmentEntity throws illegal argument exception when requested category code is not found`() {
    val request = appointmentCreateRequest()

    whenever(prisonApiUserClient.getUserCaseLoads()).thenReturn(Mono.just(userCaseLoads(request.prisonCode!!)))
    whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT)).thenReturn(emptyMap())
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(any())).thenReturn(Mono.just(emptyList()))
    assertThatThrownBy {
      service.buildValidAppointmentEntity(
        appointmentType = request.appointmentType,
        prisonCode = request.prisonCode!!,
        prisonerNumbers = request.prisonerNumbers,
        prisonerBookings = emptyMap(),
        categoryCode = request.categoryCode,
        appointmentDescription = request.appointmentDescription,
        internalLocationId = request.internalLocationId,
        inCell = request.inCell,
        startDate = request.startDate,
        startTime = request.startTime,
        endTime = request.endTime,
        repeat = request.repeat,
        comment = request.comment,
        createdBy = principal.name,
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Appointment Category with code ${request.categoryCode} not found or is not active")

    verify(appointmentRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `buildValidAppointmentEntity throws illegal argument exception when inCell = false and requested internal location id is not found`() {
    val request = appointmentCreateRequest()

    whenever(prisonApiUserClient.getUserCaseLoads()).thenReturn(Mono.just(userCaseLoads(request.prisonCode!!)))
    whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT))
      .thenReturn(mapOf(request.categoryCode!! to appointmentCategoryReferenceCode(request.categoryCode!!)))
    whenever(locationService.getLocationsForAppointmentsMap(request.prisonCode!!)).thenReturn(emptyMap())

    assertThatThrownBy {
      service.buildValidAppointmentEntity(
        appointmentType = request.appointmentType,
        prisonCode = request.prisonCode!!,
        prisonerNumbers = request.prisonerNumbers,
        prisonerBookings = emptyMap(),
        categoryCode = request.categoryCode,
        appointmentDescription = request.appointmentDescription,
        internalLocationId = request.internalLocationId,
        inCell = request.inCell,
        startDate = request.startDate,
        startTime = request.startTime,
        endTime = request.endTime,
        repeat = request.repeat,
        comment = request.comment,
        createdBy = principal.name,
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Appointment location with id ${request.internalLocationId} not found in prison '${request.prisonCode}'")

    verify(appointmentRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `buildValidAppointmentEntity throws illegal argument exception when prisoner is not found`() {
    val request = appointmentCreateRequest()

    whenever(prisonApiUserClient.getUserCaseLoads()).thenReturn(Mono.just(userCaseLoads(request.prisonCode!!)))
    whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT))
      .thenReturn(mapOf(request.categoryCode!! to appointmentCategoryReferenceCode(request.categoryCode!!)))
    whenever(locationService.getLocationsForAppointmentsMap(request.prisonCode!!))
      .thenReturn(mapOf(request.internalLocationId!! to appointmentLocation(request.internalLocationId!!, request.prisonCode!!)))
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers)).thenReturn(Mono.just(emptyList()))

    assertThatThrownBy {
      service.buildValidAppointmentEntity(
        appointmentType = request.appointmentType,
        prisonCode = request.prisonCode!!,
        prisonerNumbers = request.prisonerNumbers,
        prisonerBookings = emptyMap(),
        categoryCode = request.categoryCode,
        appointmentDescription = request.appointmentDescription,
        internalLocationId = request.internalLocationId,
        inCell = request.inCell,
        startDate = request.startDate,
        startTime = request.startTime,
        endTime = request.endTime,
        repeat = request.repeat,
        comment = request.comment,
        createdBy = principal.name,
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Prisoner(s) with prisoner number(s) '${request.prisonerNumbers.first()}' not found, were inactive or are residents of a different prison.")

    verify(appointmentRepository, never()).saveAndFlush(any())
  }

  @Test
  fun`buildValidAppointmentEntity does not perform any validation if the appointment is a migration`() {
    val request = appointmentMigrateRequest()

    service.buildValidAppointmentEntity(
      appointmentType = AppointmentType.INDIVIDUAL,
      prisonCode = request.prisonCode!!,
      prisonerNumbers = listOf(request.prisonerNumber!!),
      prisonerBookings = mapOf(request.prisonerNumber!! to request.bookingId.toString()),
      categoryCode = request.categoryCode,
      internalLocationId = request.internalLocationId,
      startDate = request.startDate,
      startTime = request.startTime,
      endTime = request.endTime,
      comment = request.comment!!,
      createdBy = "MIGRATION.USER",
      isMigrated = true,
    )

    verify(times(0)) { prisonApiUserClient.getUserCaseLoads() }
    verify(times(0)) { referenceCodeService.getScheduleReasonsMap(any()) }
    verify(times(0)) { locationService.getLocationsForAppointmentsMap(any()) }
  }

  @Test
  fun`buildValidAppointmentEntity converts a blank appointment description to null`() {
    val request = appointmentCreateRequest(appointmentDescription = "    ")

    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers)).thenReturn(Mono.just(emptyList()))

    val appointment = service.buildValidAppointmentEntity(
      appointmentType = request.appointmentType,
      prisonCode = request.prisonCode!!,
      prisonerNumbers = request.prisonerNumbers,
      prisonerBookings = emptyMap(),
      categoryCode = request.categoryCode,
      appointmentDescription = request.appointmentDescription,
      internalLocationId = request.internalLocationId,
      inCell = request.inCell,
      startDate = request.startDate,
      startTime = request.startTime,
      endTime = request.endTime,
      repeat = request.repeat,
      comment = request.comment,
      createdBy = principal.name,
      isMigrated = true,
    )

    assertThat(appointment.appointmentDescription).isNull()
  }

  @Test
  fun `createAppointment throws illegal argument exception when prisoner is not a resident of requested prison code`() {
    val request = appointmentCreateRequest()

    whenever(prisonApiUserClient.getUserCaseLoads()).thenReturn(Mono.just(userCaseLoads(request.prisonCode!!)))
    whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT))
      .thenReturn(mapOf(request.categoryCode!! to appointmentCategoryReferenceCode(request.categoryCode!!)))
    whenever(locationService.getLocationsForAppointmentsMap(request.prisonCode!!))
      .thenReturn(mapOf(request.internalLocationId!! to appointmentLocation(request.internalLocationId!!, request.prisonCode!!)))
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers))
      .thenReturn(Mono.just(listOf(PrisonerSearchPrisonerFixture.instance(prisonerNumber = request.prisonerNumbers.first(), prisonId = "DIFFERENT"))))

    assertThatThrownBy {
      service.createAppointment(request, principal)
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Prisoner(s) with prisoner number(s) '${request.prisonerNumbers.first()}' not found, were inactive or are residents of a different prison.")

    verify(appointmentRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `createAppointment group repeat appointment throws error if occurrence allocations exceeds 20,000`() {
    val prisonerList = MutableList(60) { prisoner -> "A11${prisoner}BC" }
    val request = appointmentCreateRequest(
      prisonerNumbers = prisonerList,
      repeat = AppointmentRepeat(AppointmentRepeatPeriodModel.DAILY, 350),
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
      service.createAppointment(request, principal)
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("You cannot schedule more than 333 appointments for this number of attendees.")

    verify(appointmentRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `createAppointment single appointment single prisoner success`() {
    val request = appointmentCreateRequest()

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
    whenever(appointmentRepository.saveAndFlush(appointmentEntityCaptor.capture())).thenReturn(appointmentEntity())

    service.createAppointment(request, principal)

    with(appointmentEntityCaptor.value) {
      assertThat(categoryCode).isEqualTo(request.categoryCode)
      assertThat(prisonCode).isEqualTo(request.prisonCode)
      assertThat(internalLocationId).isEqualTo(request.internalLocationId)
      assertThat(inCell).isEqualTo(request.inCell)
      assertThat(startDate).isEqualTo(request.startDate)
      assertThat(startTime).isEqualTo(request.startTime)
      assertThat(endTime).isEqualTo(request.endTime)
      assertThat(comment).isEqualTo(request.comment)
      assertThat(created).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(createdBy).isEqualTo(DEFAULT_USERNAME)
      assertThat(updated).isNull()
      assertThat(updatedBy).isNull()
      with(occurrences()) {
        assertThat(size).isEqualTo(1)
        with(occurrences().first()) {
          assertThat(categoryCode).isEqualTo(request.categoryCode)
          assertThat(prisonCode).isEqualTo(request.prisonCode)
          assertThat(internalLocationId).isEqualTo(request.internalLocationId)
          assertThat(inCell).isEqualTo(request.inCell)
          assertThat(startDate).isEqualTo(request.startDate)
          assertThat(startTime).isEqualTo(request.startTime)
          assertThat(endTime).isEqualTo(request.endTime)
          assertThat(comment).isNull()
          assertThat(created).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
          assertThat(createdBy).isEqualTo(DEFAULT_USERNAME)
          assertThat(updated).isNull()
          assertThat(updatedBy).isNull()
          assertThat(deleted).isFalse
          with(allocations()) {
            assertThat(size).isEqualTo(1)
            with(first()) {
              assertThat(prisonerNumber).isEqualTo(request.prisonerNumbers.first())
              assertThat(bookingId).isEqualTo(1)
            }
          }
        }
      }

      verify(telemetryClient).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_CREATED.name),
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
        assertThat(value[EXTRA_INFORMATION_LENGTH_METRIC_KEY]).isEqualTo(25.0)
        assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
      }
    }
  }

  @Test
  fun `createAppointment group appointment two prisoners success`() {
    val request = appointmentCreateRequest(
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
    whenever(appointmentRepository.saveAndFlush(appointmentEntityCaptor.capture())).thenReturn(appointmentEntity())

    service.createAppointment(request, principal)

    with(appointmentEntityCaptor.value) {
      with(occurrences()) {
        assertThat(size).isEqualTo(1)
        assertThat(occurrences().first().allocations().toModel()).containsAll(
          listOf(
            AppointmentOccurrenceAllocation(id = 0, prisonerNumber = "A12345BC", bookingId = 1),
            AppointmentOccurrenceAllocation(id = 0, prisonerNumber = "B23456CE", bookingId = 2),
          ),
        )
      }
    }
  }

  @Test
  fun `createAppointment individual repeat appointment success`() {
    val request = appointmentCreateRequest(repeat = AppointmentRepeat(AppointmentRepeatPeriodModel.WEEKLY, 3))

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
    whenever(appointmentRepository.saveAndFlush(appointmentEntityCaptor.capture())).thenReturn(appointmentEntity(repeatPeriod = AppointmentRepeatPeriod.WEEKLY, numberOfOccurrences = 3))

    service.createAppointment(request, principal)

    with(appointmentEntityCaptor.value.occurrences()) {
      assertThat(size).isEqualTo(3)
      assertThat(map { it.sequenceNumber }).isEqualTo(listOf(1, 2, 3))
    }
  }

  @Test
  fun `createAppointment for fifteen prisoners synchronously when it does not repeat creating fifteen appointment instances`() {
    val prisonerNumberToBookingIdMap = (1L..15L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val request = appointmentCreateRequest(appointmentType = AppointmentType.GROUP, prisonerNumbers = prisonerNumberToBookingIdMap.keys.toList(), repeat = null)

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
    whenever(appointmentRepository.saveAndFlush(appointmentEntityCaptor.capture())).thenReturn(appointmentEntity(prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap))

    service.createAppointment(request, principal)

    with(appointmentEntityCaptor.value) {
      occurrences() hasSize 1
      occurrences().flatMap { it.allocations() } hasSize 15
    }

    verify(createAppointmentOccurrencesJob, never()).execute(any(), any())
  }

  @Test
  fun `createAppointment for fifteen prisoners synchronously when it repeats once creating fifteen appointment instances`() {
    val prisonerNumberToBookingIdMap = (1L..15L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val request = appointmentCreateRequest(appointmentType = AppointmentType.GROUP, prisonerNumbers = prisonerNumberToBookingIdMap.keys.toList(), repeat = AppointmentRepeat(AppointmentRepeatPeriodModel.DAILY, 1))

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
    whenever(appointmentRepository.saveAndFlush(appointmentEntityCaptor.capture())).thenReturn(appointmentEntity(prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap))

    service.createAppointment(request, principal)

    with(appointmentEntityCaptor.value) {
      occurrences() hasSize 1
      occurrences().flatMap { it.allocations() } hasSize 15
    }

    verify(createAppointmentOccurrencesJob, never()).execute(any(), any())
  }

  @Test
  fun `createAppointment for seven prisoners synchronously when it repeats twice creating fourteen appointment instances`() {
    val prisonerNumberToBookingIdMap = (1L..7L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val request = appointmentCreateRequest(appointmentType = AppointmentType.GROUP, prisonerNumbers = prisonerNumberToBookingIdMap.keys.toList(), repeat = AppointmentRepeat(AppointmentRepeatPeriodModel.DAILY, 2))

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
    whenever(appointmentRepository.saveAndFlush(appointmentEntityCaptor.capture())).thenReturn(appointmentEntity(prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap))

    service.createAppointment(request, principal)

    with(appointmentEntityCaptor.value) {
      occurrences() hasSize 2
      occurrences().flatMap { it.allocations() } hasSize 14
    }

    verify(createAppointmentOccurrencesJob, never()).execute(any(), any())
  }

  @Test
  fun `createAppointment for three prisoners asynchronously when it repeats five times creating fifteen appointment instances`() {
    val prisonerNumberToBookingIdMap = (1L..3L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val request = appointmentCreateRequest(appointmentType = AppointmentType.GROUP, prisonerNumbers = prisonerNumberToBookingIdMap.keys.toList(), repeat = AppointmentRepeat(AppointmentRepeatPeriodModel.DAILY, 5))

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
    whenever(appointmentRepository.saveAndFlush(appointmentEntityCaptor.capture())).thenReturn(appointmentEntity(prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap))

    service.createAppointment(request, principal)

    with(appointmentEntityCaptor.value) {
      occurrences() hasSize 1
      occurrences().flatMap { it.allocations() } hasSize 3
    }

    verify(createAppointmentOccurrencesJob).execute(1, prisonerNumberToBookingIdMap.map { it.key to it.value.toString() }.toMap())
  }

  @Test
  fun `create bulk appointment success`() {
    val request = bulkAppointmentRequest()

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

    whenever(bulkAppointmentRepository.saveAndFlush(bulkAppointmentEntityCaptor.capture())).thenReturn(
      BulkAppointment(
        bulkAppointmentId = 1,
        prisonCode = request.prisonCode,
        categoryCode = request.categoryCode,
        appointmentDescription = request.appointmentDescription,
        internalLocationId = request.internalLocationId,
        inCell = request.inCell,
        startDate = request.startDate,
        createdBy = "TEST.USER",
      ).apply {
        this.addAppointment(appointmentEntity(appointmentId = 1, bulkAppointment = this))
        this.addAppointment(appointmentEntity(appointmentId = 2, bulkAppointment = this))
      },
    )

    service.bulkCreateAppointments(request, principal)

    verify(telemetryClient).trackEvent(
      eq(TelemetryEvent.APPOINTMENT_SET_CREATED.name),
      telemetryPropertyMap.capture(),
      telemetryMetricsMap.capture(),
    )

    with(bulkAppointmentEntityCaptor.value) {
      assertThat(prisonCode).isEqualTo(request.prisonCode)
      assertThat(categoryCode).isEqualTo(request.categoryCode)
      assertThat(appointmentDescription).isEqualTo(request.appointmentDescription)
      assertThat(internalLocationId).isEqualTo(request.internalLocationId)
      assertThat(inCell).isEqualTo(request.inCell)
      assertThat(startDate).isEqualTo(request.startDate)
      assertThat(createdBy).isEqualTo(DEFAULT_USERNAME)
      assertThat(appointments()).hasSize(2)
      assertThat(appointments()[0].occurrences()[0].allocations()[0].prisonerNumber).isEqualTo("A1234BC")
      assertThat(appointments()[1].occurrences()[0].allocations()[0].prisonerNumber).isEqualTo("A1234BD")

      appointments().forEach {
        assertThat(it.categoryCode).isEqualTo("TEST")
        assertThat(it.prisonCode).isEqualTo("TPR")
        assertThat(it.internalLocationId).isEqualTo(123)
        assertThat(it.inCell).isFalse()
        assertThat(it.startDate).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(it.startTime).isEqualTo(LocalTime.of(13, 0))
        assertThat(it.endTime).isEqualTo(LocalTime.of(14, 30))
        assertThat(it.comment).isEqualTo("Test comment")
        assertThat(it.appointmentDescription).isEqualTo("Appointment description")
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
      assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
    }
  }

  @Test
  fun `create bulk appointment throws illegal argument exception when prisoner is not a resident of requested prison code`() {
    val request = bulkAppointmentRequest()

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
      service.bulkCreateAppointments(request, principal)
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Prisoner(s) with prisoner number(s) '${request.appointments[0].prisonerNumber}' not found, were inactive or are residents of a different prison.")

    verify(appointmentRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `create bulk appointment fails if no appointments provided`() {
    val request = bulkAppointmentRequest().copy(appointments = emptyList())

    assertThatThrownBy {
      service.bulkCreateAppointments(request, principal)
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("One or more appointments must be supplied.")

    verify(appointmentRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `migrateAppointment with comment under 40 characters success`() {
    val request = appointmentMigrateRequest()
    whenever(appointmentRepository.saveAndFlush(appointmentEntityCaptor.capture())).thenReturn(appointmentEntity())

    service.migrateAppointment(request, principal)

    with(appointmentEntityCaptor.value) {
      assertThat(categoryCode).isEqualTo(request.categoryCode)
      assertThat(prisonCode).isEqualTo(request.prisonCode)
      assertThat(internalLocationId).isEqualTo(request.internalLocationId)
      assertThat(startDate).isEqualTo(request.startDate)
      assertThat(startTime).isEqualTo(request.startTime)
      assertThat(endTime).isEqualTo(request.endTime)
      assertThat(comment).isEqualTo(request.comment)
      assertThat(appointmentDescription).isNull()
      assertThat(created).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(createdBy).isEqualTo(request.createdBy)
      assertThat(updated).isNull()
      assertThat(updatedBy).isNull()
      assertThat(isMigrated).isTrue()
      with(occurrences()) {
        assertThat(size).isEqualTo(1)
        with(occurrences().first()) {
          assertThat(categoryCode).isEqualTo(request.categoryCode)
          assertThat(prisonCode).isEqualTo(request.prisonCode)
          assertThat(internalLocationId).isEqualTo(request.internalLocationId)
          assertThat(startDate).isEqualTo(request.startDate)
          assertThat(startTime).isEqualTo(request.startTime)
          assertThat(endTime).isEqualTo(request.endTime)
          assertThat(comment).isNull()
          assertThat(created).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
          assertThat(createdBy).isEqualTo(request.createdBy)
          assertThat(updated).isNull()
          assertThat(updatedBy).isNull()
          assertThat(deleted).isFalse
          with(allocations()) {
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
  fun `migrateAppointment with comment over 40 characters success`() {
    val request = appointmentMigrateRequest(comment = "A".padEnd(41, 'Z'))
    whenever(appointmentRepository.saveAndFlush(appointmentEntityCaptor.capture())).thenReturn(appointmentEntity())

    service.migrateAppointment(request, principal)

    with(appointmentEntityCaptor.value) {
      assertThat(categoryCode).isEqualTo(request.categoryCode)
      assertThat(prisonCode).isEqualTo(request.prisonCode)
      assertThat(internalLocationId).isEqualTo(request.internalLocationId)
      assertThat(startDate).isEqualTo(request.startDate)
      assertThat(startTime).isEqualTo(request.startTime)
      assertThat(endTime).isEqualTo(request.endTime)
      assertThat(comment).isEqualTo(request.comment)
      assertThat(appointmentDescription).isNull()
      assertThat(created).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(createdBy).isEqualTo(request.createdBy)
      assertThat(updated).isNull()
      assertThat(updatedBy).isNull()
      assertThat(isMigrated).isTrue()
      with(occurrences()) {
        assertThat(size).isEqualTo(1)
        with(occurrences().first()) {
          assertThat(categoryCode).isEqualTo(request.categoryCode)
          assertThat(prisonCode).isEqualTo(request.prisonCode)
          assertThat(internalLocationId).isEqualTo(request.internalLocationId)
          assertThat(startDate).isEqualTo(request.startDate)
          assertThat(startTime).isEqualTo(request.startTime)
          assertThat(endTime).isEqualTo(request.endTime)
          assertThat(comment).isNull()
          assertThat(created).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
          assertThat(createdBy).isEqualTo(request.createdBy)
          assertThat(updated).isNull()
          assertThat(updatedBy).isNull()
          assertThat(deleted).isFalse
          with(allocations()) {
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
      created = LocalDateTime.of(2022, 10, 23, 10, 30),
      createdBy = "DPS.USER",
    )
    whenever(appointmentRepository.saveAndFlush(appointmentEntityCaptor.capture())).thenReturn(appointmentEntity())

    service.migrateAppointment(request, principal)

    with(appointmentEntityCaptor.value) {
      assertThat(created).isEqualTo(request.created)
      assertThat(createdBy).isEqualTo(request.createdBy)
    }
  }

  @Test
  fun `migrateAppointment with specified updated and updatedBy success`() {
    val request = appointmentMigrateRequest(
      updated = LocalDateTime.of(2022, 10, 23, 10, 30),
      updatedBy = "DPS.USER",
    )
    whenever(appointmentRepository.saveAndFlush(appointmentEntityCaptor.capture())).thenReturn(appointmentEntity())

    service.migrateAppointment(request, principal)

    with(appointmentEntityCaptor.value) {
      assertThat(updated).isEqualTo(request.updated)
      assertThat(updatedBy).isEqualTo(request.updatedBy)
      with(occurrences().first()) {
        assertThat(updated).isEqualTo(request.updated)
        assertThat(updatedBy).isEqualTo(request.updatedBy)
      }
    }
  }

  @Test
  fun `migrateAppointment isCancelled defaults to false`() {
    val request = appointmentMigrateRequest(isCancelled = null)

    whenever(appointmentRepository.saveAndFlush(appointmentEntityCaptor.capture())).thenReturn(appointmentEntity())

    service.migrateAppointment(request, principal)

    with(appointmentEntityCaptor.value) {
      with(occurrences().first()) {
        assertThat(cancelled).isNull()
        assertThat(cancellationReason).isNull()
        assertThat(cancelledBy).isNull()
      }
    }
  }

  @Test
  fun `migrateAppointment with isCancelled = true success`() {
    val request = appointmentMigrateRequest(isCancelled = true)
    val cancellationReason = AppointmentCancellationReason(2L, "Cancelled", false)
    whenever(appointmentCancellationReasonRepository.findById(2)).thenReturn(Optional.of(cancellationReason))
    whenever(appointmentRepository.saveAndFlush(appointmentEntityCaptor.capture())).thenReturn(appointmentEntity())

    service.migrateAppointment(request, principal)

    with(appointmentEntityCaptor.value) {
      with(occurrences().first()) {
        assertThat(cancelled).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
        assertThat(cancellationReason).isEqualTo(cancellationReason)
        assertThat(cancelledBy).isEqualTo(request.createdBy)
      }
    }
  }

  @Test
  fun `migrateAppointment with isCancelled = true will use updated and updated by if specified`() {
    val request = appointmentMigrateRequest(
      isCancelled = true,
      updated = LocalDateTime.of(2022, 10, 23, 10, 30),
      updatedBy = "DPS.USER",
    )
    val cancellationReason = AppointmentCancellationReason(2L, "Cancelled", false)
    whenever(appointmentCancellationReasonRepository.findById(2)).thenReturn(Optional.of(cancellationReason))
    whenever(appointmentRepository.saveAndFlush(appointmentEntityCaptor.capture())).thenReturn(appointmentEntity())

    service.migrateAppointment(request, principal)

    with(appointmentEntityCaptor.value) {
      with(occurrences().first()) {
        assertThat(cancelled).isEqualTo(request.updated)
        assertThat(cancellationReason).isEqualTo(cancellationReason)
        assertThat(cancelledBy).isEqualTo(request.updatedBy)
      }
    }
  }

  @Test
  fun `migrateAppointment with isCancelled = true will use created and created by if specified and updated and updated by are null`() {
    val request = appointmentMigrateRequest(
      isCancelled = true,
      created = LocalDateTime.of(2022, 10, 23, 10, 30),
      createdBy = "DPS.USER",
      updated = null,
      updatedBy = null,
    )
    val cancellationReason = AppointmentCancellationReason(2L, "Cancelled", false)
    whenever(appointmentCancellationReasonRepository.findById(2)).thenReturn(Optional.of(cancellationReason))
    whenever(appointmentRepository.saveAndFlush(appointmentEntityCaptor.capture())).thenReturn(appointmentEntity())

    service.migrateAppointment(request, principal)

    with(appointmentEntityCaptor.value) {
      with(occurrences().first()) {
        assertThat(cancelled).isEqualTo(request.created)
        assertThat(cancellationReason).isEqualTo(cancellationReason)
        assertThat(cancelledBy).isEqualTo(request.createdBy)
      }
    }
  }
}
