package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalTimeRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentOccurrenceSearchEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceSearchRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceAllocationSearchRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceSearchRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceSearchSpecification
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.CaseloadAccessException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.addCaseloadIdToRequestHeader
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.clearCaseloadIdFromRequestHeader
import java.security.Principal
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

class AppointmentOccurrenceSearchServiceTest {
  private val appointmentOccurrenceSearchRepository: AppointmentOccurrenceSearchRepository = mock()
  private val appointmentOccurrenceAllocationSearchRepository: AppointmentOccurrenceAllocationSearchRepository = mock()
  private val appointmentOccurrenceSearchSpecification: AppointmentOccurrenceSearchSpecification = spy()
  private val prisonRegimeService: PrisonRegimeService = mock()
  private val referenceCodeService: ReferenceCodeService = mock()
  private val locationService: LocationService = mock()

  private val principal: Principal = mock()

  private val service = AppointmentOccurrenceSearchService(
    appointmentOccurrenceSearchRepository,
    appointmentOccurrenceAllocationSearchRepository,
    appointmentOccurrenceSearchSpecification,
    prisonRegimeService,
    referenceCodeService,
    locationService,
  )

  @BeforeEach
  fun setup() {
    addCaseloadIdToRequestHeader("TPR")
  }

  @AfterEach
  fun tearDown() {
    clearCaseloadIdFromRequestHeader()
  }

  @Test
  fun `search by start date`() {
    val request = AppointmentOccurrenceSearchRequest(startDate = LocalDate.now())
    val result = appointmentOccurrenceSearchEntity()

    whenever(appointmentOccurrenceSearchRepository.findAll(any())).thenReturn(listOf(result))
    whenever(appointmentOccurrenceAllocationSearchRepository.findByAppointmentOccurrenceIds(listOf(result.appointmentOccurrenceId))).thenReturn(result.allocations)
    whenever(referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY))
      .thenReturn(mapOf(result.categoryCode to appointmentCategoryReferenceCode(result.categoryCode)))
    whenever(locationService.getLocationsForAppointmentsMap(result.prisonCode))
      .thenReturn(mapOf(result.internalLocationId!! to appointmentLocation(result.internalLocationId!!, "TPR")))

    service.searchAppointmentOccurrences("TPR", request, principal)

