package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockitoAnnotations.openMocks
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentParentCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.CategoryStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCategoryRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentParentCategoryRepository
import java.util.Optional

class AppointmentCategoryServiceTest {
  private val appointmentCategoryRepository: AppointmentCategoryRepository = mock()
  private val appointmentParentCategoryRepository: AppointmentParentCategoryRepository = mock()
  private val appointmentCategoryService = AppointmentCategoryService(appointmentCategoryRepository, appointmentParentCategoryRepository)

  val captor = argumentCaptor<AppointmentCategory>()

  @BeforeEach
  fun setUp() {
    openMocks(this)
  }

  @Test
  fun `returns empty list of appointment category`() {
    whenever(appointmentCategoryRepository.findAll()).thenReturn(emptyList())
    assertThat(appointmentCategoryService.get()).isEqualTo(emptyList<AppointmentCategorySummary>())
  }

  @Test
  fun `returns the list of appointment category`() {
    val appointmentCategoryActive = AppointmentCategory(
      appointmentCategoryId = 1,
      code = "TEST",
      description = "Test Category",
      appointmentParentCategory = AppointmentParentCategory(5, "Category"),
      status = CategoryStatus.ACTIVE,
    )
    val appointmentCategoryInactive = AppointmentCategory(
      appointmentCategoryId = 2,
      code = "TEST2",
      description = "Test Category2",
      appointmentParentCategory = AppointmentParentCategory(5, "Category"),
      status = CategoryStatus.INACTIVE,
    )

    whenever(appointmentCategoryRepository.findAll()).thenReturn(listOf(appointmentCategoryActive, appointmentCategoryInactive))
    assertThat(appointmentCategoryService.get()).isEqualTo(listOf(appointmentCategorySummary()))
  }

  @Test
  fun `should create appointment category`() {
    val appointmentParentCategory = AppointmentParentCategory(5, "Other", "Other category")
    whenever(appointmentParentCategoryRepository.findById(1)).thenReturn(Optional.of(appointmentParentCategory))
    whenever(appointmentCategoryRepository.findByCode("TEST")).thenReturn(Optional.empty())

    val savedAppointmentCategory = AppointmentCategory(1, "TEST", "Test category", appointmentParentCategory, CategoryStatus.ACTIVE)
    whenever(appointmentCategoryRepository.save(any<AppointmentCategory>())).thenReturn(savedAppointmentCategory)

    val request = AppointmentCategoryRequest("TEST", "Test category", 1, CategoryStatus.ACTIVE)
    val result = appointmentCategoryService.create(request)

    with(result) {
      id isEqualTo 1
      code isEqualTo "TEST"
      description isEqualTo "Test category"
      appointmentParentCategory isEqualTo appointmentParentCategory
      status isEqualTo CategoryStatus.ACTIVE
    }

    verify(appointmentCategoryRepository).save(captor.capture())

    with(captor.firstValue) {
      appointmentCategoryId isEqualTo 0
      code isEqualTo "TEST"
      description isEqualTo "Test category"
      appointmentParentCategory isEqualTo appointmentParentCategory
      status isEqualTo CategoryStatus.ACTIVE
    }
  }

  @Test
  fun `should throw exception when appointment category exists`() {
    val appointmentParentCategory = AppointmentParentCategory(5, "Other", "Other category")
    val existingAppointmentCategory = AppointmentCategory(1, "TEST", "Test category", appointmentParentCategory, CategoryStatus.ACTIVE)
    whenever(appointmentCategoryRepository.findByCode("TEST")).thenReturn(Optional.of(existingAppointmentCategory))

    val request = AppointmentCategoryRequest("TEST", "Test category", 1, CategoryStatus.ACTIVE)
    assertThatThrownBy {
      appointmentCategoryService.create(request)
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Appointment Category ${request.code} is found")
  }

  @Test
  fun `should throw exception when appointment parent category does not exist`() {
    whenever(appointmentParentCategoryRepository.findById(1)).thenReturn(Optional.empty())
    whenever(appointmentCategoryRepository.findByCode("TEST")).thenReturn(Optional.empty())

    val request = AppointmentCategoryRequest("TEST", "Test category", 1, CategoryStatus.ACTIVE)
    assertThatThrownBy {
      appointmentCategoryService.create(request)
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Appointment Parent Category 1 not found")
  }

  @Test
  fun `should update appointment category`() {
    val appointmentParentCategory = AppointmentParentCategory(5, "Other", "Other category")
    whenever(appointmentParentCategoryRepository.findById(1)).thenReturn(Optional.of(appointmentParentCategory))

    val existingAppointmentCategory = AppointmentCategory(1, "TEST", "Test category", appointmentParentCategory, CategoryStatus.ACTIVE)
    whenever(appointmentCategoryRepository.findById(1)).thenReturn(Optional.of(existingAppointmentCategory))

    val savedAppointmentCategory = AppointmentCategory(1, "TEST", "Test description", appointmentParentCategory, CategoryStatus.ACTIVE)
    whenever(appointmentCategoryRepository.save(any<AppointmentCategory>())).thenReturn(savedAppointmentCategory)

    val request = AppointmentCategoryRequest("TEST", "Test description", 1, CategoryStatus.ACTIVE)
    val result = appointmentCategoryService.update(1, request)

    with(result) {
      id isEqualTo 1
      code isEqualTo "TEST"
      description isEqualTo "Test description"
      appointmentParentCategory isEqualTo appointmentParentCategory
      status isEqualTo CategoryStatus.ACTIVE
    }

    verify(appointmentCategoryRepository).save(captor.capture())

    with(captor.firstValue) {
      appointmentCategoryId isEqualTo 1
      code isEqualTo "TEST"
      description isEqualTo "Test description"
      appointmentParentCategory isEqualTo appointmentParentCategory
      status isEqualTo CategoryStatus.ACTIVE
    }
  }

  @Test
  fun `should throw exception when appointment category does not exist on update`() {
    whenever(appointmentCategoryRepository.findById(1)).thenReturn(Optional.empty())

    val request = AppointmentCategoryRequest("TEST", "Test category", 1, CategoryStatus.ACTIVE)
    assertThatThrownBy {
      appointmentCategoryService.update(1, request)
    }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Appointment Category 1 not found")
  }

  @Test
  fun `should delete appointment category`() {
    val appointmentParentCategory = AppointmentParentCategory(5, "Other", "Other category")
    whenever(appointmentParentCategoryRepository.findById(1)).thenReturn(Optional.of(appointmentParentCategory))

    val existingAppointmentCategory = AppointmentCategory(1, "TEST", "Test category", appointmentParentCategory, CategoryStatus.ACTIVE)
    whenever(appointmentCategoryRepository.findById(1)).thenReturn(Optional.of(existingAppointmentCategory))

    appointmentCategoryService.delete(1)

    verify(appointmentCategoryRepository).delete(existingAppointmentCategory)
  }

  @Test
  fun `should throw exception when appointment category does not exist on delete`() {
    whenever(appointmentCategoryRepository.findById(1)).thenReturn(Optional.empty())

    assertThatThrownBy {
      appointmentCategoryService.delete(1)
    }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Appointment Category 1 not found")
  }
}
