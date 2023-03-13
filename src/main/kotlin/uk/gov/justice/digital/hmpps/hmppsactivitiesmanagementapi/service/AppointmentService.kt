package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiUserClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentRepeatPeriod
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowIllegalArgument
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import java.security.Principal
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment as AppointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentInstance as AppointmentInstanceEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrence as AppointmentOccurrenceEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrenceAllocation as AppointmentOccurrenceAllocationEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment as AppointmentModel

@Service
class AppointmentService(
  private val appointmentCategoryRepository: AppointmentCategoryRepository,
  private val appointmentRepository: AppointmentRepository,
  private val appointmentScheduleRepository: AppointmentScheduleRepository,
  private val locationService: LocationService,
  private val prisonApiUserClient: PrisonApiUserClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
) {
  fun getAppointmentById(appointmentId: Long) =
    appointmentRepository.findOrThrowNotFound(appointmentId).toModel()

  fun createAppointment(request: AppointmentCreateRequest, principal: Principal): AppointmentModel {
    failIfPrisonCodeNotInUserCaseLoad(request.prisonCode!!)

    val category = appointmentCategoryRepository.findOrThrowIllegalArgument(request.categoryId!!)
    failIfCategoryIsNotActive(category)

    failIfLocationNotFound(request)

    val prisonerMap = prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers).block()!!
      .filter { prisoner -> prisoner.prisonId == request.prisonCode }
      .associateBy { it.prisonerNumber }

    failIfMissingPrisoners(request.prisonerNumbers, prisonerMap)

    val schedule = request.repeat?.let {
      appointmentScheduleRepository.saveAndFlush(
        AppointmentSchedule(
          repeatPeriod = AppointmentRepeatPeriod.valueOf(request.repeat.period!!.name),
          repeatCount = request.repeat.count!!,
        ),
      )
    }

    return AppointmentEntity(
      category = category,
      prisonCode = request.prisonCode,
      internalLocationId = if (request.inCell) null else request.internalLocationId,
      inCell = request.inCell,
      startDate = request.startDate!!,
      startTime = request.startTime!!,
      endTime = request.endTime,
      schedule = schedule,
      comment = request.comment,
      createdBy = principal.name,
    ).apply {
      this.scheduleIterator().forEach {
        this.addOccurrence(
          AppointmentOccurrenceEntity(
            appointment = this,
            internalLocationId = this.internalLocationId,
            inCell = this.inCell,
            startDate = it,
            startTime = this.startTime,
            endTime = this.endTime,
          ).apply {
            prisonerMap.values.forEach { prisoner ->
              this.addAllocation(
                AppointmentOccurrenceAllocationEntity(
                  appointmentOccurrence = this,
                  prisonerNumber = prisoner.prisonerNumber,
                  bookingId = prisoner.bookingId!!.toLong(),
                ),
              )
              this.addInstance(
                AppointmentInstanceEntity(
                  appointmentOccurrence = this,
                  prisonerNumber = prisoner.prisonerNumber,
                  bookingId = prisoner.bookingId.toLong(),
                  appointmentDate = this.startDate,
                  category = category,
                  endTime = this.endTime,
                  inCell = this.inCell,
                  internalLocationId = this.internalLocationId,
                  prisonCode = request.prisonCode,
                  startTime = this.startTime,
                ),
              )
            }
          },
        )
      }
    }.let { (appointmentRepository.saveAndFlush(it)).toModel() }
  }

  private fun failIfPrisonCodeNotInUserCaseLoad(prisonCode: String) {
    prisonApiUserClient.getUserCaseLoads().block()
      ?.firstOrNull { caseLoad -> caseLoad.caseLoadId == prisonCode }
      ?: throw IllegalArgumentException("Prison code '$prisonCode' not found in user's case load")
  }

  private fun failIfCategoryIsNotActive(category: AppointmentCategory) {
    if (!category.active) throw IllegalArgumentException("Appointment Category ${category.appointmentCategoryId} is not active")
  }

  private fun failIfLocationNotFound(request: AppointmentCreateRequest) {
    if (!request.inCell) {
      locationService.getLocationsForAppointments(request.prisonCode!!)
        ?.firstOrNull { location -> location.locationId == request.internalLocationId }
        ?: throw IllegalArgumentException("Appointment location with id ${request.internalLocationId} not found in prison '${request.prisonCode}'")
    }
  }

  private fun failIfMissingPrisoners(prisonerNumbers: List<String>, prisonerMap: Map<String, Prisoner>) {
    prisonerNumbers.filter { number -> !prisonerMap.containsKey(number) }.let {
      if (it.any()) throw IllegalArgumentException("Prisoner(s) with prisoner number(s) '${it.joinToString("', '")}' not found, were inactive or are residents of a different prison.")
    }
  }
}
