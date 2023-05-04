package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiUserClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentRepeatPeriod
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentRepeat
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentMigrateRequest
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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.BulkAppointment as BulkAppointmentEntity

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
    createPrisonerMap(request.appointments.map { it.prisonerNumber }, request.prisonCode).let { prisonerBookings ->
      BulkAppointmentEntity(
        appointments = request.appointments.map {
          buildValidAppointmentEntity(
            prisonCode = request.prisonCode,
            categoryCode = request.categoryCode,
            appointmentDescription = request.appointmentDescription,
            internalLocationId = request.internalLocationId,
            inCell = request.inCell,
            startDate = request.startDate,
            prisonerBookings = prisonerBookings.filter { entry -> entry.key == it.prisonerNumber },
            prisonerNumbers = listOf(it.prisonerNumber),
            startTime = it.startTime,
            endTime = it.endTime,
            appointmentType = AppointmentType.INDIVIDUAL,
            principal = principal,
          )
        }.toList(),
        createdBy = principal.name,
      ).let { bulkAppointmentRepository.saveAndFlush(it).toModel() }
    }

  fun createAppointment(request: AppointmentCreateRequest, principal: Principal) =
    createPrisonerMap(request.prisonerNumbers, request.prisonCode).let { prisonerBookings ->
      buildValidAppointmentEntity(
        inCell = request.inCell,
        prisonCode = request.prisonCode,
        categoryCode = request.categoryCode,
        internalLocationId = request.internalLocationId,
        prisonerBookings = prisonerBookings,
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
    }

  fun migrateAppointment(request: AppointmentMigrateRequest, principal: Principal) =

    mapOf(request.prisonerNumber to request.bookingId.toString()).let { prisonerBookings ->
      buildValidAppointmentEntity(
        prisonCode = request.prisonCode,
        categoryCode = request.categoryCode,
        internalLocationId = request.internalLocationId,
        prisonerBookings = prisonerBookings,
        prisonerNumbers = listOf(request.prisonerNumber),
        inCell = false,
        startDate = request.startDate,
        startTime = request.startTime,
        endTime = request.endTime,
        comment = if (request.comment.length <= 40) request.comment else "",
        appointmentDescription = if (request.comment.length > 40) request.comment else null,
        appointmentType = AppointmentType.INDIVIDUAL,
        principal = principal,
        isMigration = true,
      ).let { (appointmentRepository.saveAndFlush(it)).toModel() }
    }

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

  private fun failIfMissingPrisoners(prisonerNumbers: List<String>, prisonerBookings: Map<String, String?>) {
    prisonerNumbers.filter { number -> !prisonerBookings.containsKey(number) }.let {
      if (it.any()) throw IllegalArgumentException("Prisoner(s) with prisoner number(s) '${it.joinToString("', '")}' not found, were inactive or are residents of a different prison.")
    }
  }

  fun buildValidAppointmentEntity(
    inCell: Boolean,
    prisonCode: String? = null,
    categoryCode: String? = null,
    internalLocationId: Long? = null,
    prisonerNumbers: List<String>,
    prisonerBookings: Map<String, String?>,
    startDate: LocalDate?,
    startTime: LocalTime?,
    endTime: LocalTime?,
    comment: String = "",
    appointmentDescription: String? = null,
    appointmentType: AppointmentType? = null,
    repeat: AppointmentRepeat? = null,
    principal: Principal,
    isMigration: Boolean = false,
  ): AppointmentEntity {
    if (!isMigration) {
      failIfPrisonCodeNotInUserCaseLoad(prisonCode!!)
      failIfCategoryNotFound(categoryCode!!)
      failIfLocationNotFound(inCell, prisonCode, internalLocationId)
      failIfMissingPrisoners(prisonerNumbers, prisonerBookings)
    }

    return AppointmentEntity(
      categoryCode = categoryCode ?: "",
      prisonCode = prisonCode ?: "",
      internalLocationId = if (inCell) null else internalLocationId,
      inCell = inCell,
      startDate = startDate!!,
      startTime = startTime!!,
      endTime = endTime,
      comment = comment,
      appointmentDescription = appointmentDescription,
      createdBy = principal.name,
      appointmentType = appointmentType!!,
      isMigrated = isMigration,
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
            prisonerBookings.forEach { prisonerBooking ->
              this.addAllocation(
                AppointmentOccurrenceAllocationEntity(
                  appointmentOccurrence = this,
                  prisonerNumber = prisonerBooking.key,
                  bookingId = prisonerBooking.value!!.toLong(),
                ),
              )
            }
          },
        )
      }
    }
  }

  private fun createPrisonerMap(prisonerNumbers: List<String>, prisonCode: String?) =
    prisonerSearchApiClient.findByPrisonerNumbers(prisonerNumbers).block()!!
      .filter { prisoner -> prisoner.prisonId == prisonCode }
      .associate { it.prisonerNumber to it.bookingId }
}
