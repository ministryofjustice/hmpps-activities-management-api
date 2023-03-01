package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiUserClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.userCaseLoads
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrenceAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import java.lang.IllegalArgumentException
import java.security.Principal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.Optional

class AppointmentServiceTest {
  private val appointmentCategoryRepository: AppointmentCategoryRepository = mock()
  private val appointmentRepository: AppointmentRepository = mock()
  private val locationService: LocationService = mock()
  private val prisonApiUserClient: PrisonApiUserClient = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()

  @Captor
  private lateinit var appointmentEntityCaptor: ArgumentCaptor<Appointment>

  private val service = AppointmentService(
    appointmentCategoryRepository,
    appointmentRepository,
    locationService,
    prisonApiUserClient,
    prisonerSearchApiClient
  )

  @BeforeEach
  fun setUp() {
    MockitoAnnotations.openMocks(this)
  }

  @Test
  fun `getAppointmentById returns an appointment for known appointment id`() {
    val entity = appointmentEntity()
    whenever(appointmentRepository.findById(1)).thenReturn(Optional.of(entity))
    assertThat(service.getAppointmentById(1)).isEqualTo(entity.toModel())
  }

  @Test
  fun `getAppointmentById throws entity not found exception for unknown appointment id`() {
    assertThatThrownBy { service.getAppointmentById(-1) }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Appointment -1 not found")
  }