    verify(appointmentOccurrenceSearchSpecification).prisonCodeEquals("TPR")
    verify(appointmentOccurrenceSearchSpecification).startDateEquals(request.startDate!!)
    verifyNoMoreInteractions(appointmentOccurrenceSearchSpecification)
  }

  @Test
  fun `search by start and end date`() {
    val request = AppointmentOccurrenceSearchRequest(startDate = LocalDate.now(), endDate = LocalDate.now().plusWeeks(1))
    val result = appointmentOccurrenceSearchEntity()

    whenever(appointmentOccurrenceSearchRepository.findAll(any())).thenReturn(listOf(result))
    whenever(appointmentOccurrenceAllocationSearchRepository.findByAppointmentOccurrenceIds(listOf(result.appointmentOccurrenceId))).thenReturn(result.allocations)
    whenever(referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY))
      .thenReturn(mapOf(result.categoryCode to appointmentCategoryReferenceCode(result.categoryCode)))
    whenever(locationService.getLocationsForAppointmentsMap(result.prisonCode))
      .thenReturn(mapOf(result.internalLocationId!! to appointmentLocation(result.internalLocationId!!, "TPR")))

    service.searchAppointmentOccurrences("TPR", request, principal)

    verify(appointmentOccurrenceSearchSpecification).prisonCodeEquals("TPR")
    verify(appointmentOccurrenceSearchSpecification).startDateBetween(request.startDate!!, request.endDate!!)
    verifyNoMoreInteractions(appointmentOccurrenceSearchSpecification)
  }

  @Test
  fun `search by time slot`() {
    val request = AppointmentOccurrenceSearchRequest(startDate = LocalDate.now(), timeSlot = TimeSlot.AM)
    val result = appointmentOccurrenceSearchEntity()

    whenever(prisonRegimeService.getTimeRangeForPrisonAndTimeSlot("TPR", request.timeSlot!!))
      .thenReturn(LocalTimeRange(LocalTime.of(0, 0), LocalTime.of(13, 0)))
    whenever(appointmentOccurrenceSearchRepository.findAll(any())).thenReturn(listOf(result))
    whenever(appointmentOccurrenceAllocationSearchRepository.findByAppointmentOccurrenceIds(listOf(result.appointmentOccurrenceId))).thenReturn(result.allocations)
    whenever(referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY))
      .thenReturn(mapOf(result.categoryCode to appointmentCategoryReferenceCode(result.categoryCode)))
    whenever(locationService.getLocationsForAppointmentsMap(result.prisonCode))
      .thenReturn(mapOf(result.internalLocationId!! to appointmentLocation(result.internalLocationId!!, "TPR")))

    service.searchAppointmentOccurrences("TPR", request, principal)

    verify(appointmentOccurrenceSearchSpecification).prisonCodeEquals("TPR")
    verify(appointmentOccurrenceSearchSpecification).startDateEquals(request.startDate!!)
    verify(appointmentOccurrenceSearchSpecification).startTimeBetween(LocalTime.of(0, 0), LocalTime.of(12, 59))
    verifyNoMoreInteractions(appointmentOccurrenceSearchSpecification)
  }

  @Test
  fun `search by category code`() {
    val request = AppointmentOccurrenceSearchRequest(startDate = LocalDate.now(), categoryCode = "TEST")
    val result = appointmentOccurrenceSearchEntity()

    whenever(appointmentOccurrenceSearchRepository.findAll(any())).thenReturn(listOf(result))
    whenever(appointmentOccurrenceAllocationSearchRepository.findByAppointmentOccurrenceIds(listOf(result.appointmentOccurrenceId))).thenReturn(result.allocations)
    whenever(referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY))
      .thenReturn(mapOf(result.categoryCode to appointmentCategoryReferenceCode(result.categoryCode)))
    whenever(locationService.getLocationsForAppointmentsMap(result.prisonCode))
      .thenReturn(mapOf(result.internalLocationId!! to appointmentLocation(result.internalLocationId!!, "TPR")))

    service.searchAppointmentOccurrences("TPR", request, principal)

    verify(appointmentOccurrenceSearchSpecification).prisonCodeEquals("TPR")
    verify(appointmentOccurrenceSearchSpecification).startDateEquals(request.startDate!!)
    verify(appointmentOccurrenceSearchSpecification).categoryCodeEquals(request.categoryCode!!)
    verifyNoMoreInteractions(appointmentOccurrenceSearchSpecification)
  }

  @Test
  fun `search by internal location id`() {
    val request = AppointmentOccurrenceSearchRequest(startDate = LocalDate.now(), internalLocationId = 123)
    val result = appointmentOccurrenceSearchEntity()

    whenever(appointmentOccurrenceSearchRepository.findAll(any())).thenReturn(listOf(result))
    whenever(appointmentOccurrenceAllocationSearchRepository.findByAppointmentOccurrenceIds(listOf(result.appointmentOccurrenceId))).thenReturn(result.allocations)
    whenever(referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY))
      .thenReturn(mapOf(result.categoryCode to appointmentCategoryReferenceCode(result.categoryCode)))
    whenever(locationService.getLocationsForAppointmentsMap(result.prisonCode))
      .thenReturn(mapOf(result.internalLocationId!! to appointmentLocation(result.internalLocationId!!, "TPR")))

    service.searchAppointmentOccurrences("TPR", request, principal)

    verify(appointmentOccurrenceSearchSpecification).prisonCodeEquals("TPR")
    verify(appointmentOccurrenceSearchSpecification).startDateEquals(request.startDate!!)
    verify(appointmentOccurrenceSearchSpecification).internalLocationIdEquals(request.internalLocationId!!)
    verifyNoMoreInteractions(appointmentOccurrenceSearchSpecification)
  }

  @Test
  fun `search by in cell`() {
    val request = AppointmentOccurrenceSearchRequest(startDate = LocalDate.now(), inCell = true)
    val result = appointmentOccurrenceSearchEntity(inCell = true)

    whenever(appointmentOccurrenceSearchRepository.findAll(any())).thenReturn(listOf(result))
    whenever(appointmentOccurrenceAllocationSearchRepository.findByAppointmentOccurrenceIds(listOf(result.appointmentOccurrenceId))).thenReturn(result.allocations)
    whenever(referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY))
      .thenReturn(mapOf(result.categoryCode to appointmentCategoryReferenceCode(result.categoryCode)))
    whenever(locationService.getLocationsForAppointmentsMap(result.prisonCode))
      .thenReturn(emptyMap())

    service.searchAppointmentOccurrences("TPR", request, principal)

    verify(appointmentOccurrenceSearchSpecification).prisonCodeEquals("TPR")
    verify(appointmentOccurrenceSearchSpecification).startDateEquals(request.startDate!!)
    verify(appointmentOccurrenceSearchSpecification).inCellEquals(request.inCell!!)
    verifyNoMoreInteractions(appointmentOccurrenceSearchSpecification)
  }

  @Test
  fun `search by prisoner numbers`() {
    val request = AppointmentOccurrenceSearchRequest(startDate = LocalDate.now(), prisonerNumbers = listOf("A1234BC"))
    val result = appointmentOccurrenceSearchEntity()

    whenever(appointmentOccurrenceSearchRepository.findAll(any())).thenReturn(listOf(result))
    whenever(appointmentOccurrenceAllocationSearchRepository.findByAppointmentOccurrenceIds(listOf(result.appointmentOccurrenceId))).thenReturn(result.allocations)
    whenever(referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY))
      .thenReturn(mapOf(result.categoryCode to appointmentCategoryReferenceCode(result.categoryCode)))
    whenever(locationService.getLocationsForAppointmentsMap(result.prisonCode))
      .thenReturn(mapOf(result.internalLocationId!! to appointmentLocation(result.internalLocationId!!, "TPR")))

    service.searchAppointmentOccurrences("TPR", request, principal)

    verify(appointmentOccurrenceSearchSpecification).prisonCodeEquals("TPR")
    verify(appointmentOccurrenceSearchSpecification).startDateEquals(request.startDate!!)
    verify(appointmentOccurrenceSearchSpecification).prisonerNumbersIn(request.prisonerNumbers!!)
    verifyNoMoreInteractions(appointmentOccurrenceSearchSpecification)
  }

  @Test
  fun `search by created by`() {
    val request = AppointmentOccurrenceSearchRequest(startDate = LocalDate.now(), createdBy = "CREATE.USER")
    val result = appointmentOccurrenceSearchEntity()

    whenever(appointmentOccurrenceSearchRepository.findAll(any())).thenReturn(listOf(result))
    whenever(appointmentOccurrenceAllocationSearchRepository.findByAppointmentOccurrenceIds(listOf(result.appointmentOccurrenceId))).thenReturn(result.allocations)
    whenever(referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY))
      .thenReturn(mapOf(result.categoryCode to appointmentCategoryReferenceCode(result.categoryCode)))
    whenever(locationService.getLocationsForAppointmentsMap(result.prisonCode))
      .thenReturn(mapOf(result.internalLocationId!! to appointmentLocation(result.internalLocationId!!, "TPR")))

    service.searchAppointmentOccurrences("TPR", request, principal)

    verify(appointmentOccurrenceSearchSpecification).prisonCodeEquals("TPR")
    verify(appointmentOccurrenceSearchSpecification).startDateEquals(request.startDate!!)
    verify(appointmentOccurrenceSearchSpecification).createdByEquals(request.createdBy!!)
    verifyNoMoreInteractions(appointmentOccurrenceSearchSpecification)
  }

  @Test
  fun `search throws caseload access exception if caseload id header does not match`() {
    addCaseloadIdToRequestHeader("WRONG")
    val request = AppointmentOccurrenceSearchRequest(startDate = LocalDate.now(), createdBy = "CREATE.USER")
    val result = appointmentOccurrenceSearchEntity()

    whenever(appointmentOccurrenceSearchRepository.findAll(any())).thenReturn(listOf(result))
    whenever(appointmentOccurrenceAllocationSearchRepository.findByAppointmentOccurrenceIds(listOf(result.appointmentOccurrenceId))).thenReturn(result.allocations)
    whenever(referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY))
      .thenReturn(mapOf(result.categoryCode to appointmentCategoryReferenceCode(result.categoryCode)))
    whenever(locationService.getLocationsForAppointmentsMap(result.prisonCode))
      .thenReturn(mapOf(result.internalLocationId!! to appointmentLocation(result.internalLocationId!!, "TPR")))

    Assertions.assertThatThrownBy { service.searchAppointmentOccurrences("TPR", request, principal) }
      .isInstanceOf(CaseloadAccessException::class.java)
  }
}
