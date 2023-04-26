package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiUserClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentRepeatPeriod
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.BulkAppointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentRepeat
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.BulkAppointmentsRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.BulkAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import java.security.Principal
import java.time.LocalDate
import java.time.LocalTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment as AppointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrence as AppointmentOccurrenceEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrenceAllocation as AppointmentOccurrenceAllocationEntity

@Service
class AppointmentService(
  private val appointmentRepository: AppointmentRepository,
  private val bulkAppointmentRepository: BulkAppointmentRepository,
  private val referenceCodeService: ReferenceCodeService,
  private val locationService: LocationService,
  private val prisonApiUserClient: PrisonApiUserClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
) {
  fun getAppointmentById(appointmentId: Long) =
    appointmentRepository.findOrThrowNotFound(appointmentId).toModel()

  fun bulkCreateAppointments(request: BulkAppointmentsRequest, principal: Principal) =
    BulkAppointment(
      appointments = request.appointments.map {
        buildValidAppointmentEntity(
          prisonCode = request.prisonCode,
          categoryCode = request.categoryCode,
          appointmentDescription = request.appointmentDescription,
          internalLocationId = request.internalLocationId,
          inCell = request.inCell,
          startDate = request.startDate,
          prisonerNumbers = listOf(it.prisonerNumber),
          startTime = it.startTime,
          endTime = it.endTime,
          appointmentType = AppointmentType.INDIVIDUAL,
          principal = principal,
        )
      }.toList(),
    ).let { bulkAppointmentRepository.saveAndFlush(it).toModel() }

  fun createAppointment(request: AppointmentCreateRequest, principal: Principal) =
    buildValidAppointmentEntity(
      inCell = request.inCell,
      prisonCode = request.prisonCode,
      categoryCode = request.categoryCode,
      internalLocationId = request.internalLocationId,
      prisonerNumbers = request.prisonerNumbers,
      startDate = request.startDate,
      startTime = request.startTime,
      endTime = request.endTime,
      comment = request.comment,
      appointmentDescription = request.appointmentDescription,
      appointmentType = request.appointmentType,
      principal = principal,
      repeat = request.repeat,
    ).let { (appointmentRepository.saveAndFlush(it)).toModel() }

  private fun failIfPrisonCodeNotInUserCaseLoad(prisonCode: String) {
    prisonApiUserClient.getUserCaseLoads().block()
      ?.firstOrNull { caseLoad -> caseLoad.caseLoadId == prisonCode }
      ?: throw IllegalArgumentException("Prison code '$prisonCode' not found in user's case load")
  }

  private fun failIfCategoryNotFound(categoryCode: String) {
    referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT)[categoryCode]
      ?: throw IllegalArgumentException("Appointment Category with code $categoryCode not found or is not active")
  }

  private fun failIfLocationNotFound(inCell: Boolean, prisonCode: String?, internalLocationId: Long?) {
    if (!inCell) {
      locationService.getLocationsForAppointmentsMap(prisonCode!!)[internalLocationId]
        ?: throw IllegalArgumentException("Appointment location with id $internalLocationId not found in prison '$prisonCode'")
    }
  }

  private fun failIfMissingPrisoners(prisonerNumbers: List<String>, prisonerMap: Map<String, Prisoner>) {
    prisonerNumbers.filter { number -> !prisonerMap.containsKey(number) }.let {
      if (it.any()) throw IllegalArgumentException("Prisoner(s) with prisoner number(s) '${it.joinToString("', '")}' not found, were inactive or are residents of a different prison.")
    }
  }

  private fun buildValidAppointmentEntity(
    inCell: Boolean,
    prisonCode: String? = null,
    categoryCode: String? = null,
    internalLocationId: Long? = null,
    prisonerNumbers: List<String>,
    startDate: LocalDate?,
    startTime: LocalTime?,
    endTime: LocalTime?,
    comment: String = "",
    appointmentDescription: String? = null,
    appointmentType: AppointmentType? = null,
    repeat: AppointmentRepeat? = null,
    principal: Principal,
  ): AppointmentEntity {
    failIfPrisonCodeNotInUserCaseLoad(prisonCode!!)

    failIfCategoryNotFound(categoryCode!!)

    failIfLocationNotFound(inCell, prisonCode, internalLocationId)

    val prisonerMap = prisonerSearchApiClient.findByPrisonerNumbers(prisonerNumbers).block()!!
      .filter { prisoner -> prisoner.prisonId == prisonCode }
      .associateBy { it.prisonerNumber }

    failIfMissingPrisoners(prisonerNumbers, prisonerMap)

    return AppointmentEntity(
      categoryCode = categoryCode,
      prisonCode = prisonCode,
      internalLocationId = if (inCell) null else internalLocationId,
      inCell = inCell,
      startDate = startDate!!,
      startTime = startTime!!,
      endTime = endTime,
      comment = comment,
      appointmentDescription = appointmentDescription,
      createdBy = principal.name,
      appointmentType = appointmentType!!,
    ).apply {
      this.schedule = repeat?.let {
        AppointmentSchedule(
          appointment = this,
          repeatPeriod = AppointmentRepeatPeriod.valueOf(repeat.period!!.name),
          repeatCount = repeat.count!!,
        )
      }

      this.scheduleIterator().withIndex().forEach {
        this.addOccurrence(
          AppointmentOccurrenceEntity(
            appointment = this,
            sequenceNumber = it.index + 1,
            internalLocationId = this.internalLocationId,
            inCell = this.inCell,
            startDate = it.value,
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
            }
          },
        )
      }
    }
  }
}