  @Test
  fun `createAppointment throws illegal argument exception when requested prison code is not in user's case load`() {
    val request = appointmentCreateRequest()
    val principal: Principal = mock()

    whenever(prisonApiUserClient.getUserCaseLoads()).thenReturn(Mono.just(emptyList()))

    assertThatThrownBy { service.createAppointment(request, principal) }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Prison code '${request.prisonCode}' not found in user's case load")

    verify(appointmentRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `createAppointment throws illegal argument exception when requested category id is not found`() {
    val request = appointmentCreateRequest()
    val principal: Principal = mock()

    whenever(prisonApiUserClient.getUserCaseLoads()).thenReturn(Mono.just(userCaseLoads(request.prisonCode!!)))
    whenever(appointmentCategoryRepository.findById(request.categoryId!!)).thenReturn(Optional.empty())

    assertThatThrownBy { service.createAppointment(request, principal) }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Appointment Category ${request.categoryId} not found")

    verify(appointmentRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `createAppointment throws illegal argument exception when requested category id is inactive`() {
    val request = appointmentCreateRequest()
    val principal: Principal = mock()

    whenever(prisonApiUserClient.getUserCaseLoads()).thenReturn(Mono.just(userCaseLoads(request.prisonCode!!)))
    whenever(appointmentCategoryRepository.findById(request.categoryId!!)).thenReturn(Optional.of(appointmentCategoryEntity(active = false)))

    assertThatThrownBy { service.createAppointment(request, principal) }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Appointment Category ${request.categoryId} is not active")

    verify(appointmentRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `createAppointment throws illegal argument exception when inCell = false and requested internal location id is not found`() {
    val request = appointmentCreateRequest()
    val principal: Principal = mock()

    whenever(prisonApiUserClient.getUserCaseLoads()).thenReturn(Mono.just(userCaseLoads(request.prisonCode!!)))
    whenever(appointmentCategoryRepository.findById(request.categoryId!!)).thenReturn(Optional.of(appointmentCategoryEntity()))
    whenever(locationService.getLocationsForAppointments(request.prisonCode!!)).thenReturn(listOf())

    assertThatThrownBy { service.createAppointment(request, principal) }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Appointment location with id ${request.internalLocationId} not found in prison '${request.prisonCode}'")

    verify(appointmentRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `createAppointment throws illegal argument exception when prisoner is not found`() {
    val request = appointmentCreateRequest()
    val principal: Principal = mock()

    whenever(prisonApiUserClient.getUserCaseLoads()).thenReturn(Mono.just(userCaseLoads(request.prisonCode!!)))
    whenever(appointmentCategoryRepository.findById(request.categoryId!!)).thenReturn(Optional.of(appointmentCategoryEntity()))
    whenever(locationService.getLocationsForAppointments(request.prisonCode!!)).thenReturn(listOf(appointmentLocation(request.internalLocationId!!, request.prisonCode!!)))
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers)).thenReturn(Mono.just(emptyList()))

    assertThatThrownBy { service.createAppointment(request, principal) }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Prisoner(s) with prisoner number(s) '${request.prisonerNumbers.first()}' not found, were inactive or are residents of a different prison.")

    verify(appointmentRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `createAppointment throws illegal argument exception when prisoner is not a resident of requested prison code`() {
    val request = appointmentCreateRequest()
    val principal: Principal = mock()

    whenever(prisonApiUserClient.getUserCaseLoads()).thenReturn(Mono.just(userCaseLoads(request.prisonCode!!)))
    whenever(appointmentCategoryRepository.findById(request.categoryId!!)).thenReturn(Optional.of(appointmentCategoryEntity()))
    whenever(locationService.getLocationsForAppointments(request.prisonCode!!))
      .thenReturn(listOf(appointmentLocation(request.internalLocationId!!, request.prisonCode!!)))
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers))
      .thenReturn(Mono.just(listOf(PrisonerSearchPrisonerFixture.instance(prisonerNumber = request.prisonerNumbers.first(), prisonId = "DIFFERENT"))))

    assertThatThrownBy { service.createAppointment(request, principal) }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Prisoner(s) with prisoner number(s) '${request.prisonerNumbers.first()}' not found, were inactive or are residents of a different prison.")

    verify(appointmentRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `createAppointment single appointment single prisoner success`() {
    val request = appointmentCreateRequest()
    val principal: Principal = mock()
    whenever(principal.name).thenReturn("TEST.USER")

    whenever(prisonApiUserClient.getUserCaseLoads()).thenReturn(Mono.just(userCaseLoads(request.prisonCode!!)))
    whenever(appointmentCategoryRepository.findById(request.categoryId!!)).thenReturn(Optional.of(appointmentCategoryEntity()))
    whenever(locationService.getLocationsForAppointments(request.prisonCode!!))
      .thenReturn(listOf(appointmentLocation(request.internalLocationId!!, request.prisonCode!!)))
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers))
      .thenReturn(
        Mono.just(
          listOf(
            PrisonerSearchPrisonerFixture.instance(prisonerNumber = request.prisonerNumbers.first(), bookingId = 1, prisonId = request.prisonCode!!)
          )
        )
      )
    whenever(appointmentRepository.saveAndFlush(appointmentEntityCaptor.capture())).thenReturn(appointmentEntity())

    service.createAppointment(request, principal)

    with(appointmentEntityCaptor.value) {
      assertThat(category.appointmentCategoryId).isEqualTo(request.categoryId)
      assertThat(prisonCode).isEqualTo(request.prisonCode)
      assertThat(internalLocationId).isEqualTo(request.internalLocationId)
      assertThat(inCell).isEqualTo(request.inCell)
      assertThat(startDate).isEqualTo(request.startDate)
      assertThat(startTime).isEqualTo(request.startTime)
      assertThat(endTime).isEqualTo(request.endTime)
      assertThat(comment).isEqualTo(request.comment)
      assertThat(created).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(createdBy).isEqualTo("TEST.USER")
      assertThat(updated).isNull()
      assertThat(updatedBy).isNull()
      assertThat(deleted).isFalse
      with(occurrences()) {
        assertThat(size).isEqualTo(1)
        with(occurrences().first()) {
          assertThat(category.appointmentCategoryId).isEqualTo(request.categoryId)
          assertThat(prisonCode).isEqualTo(request.prisonCode)
          assertThat(internalLocationId).isEqualTo(request.internalLocationId)
          assertThat(inCell).isEqualTo(request.inCell)
          assertThat(startDate).isEqualTo(request.startDate)
          assertThat(startTime).isEqualTo(request.startTime)
          assertThat(endTime).isEqualTo(request.endTime)
          assertThat(comment).isNull()
          assertThat(created).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
          assertThat(createdBy).isEqualTo("TEST.USER")
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
          with(instances()) {
            assertThat(size).isEqualTo(1)
            with(first()) {
              assertThat(prisonerNumber).isEqualTo(request.prisonerNumbers.first())
              assertThat(bookingId).isEqualTo(1)
            }
          }
        }
      }
    }
  }

  @Test
  fun `createAppointment single appointment two prisoners success`() {
    val request = appointmentCreateRequest(prisonerNumbers = listOf("A12345BC", "B23456CE"))
    val principal: Principal = mock()
    whenever(principal.name).thenReturn("TEST.USER")

    whenever(prisonApiUserClient.getUserCaseLoads()).thenReturn(Mono.just(userCaseLoads(request.prisonCode!!)))
    whenever(appointmentCategoryRepository.findById(request.categoryId!!)).thenReturn(Optional.of(appointmentCategoryEntity()))
    whenever(locationService.getLocationsForAppointments(request.prisonCode!!))
      .thenReturn(listOf(appointmentLocation(request.internalLocationId!!, request.prisonCode!!)))
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers))
      .thenReturn(
        Mono.just(
          listOf(
            PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A12345BC", bookingId = 1, prisonId = request.prisonCode!!),
            PrisonerSearchPrisonerFixture.instance(prisonerNumber = "B23456CE", bookingId = 2, prisonId = request.prisonCode!!)
          )
        )
      )
    whenever(appointmentRepository.saveAndFlush(appointmentEntityCaptor.capture())).thenReturn(appointmentEntity())

    service.createAppointment(request, principal)

    with(appointmentEntityCaptor.value) {
      with(occurrences()) {
        assertThat(size).isEqualTo(1)
        assertThat(occurrences().first().allocations().toModel()).containsAll(
          listOf(
            AppointmentOccurrenceAllocation(id = -1, prisonerNumber = "A12345BC", bookingId = 1),
            AppointmentOccurrenceAllocation(id = -1, prisonerNumber = "B23456CE", bookingId = 2)
          )
        )
        assertThat(occurrences().first().instances().toModel()).containsAll(
          listOf(
            AppointmentInstance(
              id = -1,
              category = AppointmentCategory(
                id = 1, parent = null, code = "TEST",
                description = "Test Category", active = true, displayOrder = 2
              ),
              prisonCode = "TPR", internalLocationId = 123, inCell = false, prisonerNumber = "A12345BC",
              bookingId = 1, appointmentDate = LocalDate.now().plusDays(1),
              startTime = LocalTime.of(13, 0), endTime = LocalTime.of(14, 30),
              comment = null, attended = null, cancelled = false
            ),
            AppointmentInstance(
              id = -1,
              category = AppointmentCategory(
                id = 1, parent = null, code = "TEST",
                description = "Test Category", active = true, displayOrder = 2
              ),
              prisonCode = "TPR", internalLocationId = 123, inCell = false, prisonerNumber = "B23456CE",
              bookingId = 2, appointmentDate = LocalDate.now().plusDays(1),
              startTime = LocalTime.of(13, 0), endTime = LocalTime.of(14, 30),
              comment = null, attended = null, cancelled = false
            )
          )
        )
      }
    }
  }
}
